/****************************************************************************
 * Copyright 2022 Teaglu, LLC                                               *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *   http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.teaglu.configure.secret;

import org.eclipse.jdt.annotation.NonNull;

import com.teaglu.configure.exception.ConfigException;

/**
 * SecretReplacer
 * 
 * A SecretReplacer takes an input that's expected to be an inline secret credentials such as
 * a password, and if it matches a replacement pattern replaces the value with the actual value.
 * 
 * This is normally use to replace an inline reference from a config like "@db.foo.password" with
 * the actual value.
 *
 */
public interface SecretReplacer {
	/**
	 * replace
	 * 
	 * Resolve any replacement sequences in the input.  Either return the original string,
	 * or a modified string using replacement sequences.
	 * 
	 * @param input						Input
	 * @return							Output
	 * 
	 * @throws ConfigException
	 */
	@NonNull String replace(
			@NonNull String input) throws ConfigException;
}
