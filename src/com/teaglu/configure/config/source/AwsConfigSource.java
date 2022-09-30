package com.teaglu.configure.config.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.Base64;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.teaglu.composite.Composite;
import com.teaglu.composite.exception.SchemaException;
import com.teaglu.composite.json.JsonComposite;
import com.teaglu.configure.config.ConfigSource;
import com.teaglu.configure.exception.ConfigException;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.AppConfigClientBuilder;
import software.amazon.awssdk.services.appconfig.model.GetConfigurationRequest;
import software.amazon.awssdk.services.appconfig.model.GetConfigurationResponse;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.SetAlarmStateRequest;

/**
 * AwsConfigSource
 * 
 * A configuration source that pulls from AWS AppConfig.  This doesn't take any credentials
 * because it is assumed we're running in AWS and access has been granted via execution role
 * or similar.
 *
 */
public class AwsConfigSource implements ConfigSource {
	private static final Logger log= LoggerFactory.getLogger(AwsConfigSource.class);
	
	private @NonNull String applicationId;
	private @NonNull String configurationId;
	private @NonNull String environmentId;
	
	private @NonNull String clientId;
	
	private Composite config;
	private String configVersion;
	private boolean configPending;
	
	AppConfigClient client;
	String token;
	
	private String alarmName;
	
	private AwsConfigSource(
			@NonNull String applicationId,
			@NonNull String configurationId,
			@NonNull String environmentId,
			@Nullable String alarmName)
	{
		this.applicationId= applicationId;
		this.configurationId= configurationId;
		this.environmentId= environmentId;
		this.alarmName= alarmName;

		// As far as I know the client doesn't have any state to go bad.
		AppConfigClientBuilder builder= AppConfigClient.builder();
		
		// You would think you could apply the application/config/environment here but you can't
		client= builder.build();

		// Generate a new client ID.  This lets AppConfig do a phased rollout by knowing which
		// requests come from the same client - otherwise it can't do the math.		
		byte[] idBytes= new byte[18];
		SecureRandom rand= new SecureRandom();
		rand.nextBytes(idBytes);
		
		@SuppressWarnings("null")
		@NonNull String tmpClientId= Base64.getEncoder().encodeToString(idBytes);
		
		clientId= tmpClientId;
	}
	
	public static @NonNull ConfigSource Create(
			@NonNull String applicationId,
			@NonNull String configurationId,
			@NonNull String environmentId,
			@Nullable String alarmName)
	{
		return new AwsConfigSource(
				applicationId, configurationId, environmentId, alarmName);
	}
	
	private void refresh() throws ConfigException {
		try {
			GetConfigurationRequest.Builder requestBuilder= GetConfigurationRequest.builder();
			requestBuilder.environment(environmentId);
			requestBuilder.application(applicationId);
			requestBuilder.configuration(configurationId);
			requestBuilder.clientId(clientId);
			
			if (configVersion != null) {
				// Including the last known version will make this a non-charged call if there
				// is no change.  Or maybe a less-charged call, I'm not sure.
				requestBuilder.clientConfigurationVersion(configVersion);
			}
			
			GetConfigurationRequest request= requestBuilder.build();

			// We're supposed to start using the StartConfigurationSession and
			// GetLatestConfiguration APIs but those don't seem to be part of the Java SDK
			// yet - or else I just can't find them.
			
			@SuppressWarnings("deprecation")
			GetConfigurationResponse result= client.getConfiguration(request);
			
			String newConfigVersion= result.configurationVersion();
			
			configPending= true;
			if (configVersion != null) {
				if (newConfigVersion.equals(configVersion)) {
					configPending= false;
				}
			}
	
			if (configPending) {
				// Save for the next call
				configVersion= newConfigVersion;
				
				SdkBytes contentBytes= result.content();
				
				@SuppressWarnings("null")
				@NonNull InputStream contentStream= contentBytes.asInputStream();
				
				try {
					String contentType= result.contentType();
					switch (contentType) {
					case "application/json":
						try (InputStreamReader reader= new InputStreamReader(contentStream)) {
							config= JsonComposite.Parse(reader);
						}
						break;
						
					default:
						throw new ConfigException(
								"AppConfig returned unknown content type " + contentType);
					}
				} catch (SchemaException e) {
					throw new ConfigException(
							"Error parsing configuration", e);
				} catch (IOException e) {
					throw new ConfigException(
							"Unable to read configuration input stream", e);
				}
			}
		} catch (SdkException e) {
			log.error("Error retrieving configuration from AppConfig", e);
			
			throw new ConfigException(
					"Error retrieving configuration from AppConfig");
		}
	}
	
	@Override
	public boolean needsReload() {
		try {
			refresh();
		} catch (ConfigException e) {
		}

		return configPending;
	}

	@Override
	public @NonNull Composite reload() throws ConfigException {
		Composite rval= config;
		if (rval == null) {
			refresh();
			rval= config;
		}
		
		if (rval == null) {
			throw new RuntimeException(
					"Logic error - config should never be null here");
		}
		
		configPending= false;
		
		return rval;
	}

	@Override
	public void reportSuccess() {
	}

	@Override
	public void reportFailure(@NonNull String code, @NonNull String message) {
		if (alarmName != null) {
			try {
				CloudWatchClient cloudWatchClient= CloudWatchClient.create(); 
				
				SetAlarmStateRequest.Builder requestBuilder= SetAlarmStateRequest.builder();
				requestBuilder.alarmName(alarmName);
				
				SetAlarmStateRequest request= requestBuilder.build();
				
				cloudWatchClient.setAlarmState(request);
			} catch (SdkException e) {
				log.error("Error setting cloudwatch alarm");
			}
		}
	}
}
