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

package com.teaglu.configure.config.source;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.teaglu.composite.Composite;
import com.teaglu.composite.exception.SchemaException;
import com.teaglu.composite.json.JsonComposite;
import com.teaglu.composite.yaml.YamlComposite;
import com.teaglu.configure.config.ConfigSource;
import com.teaglu.configure.exception.ConfigException;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClientBuilder;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.SetAlarmStateRequest;
import software.amazon.awssdk.services.cloudwatch.model.StateValue;

/**
 * AwsAppConfigSource
 * 
 * A configuration source that pulls from AWS AppConfig.  This doesn't take any credentials
 * because it is assumed we're running in AWS and access has been granted via execution role
 * or similar.
 *
 */
public class AwsAppConfigSource implements ConfigSource {
	private static final Logger log= LoggerFactory.getLogger(AwsAppConfigSource.class);
	
	private @NonNull String applicationId;
	private @NonNull String configurationId;
	private @NonNull String environmentId;
	
	private int minimumPollInterval;
	
	private Composite config;
	private boolean configPending;
	
	AppConfigClient client;
	String session;
	
	AppConfigDataClient dataClient;
	
	private String alarmName;
	
	private AwsAppConfigSource(
			@NonNull String applicationId,
			@NonNull String configurationId,
			@NonNull String environmentId,
			int minimumPollInterval,
			@Nullable String alarmName) throws ConfigException
	{
		this.applicationId= applicationId;
		this.configurationId= configurationId;
		this.environmentId= environmentId;
		this.alarmName= alarmName;
		this.minimumPollInterval= minimumPollInterval;

		// As far as I know the client doesn't have any state to go bad.
		AppConfigDataClientBuilder builder= AppConfigDataClient.builder();
		
		// You would think you could apply the application/config/environment here but you can't
		dataClient= builder.build();
	}
	
	public static @NonNull ConfigSource Create(
			@NonNull String applicationId,
			@NonNull String configurationId,
			@NonNull String environmentId,
			int minimumPollInterval,
			@Nullable String alarmName) throws ConfigException
	{
		return new AwsAppConfigSource(
				applicationId, configurationId, environmentId, minimumPollInterval, alarmName);
	}
	
	private void refresh() throws ConfigException {
		try {
			// If there's not a session in play we have to start one
			if (session == null) {
				StartConfigurationSessionRequest.Builder requestBuilder=
						StartConfigurationSessionRequest.builder();
				
				requestBuilder.environmentIdentifier(environmentId);
				requestBuilder.applicationIdentifier(applicationId);
				requestBuilder.configurationProfileIdentifier(configurationId);
				requestBuilder.requiredMinimumPollIntervalInSeconds(minimumPollInterval);
				
				StartConfigurationSessionRequest request= requestBuilder.build();
				StartConfigurationSessionResponse response=
						dataClient.startConfigurationSession(request);
				
				session= response.initialConfigurationToken();
			}
			
			GetLatestConfigurationRequest.Builder requestBuilder=
					GetLatestConfigurationRequest.builder();
			
			requestBuilder.configurationToken(session);
			GetLatestConfigurationRequest request= requestBuilder.build();			
			GetLatestConfigurationResponse response= dataClient.getLatestConfiguration(request);
			
			// The response will include the next token we're supposed to use.
			session= response.nextPollConfigurationToken();

			/*
			// I'm not quite sure what this is supposed to be used for - we want to poll when
			// we want to poll.  I guess we could back-link this to tell the polling manager
			// when to ask again, but it's a weird intrusion between layers.
			int nextPollSeconds= response.nextPollIntervalInSeconds();
			
			// For now just log it to see what's going on.
			log.info("Next poll seconds = " + nextPollSeconds);
			*/

			SdkBytes contentSdkBytes= response.configuration();
			ByteBuffer contentBytes= contentSdkBytes.asByteBuffer();
			
			if (contentBytes.remaining() > 0) {
				// I can't find it in the API what content encoding is used, or how to get
				// it back from the API response.  So be a little anal about the encoding.
				CharsetDecoder decoder= StandardCharsets.UTF_8.newDecoder();
				decoder.onMalformedInput(CodingErrorAction.REPORT);
				decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
				
				try {
					@SuppressWarnings("null")
					@NonNull String content= decoder.decode(contentBytes).toString();
				
					String contentType= response.contentType();
					switch (contentType) {
					case "application/json":
						config= JsonComposite.Parse(content);
						
						// Mark that a new configuration is available.
						configPending= true;
						break;

					case "application/yaml":
						config= YamlComposite.Parse(content);
						
						configPending= true;
						break;
						
					default:
						throw new ConfigException(
								"AppConfig returned unknown content type " + contentType);
					}
				} catch (CharacterCodingException e) {
					throw new ConfigException(
							"Illegal characters in configuration", e);
				} catch (SchemaException e) {
					throw new ConfigException(
							"Error parsing configuration", e);
				}
			}
		} catch (SdkException e) {
			// Start a new session on SDK error
			session= null;
			
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
	public void reportFailure(
			@NonNull String code,
			@NonNull String message,
			@Nullable Throwable cause)
	{
		if (alarmName != null) {
			try {
				CloudWatchClient cloudWatchClient= CloudWatchClient.create(); 
				
				SetAlarmStateRequest.Builder requestBuilder= SetAlarmStateRequest.builder();
				requestBuilder.alarmName(alarmName);
				requestBuilder.stateValue(StateValue.ALARM);
				
				SetAlarmStateRequest request= requestBuilder.build();
				
				cloudWatchClient.setAlarmState(request);
			} catch (SdkException e) {
				log.error("Error setting cloudwatch alarm");
			}
		}
	}
}
