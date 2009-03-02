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

package de.jkeylockmanager.contract;

/**
 * 
 * @author Marc-Olaf Jaschke
 * 
 */
public final class Contract {

	public static void isNotNull(final Object object, final String textualCondition) {
		isTrue(object != null, textualCondition);
	}

	public static void isTrue(final boolean condition, final String textualCondition) {
		if (!condition) {
			throw new ContractBrokenError(textualCondition == null ? "" : textualCondition);
		}
	}
}
