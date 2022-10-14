/****************************************************************************
 * Copyright (c) 2020 Teaglu, LLC
 * All Rights Reserved
 ****************************************************************************/

package com.teaglu.configure.config.manager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.teaglu.composite.Composite;
import com.teaglu.composite.json.JsonCompositeImpl;
import com.teaglu.configure.config.ConfigTarget;
import com.teaglu.configure.exception.ConfigException;
import com.teaglu.configure.config.ConfigManager;
import com.teaglu.configure.config.ConfigSource;

/**
 * RouteManagerImpl
 * 
 * This standard implementation of RouteManager periodically checks if the configuration
 * source needs a reload, and if so it reloads and applies the changes live.
 * 
 */
public class PollingConfigManager implements ConfigManager, Runnable {
	private static final Logger log= LoggerFactory.getLogger(PollingConfigManager.class);
	
	private ConfigSource configSource;
	private int reloadSeconds;
	
	private ConfigTarget configTarget;
	
	private MessageDigest nodeDigest;
	private Base64.Encoder base64Encoder;
	
	private String configDigest;
	
	private PollingConfigManager(
			@NonNull ConfigSource configSource,
			@NonNull ConfigTarget configTarget,
			int reloadSeconds)
	{
		this.configSource= configSource;
		this.configTarget= configTarget;
		this.reloadSeconds= reloadSeconds;
		
		try {
			nodeDigest= MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA not available");
		}
		base64Encoder= Base64.getEncoder();
	}
	
	public static @NonNull ConfigManager Create(
			@NonNull ConfigSource configSource,
			@NonNull ConfigTarget configTarget,
			int reloadSeconds)
	{
		return new PollingConfigManager(configSource, configTarget, reloadSeconds);
	}
	
    private void reload() {
    	if (configSource.needsReload()) {
    		try {
	    		Composite config= configSource.reload();
	    		
	    		boolean changed= true;
	    		if (configDigest != null) {
	    			String newDigest= null;
	    			
	    			if (config instanceof JsonCompositeImpl) {
	    				JsonObject rawObject= config.serialize(JsonObject.class);
	    				byte[] rawBytes= rawObject.toString().getBytes(StandardCharsets.UTF_8);
	    				byte[] rawDigest= nodeDigest.digest(rawBytes);

	    				newDigest= base64Encoder.encodeToString(rawDigest);
	    			}
	    			
	    			if (newDigest != null) {
	    				if (configDigest.equals(newDigest)) {
	    					changed= false;
	    				} else {
	    					configDigest= newDigest;
	    				}
	    			}
	    		}
	    		
	    		if (changed) {
		    		try {
		    			configTarget.apply(config);
		    		
		    			configSource.reportSuccess();
		    		} catch (Exception e) {
		    			log.error("Exception applying configuration", e);
		    			
		    			configSource.reportFailure("EX",
		    					"Exception applying configuration", e);
		    		}
	    		}
    		} catch (ConfigException e) {
    			log.error("Unable to reload configuration", e);
    			
    			configSource.reportFailure("RE",
    					"Exception reloading configuration", null);
    		} catch (IOException e) {
    			log.error("Unable to retrieve configuration", e);
    			
    			// If we get an IO exception we don't know whether it's on our end or the other
    			// end, but either way it probably can't be reached.  There's no reason to try
    			// to report a failure for most likely transient things.
    		}
    	}
    }
    
	private boolean stop= false;
	private final Semaphore wake= new Semaphore(0);
	private Thread thread;
	
	@Override
	public void run() {
		for (boolean run= true; run; ) {
			reload();
			
			synchronized (this) {
				run= !stop;
			}
			
			if (run) {
				try {
					wake.tryAcquire(reloadSeconds, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	public void start() {
		thread= new Thread(this, "configuration-poller");
		thread.start();
	}
	
	public void stop() {
		synchronized (this) {
			stop= true;
		}
		wake.release();
		
		try {
			thread.join();
		} catch (InterruptedException e) {
		}
	}
}
