package com.teaglu.configure.config;

/**
 * ConfigManager
 * 
 * A config manager monitors a configuration source and sends updates to a ConfigTarget on
 * startup and if anything changes.  You don't have to use a manager - you can just call the
 * methods of the ConfigSource yourself.
 * 
 */
public interface ConfigManager {
	/**
	 * start
	 * 
	 * Start any threads for monitoring, and send the initial configuration as soon as it is
	 * available.  The initial configuration may be sent from this thread as well, so be sure
	 * the target is ready to receive configuration as soon as you call start().
	 */
	public void start();
	
	/**
	 * stop
	 * 
	 * Stop any background threads and release any resources.  It's guaranteed that no calls
	 * to targets will be made after this returns.
	 */
	public void stop();
}
