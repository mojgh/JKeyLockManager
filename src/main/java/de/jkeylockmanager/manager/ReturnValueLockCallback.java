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

/**
 * Implement this interface for callbacks with a return value, that should be
 * executed in a lock.
 * 
 * 
 * @author Marc-Olaf Jaschke
 * 
 */
public interface ReturnValueLockCallback<R> {

	/**
	 * This method is called inside the lock.
	 *
	 */
	R doInLock();

}
