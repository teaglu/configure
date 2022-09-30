package com.teaglu.configure.secret.provider;

import org.eclipse.jdt.annotation.NonNull;

import com.teaglu.configure.exception.ConfigException;
import com.teaglu.configure.secret.SecretProvider;

public class NullSecretProvider implements SecretProvider {
	private NullSecretProvider() {}
	
	public static @NonNull SecretProvider Create() {
		return new NullSecretProvider();
	}
	
	@Override
	public @NonNull String getSecret(
			@NonNull String name) throws ConfigException
	{
		throw new ConfigException("No secret provider is configured");
	}
}
