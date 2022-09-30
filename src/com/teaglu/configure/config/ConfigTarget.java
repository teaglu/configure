package com.teaglu.configure.config;

import org.eclipse.jdt.annotation.NonNull;

import com.teaglu.composite.Composite;

/**
 * ConfigTarget
 *
 * A configuration target is whatever will take the configuration and launch it.  It is normally
 * passed into the constructor of a configuration manager.
 */
public interface ConfigTarget {
	/**
	 * apply
	 * 
	 * Apply the new configuration.  The implementation may choose to destroy the old configuration
	 * before attempting to rez the new one, or it may have a mechanism to roll back in the case
	 * of failure.
	 *
	 * If the upstream source supports notification, it will be notified of either success or
	 * failure depending on if an exception is thrown by the apply() call.
	 * 
	 * @param config					The new configuration
	 * 
	 * @throws Exception				Anything that goes wrong
	 */
	public void apply(@NonNull Composite config) throws Exception;
	
	/**
	 * shutdown
	 * 
	 * Requests a system shutdown in the case a configuration cannot be retrieved in a reasonable
	 * amount of time, or if the implementation has a reason to believe the running configuration
	 * is not correct.
	 * 
	 */
	public void shutdown();
}
