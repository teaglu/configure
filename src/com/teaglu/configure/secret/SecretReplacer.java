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
