package com.teaglu.configure.config;

import org.eclipse.jdt.annotation.NonNull;

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
	 * @throws ConfigException			Configuration error
	 */
	public @NonNull Composite reload() throws ConfigException;
	
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
	 */
	public void reportFailure(@NonNull String code, @NonNull String message);
}
