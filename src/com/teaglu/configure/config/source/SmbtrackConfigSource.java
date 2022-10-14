package com.teaglu.configure.config.source;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.teaglu.composite.Composite;
import com.teaglu.composite.exception.SchemaException;
import com.teaglu.composite.exception.WrongTypeException;
import com.teaglu.composite.json.JsonComposite;
import com.teaglu.configure.config.ConfigSource;
import com.teaglu.configure.exception.ApiResponseFormatException;
import com.teaglu.configure.exception.ApiStatusException;
import com.teaglu.configure.exception.ConfigException;

public class SmbtrackConfigSource implements ConfigSource {
	private static final Logger log= LoggerFactory.getLogger(SmbtrackConfigSource.class);
	
	private static final String USER_AGENT= "Teaglu-Configure-Lib";
	private static final int READ_BUFFER_SIZE= 2048;
	
	private final @NonNull URL configUrl;
	
	private final @NonNull URL confirmUrl;
	private final @NonNull URL rejectUrl;
	
	private File cacheFile;

	// If we can't get a configuration from the upstream URL, and we don't have a cache file or
	// the cache file is missing/invalid, this is how long we wait to retry
	private static final long STARTUP_RECHECK_TIME= 15_000;

	private Composite configuration;
	private String configurationHash;
	
	private boolean needsLoad= true;
	private boolean needsReport= false;
	
	private SmbtrackConfigSource(
			@NonNull String host,
			@NonNull String token) throws ConfigException
	{
		String cacheDirectory= System.getenv("CONFIGURATION_CACHE");
		if (cacheDirectory != null) {
			if (!cacheDirectory.endsWith(File.separator)) {
				cacheDirectory+= File.separator;
			}
			
			try {
				// The token will be unique, but we still hash it just so the authentication
				// token won't be sitting around somewhere.  All we're really trying to do is
				// make sure that two different invocations don't step on each other.
				MessageDigest digest= MessageDigest.getInstance("SHA-1");
				digest.update(token.getBytes(StandardCharsets.UTF_8));
				Encoder encoder= Base64.getEncoder();
				String configHash= encoder.encodeToString(digest.digest());
				
				cacheFile= new File(cacheDirectory + configHash + ".dat");
				
				if (cacheFile.exists()) {
					String contentType;
					String content;
					
					try (InputStream cacheStream= new FileInputStream(cacheFile)) {
						try (Reader cacheReader= new InputStreamReader(cacheStream)) {
							try (BufferedReader bufferedReader= new BufferedReader(cacheReader)) {
								contentType= bufferedReader.readLine();
								
								StringBuilder contentBuilder= new StringBuilder();
								
								char buffer[]= new char[READ_BUFFER_SIZE];
								int bufferCnt= 0;
								while ((bufferCnt= bufferedReader.read(buffer)) != -1) {
									contentBuilder.append(buffer, 0, bufferCnt);
								}
								
								content= contentBuilder.toString();
							}
						}
						
						if ((contentType != null) && (content != null)) {
							try {
								configuration= parseConfiguration(contentType, content);
							} catch (SchemaException|ApiResponseFormatException e) {
								log.error("Error parsing configuration cache", e);
							}
						}
					} catch (IOException|JsonSyntaxException e) {
						log.error("Error loading configuration cache", e);
					}
				} else {
					log.warn("Configuration cache not present.  " +
								"If this is not an initial installation this could be a problem.");
				}
			} catch (NoSuchAlgorithmException e) {
				log.error(
						"Unable to find SHA-1 hash to calculate cache file names", e);
			}
		} else {
			log.warn(
					"Configuration cache not configured - this is not recommended.");
			cacheFile= null;
		}

		try {
			String configUrl= "https://" + host + "/api/v1/cfg/direct/" + token;
			
			this.configUrl= new URL(configUrl);
			this.confirmUrl= new URL(configUrl + "/confirm");
			this.rejectUrl= new URL(configUrl + "/reject");
		} catch (MalformedURLException e) {
			throw new ConfigException("Malformed configuration URL", e);
		}
	}
	
	public static @NonNull ConfigSource Create(
			@NonNull String host,
			@NonNull String token) throws ConfigException
	{
		return new SmbtrackConfigSource(host, token);
	}
	
	private String getJsonHash(@NonNull Composite el) {
		try {
			MessageDigest md= MessageDigest.getInstance("SHA-1");
			md.update(el.toString().getBytes(StandardCharsets.UTF_8));
			Encoder encoder= Base64.getEncoder();
			return encoder.encodeToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			log.error("Unable to hash algorithm SHA-1", e);
			
			return Long.toString(System.currentTimeMillis());
		}
	}
	
