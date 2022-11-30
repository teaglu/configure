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

package com.teaglu.configure.secret.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.eclipse.jdt.annotation.NonNull;

import com.teaglu.configure.exception.ConfigException;
import com.teaglu.configure.secret.SecretProvider;

public class DockerSecretProvider implements SecretProvider {
	private final Properties secrets= new Properties();
	
	private boolean isValidFilePart(@NonNull String file) {
		if (file.equals(".")) {
			return false;
		}
		if (file.equals("..")) {
			return false;
		}
		
		char[] chars= file.toCharArray();
		for (int pos= 0; pos < chars.length; pos++) {
			char c= chars[pos];
			if ((c >= 'a') && (c <= 'z')) {
			} else if ((c >= 'A') && (c <= 'Z')) {
			} else if ((c >= '0') && (c <= '9')) {
			} else if (c == '.') {
			} else if (c == '_') {
			} else {
				return false;
			}
		}
		
		return true;
	}
	
	private DockerSecretProvider(
			@NonNull String dockerSecret) throws ConfigException
	{
		if (!isValidFilePart(dockerSecret)) {
			throw new ConfigException("Secret name is not a valid unix filename");
		}
		
		File secretFile= new File("/run/secrets/" + dockerSecret);
		if (!secretFile.exists()) {
			throw new ConfigException(
					"Secret file " + secretFile.getAbsolutePath() + " does not exist");
		}

		try (InputStream file= new FileInputStream(secretFile)) {
			secrets.load(file);
		} catch (IOException e) {
			throw new ConfigException(
					"IO Error reading secrets file " + secretFile.getAbsolutePath(), e);
		}
	}
	
	public static @NonNull SecretProvider Create(
			@NonNull String dockerSecret) throws ConfigException
	{
		return new DockerSecretProvider(dockerSecret);
	}

	@Override
	public @NonNull String getSecret(
			@NonNull String name) throws ConfigException
	{
		String rval= secrets.getProperty(name);
		if (rval == null) {
			throw new ConfigException("Secret " + name + " is not defined");
		}
		
		return rval;
	}
}
