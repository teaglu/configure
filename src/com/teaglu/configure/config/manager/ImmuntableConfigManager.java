package com.teaglu.configure.config.manager;

import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.teaglu.composite.Composite;
import com.teaglu.configure.config.ConfigManager;
import com.teaglu.configure.config.ConfigSource;
import com.teaglu.configure.config.ConfigTarget;

/**
 * ImmutableConfigManager
 * 
 * An immutable configuration manager attempts only once to load the configuration, and calls
 * shutdown() if it cannot load it.  This should be used in the case of immutable sources such
 * as docker secrets.
 *
 */
public class ImmuntableConfigManager implements ConfigManager {
	private static final Logger log= LoggerFactory.getLogger(ImmuntableConfigManager.class);
	
	private @NonNull ConfigSource configSource;
	private @NonNull ConfigTarget configTarget;
	
	private ImmuntableConfigManager(
			@NonNull ConfigSource configSource,
			@NonNull ConfigTarget configTarget)
	{
		this.configSource= configSource;
		this.configTarget= configTarget;
	}
	
	public static @NonNull ConfigManager Create(
			@NonNull ConfigSource configSource,
			@NonNull ConfigTarget configTarget)
	{
		return new ImmuntableConfigManager(configSource, configTarget);
	}
	
	@Override
	public void start() {
		// There's no reason to fire up a thread for this - directly try to apply the target,
		// and if that fails call shutdown.  Nothing is going to change.
		try {
			Composite config= configSource.reload();
		
			configTarget.apply(config);
		} catch (Exception e) {
			log.error("Unable to apply configuration, requesting shutdown", e);
			configTarget.shutdown();
		}
	}

	@Override
	public void stop() {
		// Nothing - we didn't start a thread.
	}
}
