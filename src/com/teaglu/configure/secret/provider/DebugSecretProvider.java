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

public class DebugSecretProvider implements SecretProvider {
	private static final Logger log= LoggerFactory.getLogger(DebugSecretProvider.class);
	
	private final Properties secrets= new Properties();
		
	private DebugSecretProvider(
			@NonNull String path) throws ConfigException
	{
		log.warn("Using debug secret provider from secret file " + path);
		
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
		return new DebugSecretProvider(dockerSecret);
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
