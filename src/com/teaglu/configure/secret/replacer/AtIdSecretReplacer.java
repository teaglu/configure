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

package com.teaglu.configure.secret.replacer;

import org.eclipse.jdt.annotation.NonNull;

import com.teaglu.configure.exception.ConfigException;
import com.teaglu.configure.secret.SecretProvider;
import com.teaglu.configure.secret.SecretReplacer;

/**
 * AtIdSecretReplacer
 * 
 * A secret replacer that looks for an at-sign (@) as the first character, and if found replaces
 * the entirety of the value with the secret named after the at-sign.  This style of replacer
 * can't handle replacing part of a value, but usually accomplishes what we want.
 *
 */
public class AtIdSecretReplacer implements SecretReplacer {
	private @NonNull SecretProvider secretProvider;
	
	private AtIdSecretReplacer(@NonNull SecretProvider secretProvider) {
		this.secretProvider= secretProvider;
	}
	
	public static @NonNull SecretReplacer Create(
			@NonNull SecretProvider secretProvider)
	{
		return new AtIdSecretReplacer(secretProvider);
	}
	
	@Override
	public @NonNull String replace(
			@NonNull String input) throws ConfigException
	{
		String output= input;
		
		if (input.startsWith("@")) {
			@SuppressWarnings("null")
			@NonNull String varName= input.substring(1);
			
			output= secretProvider.getSecret(varName);
		}
		
		return output;
	}
}
