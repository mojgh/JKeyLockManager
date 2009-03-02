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

import static java.lang.Math.abs;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import de.jkeylockmanager.contract.Contract;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.LockCallback;
import de.jkeylockmanager.manager.ReturnValueLockCallback;
import de.jkeylockmanager.manager.exception.KeyLockManagerException;
import de.jkeylockmanager.manager.exception.KeyLockManagerExecutionException;

/**
 * Implementation of {@link KeyLockManager}.
 * 
 * Maintenance operations are implemented using lock striping.
 * 
 * All resources used by one key are freed immediately, if there is no longer a
 * thread in the locked block for this key.
 * 
 * @author Marc-Olaf Jaschke
 * 
 */
public final class StripedKeyLockManager implements KeyLockManager {

	private static final int NUMBER_OF_STRIPES = 16;

	private final ConcurrentHashMap<Object, CountingLock> key2lock = new ConcurrentHashMap<Object, CountingLock>();
	private final CountingLock[] stripes = new CountingLock[NUMBER_OF_STRIPES];
	private final long lockTimeout;
	private final TimeUnit lockTimeoutUnit;

	/**
	 * Creates a new instance of {@link StripedKeyLockManager} with the given
	 * settings.
	 * 
	 * @param lockTimeout
	 *            - the time to wait for a lock before a Exception is thrown -
	 *            must be greater than 0
	 * @param lockTimeoutUnit
	 *            - the unit for lockTimeout - must not be null
	 */
	public StripedKeyLockManager(final long lockTimeout, final TimeUnit lockTimeoutUnit) {
		Contract.isNotNull(lockTimeoutUnit, "lockTimeoutUnit != null");
		Contract.isTrue(lockTimeout > 0, "lockTimeout > 0");

		this.lockTimeout = lockTimeout;
		this.lockTimeoutUnit = lockTimeoutUnit;

		for (int i = 0; i < stripes.length; i++) {
			stripes[i] = new CountingLock(lockTimeout, lockTimeoutUnit);
		}
	}

	public final void executeLocked(final Object key, final LockCallback callback) {
		Contract.isNotNull(key, "key != null");
		Contract.isNotNull(callback, "callback != null");

		try {
			executeLockedInternal(key, new ReturnValueLockCallback<Object>() {
				public Object doInLock() throws Exception {
					callback.doInLock();
					return null;
				}
			});
		} catch (final KeyLockManagerException e) {
			throw e;
		} catch (final Exception e) {
			throw new KeyLockManagerExecutionException(e);
		}

	}

	public final <R> R executeLocked(final Object key, final ReturnValueLockCallback<R> callback) {
		Contract.isNotNull(key, "key != null");
		Contract.isNotNull(callback, "callback != null");

		try {
			return executeLockedInternal(key, callback);
		} catch (final KeyLockManagerException e) {
			throw e;
		} catch (final Exception e) {
			throw new KeyLockManagerExecutionException(e);
		}
	}

	private <R> R executeLockedInternal(final Object key, final ReturnValueLockCallback<R> callback) throws Exception {
		assert key != null : "contract broken: key != null";
		assert callback != null : "contract broken: callback != null";

		final CountingLock lock = getKeyLock(key);
		try {
			lock.tryLock();
			try {
				return callback.doInLock();
			} finally {
				lock.unlock();
			}
		} finally {
			freeKeyLock(key, lock);
		}
	}

	private final void freeKeyLock(final Object key, final CountingLock lock) {
		assert key != null : "contract broken: key != null";
		assert lock != null : "contract broken: lock != null";
		getStripedLock(key).tryLock();
		try {
			lock.decrementUses();
			if (!lock.isUsed()) {
				key2lock.remove(key);
			}
		} finally {
			getStripedLock(key).unlock();
		}
	}

	private final CountingLock getKeyLock(final Object key) {
		assert key != null : "contract broken: key != null";
		getStripedLock(key).tryLock();
		try {
			final CountingLock result;
			final CountingLock previousLock = key2lock.get(key);
			if (previousLock == null) {
				result = new CountingLock(lockTimeout, lockTimeoutUnit);
				key2lock.put(key, result);
			} else {
				result = previousLock;
			}
			result.incrementUses();
			return result;
		} finally {
			getStripedLock(key).unlock();
		}
	}

	private final CountingLock getStripedLock(final Object key) {
		assert key != null : "contract broken: key != null";
		return stripes[abs(key.hashCode() % stripes.length)];
	}

	/**
	 * for testing only
	 * 
	 * @return the number of currently active key locks
	 * 
	 */
	int activeKeyLocksCount() {
		return key2lock.size();
	}

	/**
	 * for testing only
	 * 
	 * @return the number of threads currently waiting in the queues of the key
	 *         locks
	 */
	int waitingThreadsCount() {
		int result = 0;
		for (final CountingLock lock : key2lock.values()) {
			result += lock.getQueueLength();
		}
		return result;
	}
}
