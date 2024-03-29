/****************************************************************************
 * Copyright 2022 Teaglu, LLC                                               *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *   http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.teaglu.configure.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.teaglu.configure.config.manager.ImmuntableConfigManager;
import com.teaglu.configure.config.manager.PollingConfigManager;
import com.teaglu.configure.config.parser.JsonConfigParser;
import com.teaglu.configure.config.parser.YamlConfigParser;
import com.teaglu.configure.config.source.AwsAppConfigSource;
import com.teaglu.configure.config.source.FileConfigSource;
import com.teaglu.configure.config.source.SmbtrackConfigSource;
import com.teaglu.configure.config.source.UrlConfigSource;
import com.teaglu.configure.exception.ConfigException;
import com.teaglu.configure.secret.SecretReplacer;
import com.teaglu.configure.uri.Uri;
import com.teaglu.configure.uri.UriImpl;

/**
 * ConfigManagerFactory
 * 
 * This is a singleton convenience class to create a configuration source and configuration
 * manager based on a URI-formatted string.  The local part (after the #) is interpreted as
 * a query-string style value specifying addition local processing options.
 * 
 * View the project README for the list of supported schemas.
 * 
 */
public class ConfigManagerFactory {
	private ConfigManagerFactory() {}
	
	private static ConfigManagerFactory instance;
	
	public static synchronized @NonNull ConfigManagerFactory getInstance() {
		if (instance == null) {
			instance= new ConfigManagerFactory();
		}
		
		@SuppressWarnings("null")
		@NonNull ConfigManagerFactory rval= instance;
		
		return rval;
	}

	private static final Pattern DOCKER_SECRET_PATTERN= Pattern.compile("^[a-zA-Z0-9_]+$");
	
	public @NonNull ConfigManager createFromEnvironment(
			@NonNull ConfigTarget configTarget,
			@NonNull SecretReplacer secretReplacer) throws ConfigException
	{
		String configString= System.getenv("CONFIGURATION");
		if (configString == null) {
			configString= System.getenv("CONFIG");
		}
		if (configString == null) {
			throw new ConfigException(
					"CONFIGURATION environment variable is not defined");
		}
		
		return createFromString(configString, configTarget, secretReplacer);
	}
	
	public @NonNull ConfigManager createFromString(
			@NonNull String configUri,
			@NonNull ConfigTarget configTarget,
			@Nullable SecretReplacer secretReplacer) throws ConfigException
	{
		Uri uri= UriImpl.CreateFromString(configUri);

		switch (uri.getSchema()) {
		case "http":
		case "https":
			// URL takes the whole string not the argument, since we're matching known
			// URL prefixes.
			return createUrlConfigManager(configUri, configTarget);
		
		case "docker":
			return createDockerConfigManager(uri, configTarget);
			
		case "debug":
			return createFileConfigManager(uri, configTarget, 15);
			
		case "file":
			return createFileConfigManager(uri, configTarget, 300);
			
		case "aws":
			return createAwsConfigManager(uri, configTarget);
			
		case "smbtrack":
			return createSmbtrackConfigManager(uri, configTarget);
		
		default:
			throw new ConfigException(
					"Configuration schema " + uri.getSchema() + " is not defined.");
		}
	}
	
	private String guessFormatByExtension(@NonNull String path) {
		String format= null;
		
		// Try to figure out the format by the extension
		int extensionIndex= path.lastIndexOf('.');
		if (extensionIndex > 0) {
			String extension= path.substring(extensionIndex + 1);
			switch (extension) {
			case "json":
				format= "json";
				break;
				
			case "yaml":
			case "yml":
				format= "yaml";
				break;
				
			default:
			}
		}
		
		return format;
	}
	
	private @NonNull ConfigManager createUrlConfigManager(
			@NonNull String configString,
			@NonNull ConfigTarget configTarget) throws ConfigException
	{
		// Use the entire configuration string as the URL
		ConfigSource source= UrlConfigSource.Create(configString);
		
		// There should probably be a method of declaring how often to poll - maybe
		// using a query parameter or something
		return PollingConfigManager.Create(source, configTarget, 300);
	}
	
	/**
	 * createDockerConfigManager
	 * 
	 * Create a configuration manager for docker secrets.  Since docker secrets are immutable
	 * we use an ImmutableConfigManager to just read the config once on startup.
	 * 
	 * @param arguments					Arguments from config string
	 * @param configTarget				Config target
	 * 
	 * @return							New manager
	 * 
	 * @throws ConfigException			Anything that went wrong
	 */
	private @NonNull ConfigManager createDockerConfigManager(
			@NonNull Uri uri,
			@NonNull ConfigTarget configTarget) throws ConfigException
	{
		if (uri.getPathSectionCount() != 1) {
			throw new ConfigException(
					"Docker configuration string does not have the correct number of path " +
					"components.  The correct format is \"docker://{secret}\".");
		}
		
		String secret= uri.getPathSection(0);
		Matcher matcher= DOCKER_SECRET_PATTERN.matcher(secret);
		if (!matcher.matches()) {
			throw new ConfigException(
					"Docker secret name contains illegal characters");
		}
		
		String path= "/run/secrets/" + secret;

		ConfigParser parser= null;
		
		String format= uri.getLocalArgument("format");
		if (format == null) {
			format= guessFormatByExtension(secret);
		}
		if (format == null) {
			format= "json";
		}
		
		switch (format) {
		case "json":
			parser= JsonConfigParser.Create();
			break;
		
		case "yaml":
			parser= YamlConfigParser.Create();
			break;
			
		default:
			throw new ConfigException("Unknown configuration format " + format);
		}
		
		ConfigSource source= FileConfigSource.Create(path, parser);
		return ImmuntableConfigManager.Create(source, configTarget);
	}