	private @NonNull Composite parseConfiguration(
			@NonNull String contentTypeHeader,
			@NonNull String content) throws SchemaException, ApiResponseFormatException
	{
		String contentType= contentTypeHeader;
		int charsetOffset= contentType.indexOf(';');
		if (charsetOffset > 0) {
			contentType= contentType.substring(0, charsetOffset);
		}
		
		switch (contentType) {
		case "application/json":
			return JsonComposite.Parse(content);
			
		default:
			throw new ApiResponseFormatException(
					"Content type " + contentType + " is not mapped to a known parser.");
		}
	}
	
	private @NonNull Composite fetchConfiguration(
			) throws IOException, SchemaException, ApiResponseFormatException, ApiStatusException
	{
		// If we were doing any real volume the Apache HTTP library is more efficient because it
		// keeps a consistent connection pool, but since we're only pulling something every 5
		// minutes or so the connections would go stale anyway.  Using the native stuff is one
		// less dependency to drag in.  -DAW 221007
		HttpURLConnection connection= (HttpURLConnection)configUrl.openConnection();

		try {
			connection.setRequestProperty("User-Agent", USER_AGENT);
			connection.setRequestProperty("Cache-Control", "no-cache, no-store");
			connection.setRequestMethod("GET");
	
			connection.setDoInput(true);
			connection.setDoOutput(false);

			int responseCode= connection.getResponseCode();
			if (responseCode == 200) {
				String contentType= connection.getContentType();
				if (contentType == null) {
					log.warn("Content type not received from configuration endpoint");
					contentType= "application/json";
				}
				
				InputStream input= connection.getInputStream();
				if (input == null) {
					throw new RuntimeException("Input stream is null on HttpURLConnection");
				}
				
				StringBuilder response= new StringBuilder();
				
				OutputStreamWriter cacheWriter= null;
				try {
					if (cacheFile != null) {
						cacheWriter= new FileWriter(cacheFile);						
						cacheWriter.write(contentType + "\n");
					}
					
					try (InputStreamReader reader= new InputStreamReader(input)) {
						char[] buffer= new char[READ_BUFFER_SIZE];
						int bufferCnt= 0;
						while ((bufferCnt= reader.read(buffer)) != -1) {
							response.append(buffer, 0, bufferCnt);
							
							// Copy the data to the cache file at the same time
							if (cacheWriter != null) {
								cacheWriter.write(buffer, 0, bufferCnt);
							}
						}
					}
				} finally {
					if (cacheWriter != null) {
						cacheWriter.close();
					}
				}
				
				@SuppressWarnings("null")
				@NonNull String content= response.toString();

				return parseConfiguration(contentType, content);
			} else {
				// User error stream first, or input if error is null
				InputStream inputStream= connection.getErrorStream();
				if (inputStream == null) {
					try {
						inputStream= connection.getInputStream();
					} catch (IOException e) {
					}
				}
				
				StringBuilder responseText= new StringBuilder();
				if (inputStream != null) {
					try (InputStreamReader isr=
							new InputStreamReader(inputStream, StandardCharsets.UTF_8))
					{
						char[] responseBuffer= new char[2048];
						
						while (true) {
							int bytesRead= isr.read(responseBuffer);
							if (bytesRead == -1) {
								break;
							} else {
								responseText.append(responseBuffer, 0, bytesRead);
							}
						}

						log.debug("Error response: " + responseText.toString());
					}
				}
				throw new ApiStatusException(
						"Fetching configuration returned error code " + responseCode);
			}
		} finally {
			connection.disconnect();
		}
	}
	
