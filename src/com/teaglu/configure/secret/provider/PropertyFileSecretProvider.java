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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.teaglu.configure.exception.ConfigException;
import com.teaglu.configure.secret.SecretProvider;

public class PropertyFileSecretProvider implements SecretProvider {
	private static final Logger log= LoggerFactory.getLogger(PropertyFileSecretProvider.class);
	
	private final Properties secrets= new Properties();
		
	private PropertyFileSecretProvider(
			@NonNull String path) throws ConfigException
	{
		File secretFile= new File(path);
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
		return new PropertyFileSecretProvider(dockerSecret);
	}

	@Override
	public @NonNull String getSecret(
			@NonNull String name) throws ConfigException
	{
		String rval= secrets.getProperty(name);
		if (rval == null) {
			throw new ConfigException("Secret " + name + " is not defined");
		}
		
		log.info("Deferenced secret " + name);
		
		return rval;
	}
}
