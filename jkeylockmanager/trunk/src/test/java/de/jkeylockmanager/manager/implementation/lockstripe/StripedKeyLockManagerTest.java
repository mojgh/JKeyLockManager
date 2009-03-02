/*
 * Copyright 2009 Marc-Olaf Jaschke
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.jkeylockmanager.manager.implementation.lockstripe;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Test;

import de.jkeylockmanager.manager.LockCallback;
import de.jkeylockmanager.manager.ReturnValueLockCallback;
import de.jkeylockmanager.manager.exception.KeyLockManagerException;
import de.jkeylockmanager.manager.exception.KeyLockManagerExecutionException;
import de.jkeylockmanager.manager.exception.KeyLockManagerInterruptedException;
import de.jkeylockmanager.manager.exception.KeyLockManagerTimeoutException;

/**
 * 
 * @author Marc-Olaf Jaschke
 * 
 */
public class StripedKeyLockManagerTest {

	private static class TestException extends RuntimeException {
		private static final long serialVersionUID = -7939021299106344924L;
	}

	public void assertCleanup(final StripedKeyLockManager lock) {
		assertEquals("not all locks were released", 0, lock.activeKeyLocksCount());
	}

	/**
	 * one thread holds a lock on one key - a second thread waits to acquire the
	 * lock on the same key - the first thread throws an exception - the second
	 * thread acquires the lock
	 */
	@Test
	public void testExceptionInWorkUnit() throws Exception {

		final StripedKeyLockManager manager = new StripedKeyLockManager(Long.MAX_VALUE, TimeUnit.SECONDS);

		final CountDownLatch t1WorkUnitEntry = new CountDownLatch(1);
		final CountDownLatch t1ThrowException = new CountDownLatch(1);

		final Thread t1 = new Thread() {
			@Override
			public void run() {
				try {
					manager.executeLocked("test1", new LockCallback() {
						public void doInLock() {
							try {
								t1WorkUnitEntry.countDown();
								t1ThrowException.await();
								throw new TestException();
							} catch (final InterruptedException e) {
								return;
							}
						}
					});
				} catch (final KeyLockManagerExecutionException e) {
					Assert.assertTrue(e.getCause() instanceof TestException);
				}
			}
		};
		t1.start();

		t1WorkUnitEntry.await();

		// t2 should enter the work unit after t1 throws the exception

		final Thread t2 = new Thread() {
			@Override
			public void run() {
				manager.executeLocked("test1", new LockCallback() {
					public void doInLock() {
					}
				});
			}
		};
		t2.start();

		while (manager.waitingThreadsCount() < 1) {
			Thread.sleep(10);
		}

		t1ThrowException.countDown();

		t2.join();

		t1.interrupt();
		t1.join();

		assertCleanup(manager);
	}

	/**
	 * one thread holds a lock on one key -> a second thread waits on the same
	 * key and becomes interrupted while waiting -> t2 must throw a special
	 * exception
	 */
	@Test
	public void testInterruptReaction() throws Exception {

		final StripedKeyLockManager manager = new StripedKeyLockManager(Long.MAX_VALUE, TimeUnit.SECONDS);

		final CountDownLatch t1WorkUnitEntry = new CountDownLatch(1);

		final Thread t1 = new Thread() {
			@Override
			public void run() {
				manager.executeLocked("test", new LockCallback() {
					public void doInLock() {
						try {
							t1WorkUnitEntry.countDown();
							sleep(Long.MAX_VALUE);
						} catch (final InterruptedException e) {
							return;
						}
					}
				});
			}
		};
		t1.start();

		t1WorkUnitEntry.await();

		// t2 waits for t1 before entering the work unit and gets interrupted
		// while waiting

		final Exchanger<KeyLockManagerException> exchanger = new Exchanger<KeyLockManagerException>();
		final Thread t2 = new Thread() {
			@Override
			public void run() {
				try {
					manager.executeLocked("test", new LockCallback() {
						public void doInLock() {
						}
					});
				} catch (final KeyLockManagerException e) {
					try {
						exchanger.exchange(e);
					} catch (final InterruptedException e1) {
						return;
					}
				}
			}
		};
		t2.start();

		while (manager.waitingThreadsCount() < 1) {
			Thread.sleep(10);
		}

		t2.interrupt();

		assertTrue(exchanger.exchange(null) instanceof KeyLockManagerInterruptedException);

		t1.interrupt();
		t1.join();
		t2.join();

		assertCleanup(manager);
	}

