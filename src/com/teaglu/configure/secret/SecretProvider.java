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
 * SecretProvider
 * 
 * A secret provider is something that can provide authentication secrets from a secure enclave.
 */
public interface SecretProvider {
	/**
	 * getSecret
	 * 
	 * Get the value of a secret
	 * 
	 * @param name						Name of the secret
	 * @return							Secret value
	 * 
	 * @throws ConfigException			Schema error, or secret not defined
	 */
	public @NonNull String getSecret(
			@NonNull String name) throws ConfigException;
}
