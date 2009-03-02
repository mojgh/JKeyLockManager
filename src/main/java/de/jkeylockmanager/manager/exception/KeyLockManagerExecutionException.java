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

package de.jkeylockmanager.manager.exception;

/**
 * Use this exception if a computation in a key lock manager is aborted by
 * throwing an exception. This exception can be inspected using the
 * {@link #getCause()} method.
 * 
 * @author Marc-Olaf Jaschke
 * 
 */
public class KeyLockManagerExecutionException extends KeyLockManagerException {

	private static final long serialVersionUID = -2978888391122533260L;

	public KeyLockManagerExecutionException(final Exception cause) {
		super("computation in lock caused an exception", cause);
	}

	@Override
	public Exception getCause() {
		return (Exception) super.getCause();
	}
}