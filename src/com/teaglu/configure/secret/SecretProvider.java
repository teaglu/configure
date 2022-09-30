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
