package com.teaglu.configure.uri;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.teaglu.configure.exception.UriParseException;

/**
 * UriImpl
 * 
 * Uri standard implementation
 *
 */
public class UriImpl implements Uri {
	private @NonNull String schema;
	private String nonLocalPart;
	
	private @NonNull String @NonNull[] pathSections;
	
	private Map<String, String> localArguments= new TreeMap<>();
	private Map<String, List<String>> queryArguments= new TreeMap<>();
	
	@Override
	public @NonNull String getSchema() {
		return schema;
	}
	
	@Override
	public int getPathSectionCount() {
		return pathSections.length;
	}
	
	@Override
	public @NonNull String getPathSection(
			int sectionNo)
	{
		return pathSections[sectionNo];
	}
	
	@Override
	public @Nullable String getLocalArgument(
			@NonNull String name)
	{
		return localArguments.get(name);
	}
	
	@Override
	public @NonNull String getLocalArgument(
			@NonNull String name,
			@NonNull String defaultValue)
	{
		String value= localArguments.get(name);
		if (value == null) {
			value= defaultValue;
		}
		
		return value;
	}
	
	public UriImpl(
			@NonNull String uriString) throws UriParseException
	{
		// First find the last # to break off any local parameters
		int hashOffset= uriString.lastIndexOf('#');
		if (hashOffset >= 0) {
			String localPart= uriString.substring(hashOffset + 1);
			nonLocalPart= uriString.substring(0, hashOffset);
			
			if (!localPart.isBlank()) {
				String[] entries= localPart.split("&");
				for (int entryNo= 0; entryNo < entries.length; entryNo++) {
					String entry= entries[entryNo];
					int equalsOffset= entry.indexOf('=');
					if (equalsOffset > 0) {
						try {
							String name= entry.substring(0, equalsOffset);
							String value= URLDecoder.decode(
									entry.substring(equalsOffset + 1),
									StandardCharsets.UTF_8.name());
							
							localArguments.put(name, value);
						} catch (UnsupportedEncodingException e) {
							throw new UriParseException(
									"Unable to parse local value string", e);
						}
					}
				}
			}
		} else {
			nonLocalPart= uriString;
		}

		int schemaEnd= uriString.indexOf(":");
		if (schemaEnd <= 0) {
			throw new UriParseException("Unable to determine schema from URI");
		}

		@SuppressWarnings("null")
		@NonNull String tmpSchema= uriString.substring(0, schemaEnd);
		
		schema= tmpSchema;
		
		String resourcePart= nonLocalPart.substring(schemaEnd + 1);
		
		// Trim off the starting // or / - this is a little sloppy but works
		if (resourcePart.startsWith("//")) {
			resourcePart= resourcePart.substring(2);
		} else if (resourcePart.startsWith("/")) {
			resourcePart= resourcePart.substring(1);
		}
		
		int queryOffset= resourcePart.indexOf('?');
		if (queryOffset >= 0) {
			String queryPart= resourcePart.substring(queryOffset + 1);
			resourcePart= resourcePart.substring(0, queryOffset);
			
			if (!queryPart.isBlank()) {
				String[] entries= queryPart.split("&");
				for (int entryNo= 0; entryNo < entries.length; entryNo++) {
					String entry= entries[entryNo];
					int equalsOffset= entry.indexOf('=');
					if (equalsOffset > 0) {
						try {
							String name= entry.substring(0, equalsOffset);
							String value= URLDecoder.decode(
									entry.substring(equalsOffset + 1),
									StandardCharsets.UTF_8.name());
							
							if (queryArguments.containsKey(name)) {
								queryArguments.get(name).add(value);
							} else {
								List<String> valueList= new ArrayList<>();
								valueList.add(value);
								queryArguments.put(name, valueList);
							}
							localArguments.put(name, value);
						} catch (UnsupportedEncodingException e) {
							throw new UriParseException("Unable to parse query value string", e);
						}
					}
				}
			}
		}
		
		if (resourcePart == null) {
			throw new RuntimeException("Can't happen");
		}
		
		@SuppressWarnings("null")
		@NonNull String @NonNull[] tmpParts= resourcePart.split("/");
		
		pathSections= tmpParts;
	}
	
	public static @NonNull Uri CreateFromString(
			@NonNull String configString) throws UriParseException
	{
		return new UriImpl(configString);
	}
	
	public @NonNull String getPathAsLocal() {
		StringBuilder pathBuild= new StringBuilder();

		int pathPartCount= pathSections.length;
		if (File.separatorChar == '\\') {
			// Windows is the only thing I know of that uses the backslash
			
			String firstPart= pathSections[0];
			if (firstPart.length() == 1) {
				// This catches the "mounted drive" style path, so something like
				// "file://C/Program Files/Foo" would convert to "C:\Program Files\Foo".
				// We might be tripped up if somebody made a machine with a one-character name
				// and tried to use it like a UNC path, but that's so weird I'll ignore it for now.
				pathBuild.append(firstPart);
				pathBuild.append(":");
			} else if ((firstPart.length() == 2) && (firstPart.charAt(1) == ':')) { 
				// This is something like "file://C:/Program Files" which I've never seen in
				// practice but it's plausible I guess.
				pathBuild.append(firstPart);
			} else {
				// UNC Style - add another backslash so we get the UNC prefix
				pathBuild.append("\\");
			}
			
			for (int pathPart= 1; pathPart < pathPartCount; pathPart++) {
				pathBuild.append("\\");
				pathBuild.append(pathSections[pathPart]);
			}
		} else {
			for (int pathPart= 0; pathPart < pathPartCount; pathPart++) {
				pathBuild.append(File.separatorChar);
				pathBuild.append(pathSections[pathPart]);
			}
		}
		
		@SuppressWarnings("null")
		@NonNull String path= pathBuild.toString();
		
		return path;
	}
}