	/**
	 * one thread holds a lock on one key, a second thread on an other key must
	 * not block
	 */
	@Test
	public void testLockWithDifferentKeys() throws Exception {

		final StripedKeyLockManager manager = new StripedKeyLockManager(Long.MAX_VALUE, TimeUnit.SECONDS);

		final CountDownLatch t1WorkUnitEntry = new CountDownLatch(1);
		final CountDownLatch t2WorkUnitEntry = new CountDownLatch(1);

		final Thread t1 = new Thread() {
			@Override
			public void run() {
				manager.executeLocked("test1", new LockCallback() {
					public void doInLock() {
						try {
							t1WorkUnitEntry.countDown();
							sleep(Long.MAX_VALUE);
						} catch (final InterruptedException e) {
							return;
						}
					}
				});
			}
		};
		t1.start();

		t1WorkUnitEntry.await();

		assertEquals(0, manager.waitingThreadsCount());

		// t2 should not wait for t1 to enter the work unit

		final Thread t2 = new Thread() {
			@Override
			public void run() {
				manager.executeLocked("test2", new LockCallback() {
					public void doInLock() {
						try {
							t2WorkUnitEntry.countDown();
							Thread.sleep(Long.MAX_VALUE);
						} catch (final InterruptedException e) {
							return;
						}
					}
				});
			}
		};
		t2.start();

		// blocks for many days, if 'test2' is blocked by 'test1'
		t2WorkUnitEntry.await();

		t1.interrupt();
		t2.interrupt();
		t1.join();
		t2.join();

		assertCleanup(manager);
	}

	/**
	 * one thread holds a lock on one key and a other thread is waiting to
	 * acquire the lock in the same key - the first thread gets interrupted and
	 * the second can acquire the lock
	 */
	@Test
	public void testLockWithOneKey() throws Exception {

		final StripedKeyLockManager manager = new StripedKeyLockManager(Long.MAX_VALUE, TimeUnit.SECONDS);

		final CountDownLatch t1WorkUnitEntry = new CountDownLatch(1);

		final Thread t1 = new Thread() {
			@Override
			public void run() {
				manager.executeLocked("test", new LockCallback() {
					public void doInLock() {
						try {
							t1WorkUnitEntry.countDown();
							sleep(Long.MAX_VALUE);
						} catch (final InterruptedException e) {
							return;
						}
					}
				});
			}
		};
		t1.start();

		t1WorkUnitEntry.await();

		assertEquals(0, manager.waitingThreadsCount());

		// t2 waits for t1 to become interrupted to enter the work unit

		final Thread t2 = new Thread() {
			@Override
			public void run() {
				try {
					manager.executeLocked("test", new LockCallback() {
						public void doInLock() {
						}
					});
				} catch (final KeyLockManagerInterruptedException e) {
					return;
				}
			}
		};
		t2.start();

		while (manager.waitingThreadsCount() < 1) {
			Thread.sleep(10);
		}

		assertEquals(1, manager.waitingThreadsCount());

		t1.interrupt();
		t1.join();

		// wait many days if the lock is not freed after interruption of t1
		t2.join();

		assertCleanup(manager);
	}

	/**
	 * one thread holds a lock for one key while holding a lock on an other key
	 */
	@Test
	public void testNestedUse() throws Exception {

		final StripedKeyLockManager manager = new StripedKeyLockManager(500, TimeUnit.MILLISECONDS);

		final CountDownLatch workUnitEntry = new CountDownLatch(1);
		final CountDownLatch workUnitExit = new CountDownLatch(1);

		final Thread t1 = new Thread() {
			@Override
			public void run() {
				manager.executeLocked("test", new LockCallback() {
					public void doInLock() {
						manager.executeLocked("test2", new LockCallback() {
							public void doInLock() {
								try {
									workUnitEntry.countDown();
									workUnitExit.await();
								} catch (final InterruptedException e) {
									e.printStackTrace();
								}
							}
						});
					}
				});
			}
		};
		t1.start();

		workUnitEntry.await(); // waits many days if lock is not released

		assertEquals(2, manager.activeKeyLocksCount());
		assertEquals(0, manager.waitingThreadsCount());

		workUnitExit.countDown();

		t1.join();

		assertCleanup(manager);
	}