	/**
	 * createFileConfigManager
	 * 
	 * Create a configuration manager that reads an arbitrary file.  The entire argument is
	 * used as the path - this makes it more convenient to debug on Windows machines where a
	 * colon is a normal part of the path.
	 * 
	 * @param path						Path to file
	 * @param configTarget				Target to build for
	 * @param pollSeconds				How often to poll
	 * 
	 * @return							New manager
	 * 
	 * @throws ConfigException			Something failed
	 */
	private @NonNull ConfigManager createFileConfigManager(
			@NonNull Uri uri,
			@NonNull ConfigTarget configTarget,
			int pollSeconds) throws ConfigException
	{
		String path= uri.getPathAsLocal();
		
		String format= uri.getLocalArgument("format");
		if (format == null) {
			format= guessFormatByExtension(path);
		}
		if (format == null) {
			format= "json";
		}

		ConfigParser parser= null;
		switch (format) {
		case "json":
			parser= JsonConfigParser.Create();
			break;
			
		case "yaml":
			parser= YamlConfigParser.Create();
			break;
			
		default:
			throw new ConfigException("File format " + format + " is not implemented.");
		}
		
		ConfigSource source= FileConfigSource.Create(path, parser);
		
		return PollingConfigManager.Create(source, configTarget, pollSeconds);
	}
	
	/**
	 * createSmbtrackConfigManager
	 * 
	 * Create a configuration manager for a SMBTrack-managed configuration.
	 * 
	 * @param arguments					Arguments from config string
	 * @param configTarget				Config target
	 * 
	 * @return							New manager
	 * 
	 * @throws ConfigException			Anything that went wrong
	 */
	private @NonNull ConfigManager createSmbtrackConfigManager(
			@NonNull Uri uri,
			@NonNull ConfigTarget configTarget) throws ConfigException
	{
		if (uri.getPathSectionCount() != 2) {
			throw new ConfigException(
					"SMBTrack configuration string does not have the correct number of path " +
					"components.  The correct format is \"smbtrack://{host}/{token}\".");
		}
		
		String host= uri.getPathSection(0);
		String token= uri.getPathSection(1);
		
		String pollTimeString= uri.getLocalArgument("pollTime");
		int pollTime= 300;
		if (pollTimeString != null) {
			try {
				pollTime= Integer.parseInt(pollTimeString);
			} catch (NumberFormatException e) {
				throw new ConfigException(
						"Polling frequency is not a number", e);
			}
		}
		
		ConfigSource source= SmbtrackConfigSource.Create(host, token);				
		return PollingConfigManager.Create(source, configTarget, pollTime);
	}
	
	/**
	 * createAwsConfigManager
	 * 
	 * Create a configuration manager that reads from AWS AppConfig, and optionally sets a
	 * CloudWatch alarm in case of problems to trigger a rollback.
	 * 
	 * @param arguments					Arguments from config string
	 * @param configTarget				Config target
	 * 
	 * @return							New manager
	 * 
	 * @throws ConfigException			Anything that went wrong
	 */
	private @NonNull ConfigManager createAwsConfigManager(
			@NonNull Uri uri,
			@NonNull ConfigTarget configTarget) throws ConfigException
	{
		if (uri.getPathSectionCount() < 4) {
			throw new ConfigException(
					"AWS appconfig configuration string does not have the correct number of " +
					"path sections.  The correct format is \"aws://appconfig/{application}/" +
					"{configuration}/{environment}\".");
		}

		String service= uri.getPathSection(0);
		if (!service.equals("appconfig")) {
			throw new ConfigException(
					"The AWS configuration service " + service + " is not supported.");
		}
		
		String applicationId= uri.getPathSection(1);
		String configurationId= uri.getPathSection(2);
		String environmentId= uri.getPathSection(3);
		
		String alarmName= uri.getLocalArgument("alarm");

		String pollTimeString= uri.getLocalArgument("pollTime");
		int pollTime= 300;
		if (pollTimeString != null) {
			try {
				pollTime= Integer.parseInt(pollTimeString);
			} catch (NumberFormatException e) {
				throw new ConfigException(
						"Polling frequency is not a number", e);
			}
		}

		// The AppConfig session stuff requires you (for some reason) to pass in the minimum
		// required polling time, and it has a minimum of 15.  We're giving 15 seconds of slack
		// time so set our minimum at 30.
		if (pollTime < 30) {
			pollTime= 30;
		}
		
		ConfigSource source= AwsAppConfigSource.Create(
				applicationId, configurationId, environmentId, pollTime - 15, alarmName);
		
		return PollingConfigManager.Create(source, configTarget, pollTime);
	}
}
