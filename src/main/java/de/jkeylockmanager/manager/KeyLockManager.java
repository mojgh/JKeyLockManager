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

import de.jkeylockmanager.manager.exception.KeyLockManagerInterruptedException;
import de.jkeylockmanager.manager.exception.KeyLockManagerTimeoutException;

/**
 * Protect a unit of work from concurrent access based on a key. Only one thread
 * can enter the locked unit of work for a given key. Other threads that work on
 * the same key concurrently have to wait until the first thread leaves the
 * locked unit.
 * 
 * All implementations have to be reentrant and interruptible and must have a
 * defined timeout mechanism.
 * 
 * @author Marc-Olaf Jaschke
 * 
 */
public interface KeyLockManager {

	/**
	 * Executes the given callback with protection against concurrent access for
	 * the given key.
	 * 
	 * 
	 * @param key
	 *            the key used to block concurrent access - must not be null
	 * @param callback
	 *            the template to protect against concurrent access - must not
	 *            be null
	 * 
	 * @throws KeyLockManagerInterruptedException
	 *             if the current thread becomes interrupted while waiting for a
	 *             lock
	 * @throws KeyLockManagerTimeoutException
	 *             if the instance wide waiting time is exceeded, while waiting
	 *             for a lock
	 */
	void executeLocked(Object key, LockCallback callback);

	/**
	 * Executes the given callback with protection against concurrent access for
	 * the given key and return the result of the computation done in the
	 * callback
	 * 
	 * 
	 * @param key
	 *            the key to block concurrent access - must not be null
	 * @param callback
	 *            the template to protect against concurrent access - must not
	 *            be null
	 * 
	 * @throws KeyLockManagerInterruptedException
	 *             if the current thread becomes interrupted while waiting for a
	 *             lock
	 * @throws KeyLockManagerTimeoutException
	 *             if the instance wide waiting time is exceeded, while waiting
	 *             for lock
	 * 
	 * @return result of the computation done in the callback
	 */
	<R> R executeLocked(Object key, ReturnValueLockCallback<R> callback);

}
