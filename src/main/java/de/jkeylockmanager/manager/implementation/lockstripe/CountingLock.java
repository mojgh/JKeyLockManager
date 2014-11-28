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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import de.jkeylockmanager.manager.exception.KeyLockManagerInterruptedException;
import de.jkeylockmanager.manager.exception.KeyLockManagerTimeoutException;

/**
 * 
 * Special lock implementation for internal use in this package only.
 * 
 * {@link CountingLock} adds a counter for counting its uses.
 * 
 * The counting functionality is not thread safe and so it is essential to use
 * the following methods only in the scope of a shared lock:
 * 
 * {@link #decrementUses()}, {@link #incrementUses()}, {@link #isUsed()}
 * 
 * 
 * @see ReentrantLock
 * 
 * @author Marc-Olaf Jaschke
 * 
 */
final class CountingLock {

	private final ReentrantLock delegate = new ReentrantLock();
	private final long lockTimeout;
	private final TimeUnit lockTimeoutUnit;
	private long uses = 0;

	/**
	 * Creates a new instance of {@link CountingLock} with a usage counter set
	 * to zero.
	 * 
	 * @param lockTimeout
	 *            - the time to wait for a lock before an Exception is thrown -
	 *            must be greater than 0
	 * @param lockTimeoutUnit
	 *            - the unit for lockTimeout - must not be null
	 */
	CountingLock(final long lockTimeout, final TimeUnit lockTimeoutUnit) {
		assert lockTimeout > 0 : "contract broken: lockTimeout > 0";
		assert lockTimeoutUnit != null : "contract broken: lockTimeoutUnit != null";

		this.lockTimeout = lockTimeout;
		this.lockTimeoutUnit = lockTimeoutUnit;
	}

	/**
	 * Decrements the usage counter. See class commentary for thread safety!
	 */
	void decrementUses() {
		uses--;
	}

	/**
	 * Delegates to {@link ReentrantLock#getQueueLength()}
	 */
	int getQueueLength() {
		return delegate.getQueueLength();
	}

	/**
	 * Increments the usage counter. See class commentary for thread safety!
	 */
	void incrementUses() {
		uses++;
	}

	/**
	 * See class commentary for thread safety!
	 * 
	 * @return true, if the usage counter is zero
	 */
	boolean isUsed() {
		return uses != 0;
	}

	/**
	 * Decorates {@link ReentrantLock#tryLock(long, TimeUnit)}.
	 * 
	 * @throws KeyLockManagerInterruptedException
	 *             if the current thread becomes interrupted while waiting for
	 *             the lock
	 * @throws KeyLockManagerTimeoutException
	 *             if the instance wide waiting time is exceeded
	 */
	void tryLock() {
		try {
			if (!delegate.tryLock(lockTimeout, lockTimeoutUnit)) {
				throw new KeyLockManagerTimeoutException(lockTimeout, lockTimeoutUnit);
			}
		} catch (final InterruptedException e) {
			throw new KeyLockManagerInterruptedException();
		}
	}

	/**
	 * Delegates to {@link java.util.concurrent.locks.ReentrantLock#unlock()}
	 */
	void unlock() {
		delegate.unlock();
	}

}
