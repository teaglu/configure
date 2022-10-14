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
import com.google.gson.JsonSyntaxException;
import com.teaglu.composite.Composite;
import com.teaglu.composite.exception.SchemaException;
import com.teaglu.composite.json.JsonComposite;
import com.teaglu.configure.config.ConfigSource;
import com.teaglu.configure.exception.ApiResponseFormatException;
import com.teaglu.configure.exception.ApiStatusException;
import com.teaglu.configure.exception.ConfigException;

public class UrlConfigSource implements ConfigSource {
	private static final Logger log= LoggerFactory.getLogger(UrlConfigSource.class);
	
	private static final String USER_AGENT= "Teaglu-Configure-Lib";
	
	private final @NonNull URL configUrl;
	
	private File cacheFile= null;

	// If we can't get a configuration from the upstream URL, and we don't have a cache file or
	// the cache file is missing/invalid, this is how long we wait to retry
	private static final long STARTUP_RECHECK_TIME= 15_000;

	private Composite configuration;
	private String configurationHash;
	
	private boolean needsLoad= true;
	
	private UrlConfigSource(
			@NonNull String configUrl) throws ConfigException
	{
		String cacheDirectory= System.getenv("CONFIGURATION_CACHE");
		if (cacheDirectory != null) {
			if (!cacheDirectory.endsWith(File.separator)) {
				cacheDirectory+= File.separator;
			}
			
			try {
				MessageDigest digest= MessageDigest.getInstance("SHA-1");
				digest.update(configUrl.getBytes(StandardCharsets.UTF_8));
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
								
								char buffer[]= new char[2048];
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
				log.error("Unable to find SHA-1 hash to calculate cache file names", e);
			}
		} else {
			log.warn("Configuration cache not configured - this is not recommended.");
		}

		try {
			this.configUrl= new URL(configUrl);
		} catch (MalformedURLException e) {
			throw new ConfigException("Malformed configuration URL", e);
		}
	}
	
	private @NonNull Composite parseConfiguration(
			@NonNull String contentType,
			@NonNull String content) throws SchemaException, ApiResponseFormatException {
		switch (contentType) {
		case "application/json":
			return JsonComposite.Parse(content);
			
		default:
			throw new ApiResponseFormatException(
					"Content type " + contentType + " is not mapped to a known parser.");
		}
	}
	
	public static @NonNull ConfigSource Create(
			@NonNull String configUrl) throws ConfigException
	{
		return new UrlConfigSource(configUrl);
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
	
	private @NonNull Composite fetchConfiguration(
			) throws IOException, SchemaException, ApiResponseFormatException, ApiStatusException
	{
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
						char[] buffer= new char[4096];
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
				// Implementation doesn't really specify whether you get ErrorStream or
				// InputStream - the correct answer seems to be to check ErrorStream first.
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
		} catch (ApiResponseFormatException|SchemaException e) {
			log.error("Invalid response from configuration endpoint", e);
			reportFailure("invalid-json", "Configuration is not well-formed JSON", e);
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
	}

	@Override
	public void reportFailure(
			@NonNull String code,
			@NonNull String message,
			@Nullable Throwable cause)
	{
	}
}