	private @NonNull JsonObject uploadResponse(
			@NonNull URL respondUrl,
			@NonNull JsonObject data
			) throws IOException, SchemaException, ApiResponseFormatException, ApiStatusException
	{
		HttpURLConnection connection= (HttpURLConnection)respondUrl.openConnection();

		try {
			connection.setRequestProperty("User-Agent", USER_AGENT);
			connection.setRequestProperty("Cache-Control", "no-cache, no-store");
			connection.setRequestMethod("POST");
	
			connection.setDoInput(true);
			connection.setDoOutput(true);
			
			byte[] dataBytes= data.toString().getBytes(StandardCharsets.UTF_8);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-Length", Integer.toString(dataBytes.length));
			
			OutputStream output= connection.getOutputStream();
			output.write(dataBytes);

			int responseCode= connection.getResponseCode();
			if (responseCode == 200) {
				try (InputStreamReader isr= new InputStreamReader(
						connection.getInputStream(), StandardCharsets.UTF_8))
				{
					// For now the response isn't actually used - it's just required to be a
					// JSON object.
					JsonElement responseElement= JsonParser.parseReader(isr);
					
					if (!responseElement.isJsonObject()) {
						throw new WrongTypeException("Main Body", "Element");
					}
					
					@SuppressWarnings("null")
					@NonNull JsonObject rval= responseElement.getAsJsonObject();
					
					return rval;
				} catch (JsonSyntaxException e) {
					throw new ApiResponseFormatException("Unable to Parse JSON Response", e);
				}
			} else {
				InputStream inputStream= connection.getErrorStream();
				if (inputStream == null) {
					try {
						inputStream= connection.getInputStream();
					} catch (IOException e) {
					}
				}
				
				StringBuilder responseText= new StringBuilder();
				if (inputStream != null) {
					try (InputStreamReader isr= new InputStreamReader(
							inputStream, StandardCharsets.UTF_8))
					{
						char[] responseBuffer= new char[READ_BUFFER_SIZE];
						
						while (true) {
							int bytesRead= isr.read(responseBuffer);
							if (bytesRead == -1) {
								break;
							} else {
								responseText.append(responseBuffer, 0, bytesRead);
							}
						}

						log.debug("Error response: " + responseText.toString());
					}
				}
				throw new ApiStatusException("Status code " + responseCode);
			}
		} finally {
			connection.disconnect();
		}
	}
	
	private void check() {
		try {
			Composite newConfiguration= fetchConfiguration();
			String newConfigurationHash= getJsonHash(newConfiguration);
			
			boolean changed= false;
			if (configuration == null) {
				changed= true;
			} else {
				changed= !newConfigurationHash.equals(configurationHash);
			}
			
			if (changed) {
				configuration= newConfiguration;
				configurationHash= newConfigurationHash;
				needsLoad= true;
				needsReport= true;
				
				if (cacheFile != null) {
					try (OutputStream cacheStream= new FileOutputStream(cacheFile)) {
						try (OutputStreamWriter cacheWriter=
								new OutputStreamWriter(cacheStream, StandardCharsets.UTF_8))
						{
							// Use pretty-printing so the file is readable
							GsonBuilder gsonBuilder= new GsonBuilder();
							gsonBuilder.setPrettyPrinting();
							Gson gson= gsonBuilder.create();
							
							cacheWriter.write(gson.toJson(newConfiguration));
							cacheWriter.write("\n");
						}
					} catch (IOException|JsonSyntaxException e) {
						log.error("Error writing configuration cache", e);
					}
				}
			}
		} catch (IOException|ApiStatusException e) {
			log.error("IO/status exception reading configuration", e);
		} catch (SchemaException e) {
			log.error("Invalid response from configuration endpoint", e);
			reportFailure("invalid-json", "Configuration is not well-formed file", e);
		} catch (ApiResponseFormatException e) {
			log.error("Invalid response from configuration endpoint", e);
			reportFailure("invalid-json", "Configuration data is not a known MIME type", e);
		}
	}
	
	@Override
	public synchronized boolean needsReload() {
		check();
		return needsLoad;
	}

	@Override
	public synchronized @NonNull Composite reload() throws ConfigException {
		Composite rval= configuration;
		while (rval == null) {
			check();
			rval= configuration;
			
			if (rval == null) {
				log.warn("Waiting to retry initial configuration load");
				try {
					Thread.sleep(STARTUP_RECHECK_TIME);
				} catch (InterruptedException e) {
				}
			}
		}

		needsLoad= false;
		return rval;
	}

	@Override
	public void reportSuccess() {
		if (needsReport) {
			URL url= confirmUrl;
			if (url != null) {
				JsonObject data= new JsonObject();
				try {
					uploadResponse(url, data);
					needsReport= false;
				} catch (IOException |
						ApiStatusException |
						ApiResponseFormatException |
						SchemaException e)
				{
					log.error("Unable to report configuration confirmation", e);
				}
			}
		}
	}

	@Override
	public void reportFailure(
			@NonNull String code,
			@NonNull String message,
			@Nullable Throwable cause)
	{
		if (needsReport) {
			URL url= rejectUrl;
			if (url != null) {
				JsonObject data= new JsonObject();
				data.addProperty("code", code);
				data.addProperty("message", message);
	
				try {
					uploadResponse(url, data);
					needsReport= false;
				} catch (IOException |
						SchemaException |
						ApiResponseFormatException |
						ApiStatusException e)
				{
					log.error("Unable to report configuration rejections", e);
				}
			}
		}
	}
}
