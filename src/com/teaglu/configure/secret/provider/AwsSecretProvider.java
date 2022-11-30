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

package com.teaglu.configure.secret.provider;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.teaglu.configure.exception.ConfigException;
import com.teaglu.configure.secret.SecretProvider;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class AwsSecretProvider implements SecretProvider {
	private Map<@NonNull String, String> secretValues= new TreeMap<>();

	private AwsSecretProvider(
			@NonNull String region,
			@NonNull String secretName) throws ConfigException
	{
		SecretsManagerClientBuilder builder= SecretsManagerClient.builder();
		builder.region(Region.of(region));
		
		SecretsManagerClient client= builder.build();
	    
	    // In this sample we only handle the specific exceptions for the 'GetSecretValue' API.
	    // See https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html
	    // We rethrow the exception by default.
	    
	    GetSecretValueRequest getSecretValueRequest= GetSecretValueRequest
	    		.builder()
	    		.secretId(secretName).build();

	    try {
	    	GetSecretValueResponse result = client.getSecretValue(getSecretValueRequest);
	        String rawJson= result.secretString();
	        
	        try {
	        	JsonElement jsonElement= JsonParser.parseString(rawJson);
	        	if (!jsonElement.isJsonObject()) {
	        		throw new ConfigException("AWS Secret is not Object/Key-Value");
	        	}
	        	
	        	JsonObject jsonObject= jsonElement.getAsJsonObject();
	        	for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
	        		@SuppressWarnings("null") @NonNull String key= entry.getKey();
	        		
	        		JsonElement valueEl= entry.getValue();
	        		if (valueEl == null) {
	        			throw new ConfigException(
	        					"Value for configuration key " + key + " is null");
	        		}
	        		if (!valueEl.isJsonPrimitive()) {
	        			throw new ConfigException(
	        					"Value for configuration key " + key + " is not primitive");
	        		}
	        		JsonPrimitive valuePr= valueEl.getAsJsonPrimitive();
	        		if (!valuePr.isString()) {
	        			throw new ConfigException(
	        					"Value for configuration key " + key + " is not string");
	        		}
	        		String value= valuePr.getAsString();
        			secretValues.put(key, value);
	        	}
	        } catch (JsonSyntaxException e) {
	        	throw new ConfigException(
	        			"Unable to parse AWS secret as JSON", e);
	        }
	    } catch (SdkException e) {
	        // Secrets Manager can't decrypt the protected secret text using the provided KMS key.
	        // Deal with the exception here, and/or rethrow at your discretion.
	        throw new ConfigException("Error loading secret key " + secretName, e);
	    }
	}
	
	public static @NonNull SecretProvider Create(
			@NonNull String region,
			@NonNull String secretName) throws ConfigException
	{
		return new AwsSecretProvider(region, secretName);
	}
	
	@Override
	public @NonNull String getSecret(@NonNull String name) throws ConfigException {
		String rval= secretValues.get(name);
		if (rval == null) {
			throw new ConfigException(
					"No definition for secret " + name);
		}
		
		return rval;
	}

}