	/**
	 * one thread enters a locked block for one key while holding a lock block
	 * on the same key
	 */
	@Test
	public void testReentrantBehavoir() throws Exception {

		final StripedKeyLockManager manager = new StripedKeyLockManager(10, TimeUnit.SECONDS);

		final CountDownLatch workUnitEntry = new CountDownLatch(1);
		final CountDownLatch workUnitExit = new CountDownLatch(1);

		final Thread t1 = new Thread() {
			@Override
			public void run() {
				manager.executeLocked("test", new LockCallback() {
					public void doInLock() {
						manager.executeLocked("test", new LockCallback() {
							public void doInLock() {
								try {
									workUnitEntry.countDown();
									workUnitExit.await();
								} catch (final InterruptedException e) {
									e.printStackTrace();
								}
							}
						});
					}
				});
			}
		};
		t1.start();

		workUnitEntry.await(); // waits 10s, if lock is not released

		assertEquals(1, manager.activeKeyLocksCount());
		assertEquals(0, manager.waitingThreadsCount());

		workUnitExit.countDown();

		t1.join();

		assertCleanup(manager);
	}

	/**
	 * one thread holds a lock on a key - a second thread waits to acquire the
	 * lock on the same key - the seconds thread stops waiting after a timeout
	 */
	@Test
	public void testTimeoutReaction() throws Exception {

		final StripedKeyLockManager manager = new StripedKeyLockManager(500, TimeUnit.MILLISECONDS);

		final CountDownLatch t1WorkUnitEntry = new CountDownLatch(1);

		final Thread t1 = new Thread() {
			@Override
			public void run() {
				manager.executeLocked("test", new LockCallback() {
					public void doInLock() {
						try {
							t1WorkUnitEntry.countDown();
							sleep(Long.MAX_VALUE);
						} catch (final InterruptedException e) {
							return;
						}
					}
				});
			}
		};
		t1.start();

		t1WorkUnitEntry.await();

		// t2 waits for t1 to enter the work unit and should timeout

		final Exchanger<KeyLockManagerException> exchanger = new Exchanger<KeyLockManagerException>();

		final Thread t2 = new Thread() {
			@Override
			public void run() {
				try {
					manager.executeLocked("test", new LockCallback() {
						public void doInLock() {
						}
					});
				} catch (final KeyLockManagerException e) {
					try {
						exchanger.exchange(e);
					} catch (final InterruptedException e1) {
						return;
					}
				}
			}
		};
		t2.start();

		assertTrue(exchanger.exchange(null) instanceof KeyLockManagerTimeoutException);

		assertEquals("lock was disposed to early", 1, manager.activeKeyLocksCount());

		t1.interrupt();
		t1.join();

		assertCleanup(manager);
	}

	/**
	 * one thread holds a lock on one key - a second thread waits to acquire the
	 * lock on the same key - the first thread releases the lock - the second
	 * thread enters the lock
	 */
	@Test
	public void testUnlockAfterNormalWorkUnitExit() throws Exception {

		final StripedKeyLockManager manager = new StripedKeyLockManager(500, TimeUnit.MILLISECONDS);

		final CountDownLatch t1WorkUnitEntry = new CountDownLatch(1);
		final CountDownLatch t1SignalToExit = new CountDownLatch(1);

		final Thread t1 = new Thread() {
			@Override
			public void run() {
				manager.executeLocked("test", new LockCallback() {
					public void doInLock() {
						try {
							t1WorkUnitEntry.countDown();
							t1SignalToExit.await();
						} catch (final InterruptedException e) {
							return;
						}
					}
				});
			}
		};
		t1.start();

		t1WorkUnitEntry.await();

		final Thread t2 = new Thread() {
			@Override
			public void run() {
				manager.executeLocked("test", new LockCallback() {
					public void doInLock() {
					}
				});
			}
		};
		t2.start();

		while (manager.waitingThreadsCount() < 1) {
			Thread.sleep(10);
		}

		t1SignalToExit.countDown();

		t1.join();
		t2.join(); // t2 waits many days if lock is not released

		assertCleanup(manager);
	}

	@Test
	public void testWithReturnValueCallback() {
		final StripedKeyLockManager manager = new StripedKeyLockManager(10, TimeUnit.SECONDS);

		assertEquals(Integer.valueOf(20), manager.executeLocked("test", new ReturnValueLockCallback<Integer>() {
			public Integer doInLock() {
				return 20;
			}
		}));

		assertCleanup(manager);
	}
}
