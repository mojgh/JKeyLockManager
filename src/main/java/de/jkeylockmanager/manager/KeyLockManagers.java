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

package de.jkeylockmanager.manager;

import de.jkeylockmanager.manager.implementation.lockstripe.StripedKeyLockManager;

import java.util.concurrent.TimeUnit;

/**
 *
 * This class contains factory methods to create new instances of {@link KeyLockManager}.
 *
 * @author Marc-Olaf Jaschke
 *
 */
public final class KeyLockManagers {

	/**
	 * Default lock wait time in seconds.
	 */
	public static final int DEFAULT_LOCK_TIMEOUT = 1; // 1 h


	/**
	 * Returns a new {@link KeyLockManager} with default settings. The best available multi purpose implementation is
	 * used.
	 *
	 * @return the newly created lock
	 */
	public static KeyLockManager newLock() {
		return new StripedKeyLockManager(DEFAULT_LOCK_TIMEOUT, TimeUnit.HOURS);
	}

	/**
	 * Returns a new {@link KeyLockManager} with the given timeout settings. The best available multi purpose
	 * implementation is used.
	 *
	 * @param lockTimeout
	 *            the time to wait for a lock before a Exception is thrown - must be greater than 0
	 * @param lockTimeoutUnit
	 *            the unit for lockTimeout - must not be null
	 *
	 * @return the newly created lock
	 */
	public static KeyLockManager newLock(final long lockTimeout, final TimeUnit lockTimeoutUnit) {
		return new StripedKeyLockManager(lockTimeout, lockTimeoutUnit);
	}



	/**
	 * Prevent instantiation.
	 */
	private KeyLockManagers() {
	}
}
