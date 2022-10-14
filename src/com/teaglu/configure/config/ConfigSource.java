package com.teaglu.configure.config;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.teaglu.composite.Composite;
import com.teaglu.configure.exception.ConfigException;

/**
 * ConfigSource
 * 
 * A configuration source is a source for configuration information.  It can be a local file,
 * or something pulled from the internet or the environment.
 *
 */
public interface ConfigSource {
	/**
	 * needsReload
	 * 
	 * Returns whether the configuration should be reloaded.  If this returns true,
	 * the caller should call reload() to load a new configuration.  This call should be made
	 * immediately, as several source implementations cache the result between the needsReload()
	 * and reload() calls.
	 * 
	 * @return							If a reload is needed
	 */
	public boolean needsReload();
	
	/**
	 * reload
	 * 
	 * Load a configuration from wherever it's kept.  You don't have to call needsReload for
	 * each call to reload, but it might be more efficient if you do.
	 * 
	 * @return							The new configuration
	 * 
	 * @throws ConfigException			Error parsing or processing configuration
	 * @throws IOException				Transient error retrieving configuration
	 */
	public @NonNull Composite reload() throws ConfigException, IOException;
	
	/**
	 * reportSuccess
	 * 
	 * Reports a configuration was loaded successfully
	 */
	public void reportSuccess();
	
	/**
	 * reportFailure
	 * 
	 * Reports a launch failure
	 * 
	 * @param code						Programmatic code
	 * @param message					Text reason or more data
	 * @param cause						The exception that occurred
	 */
	public void reportFailure(
			@NonNull String code,
			@NonNull String message,
			@Nullable Throwable cause);
}
