package com.teaglu.configure.config.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.teaglu.composite.Composite;
import com.teaglu.composite.exception.SchemaException;
import com.teaglu.configure.config.ConfigParser;
import com.teaglu.configure.config.ConfigSource;
import com.teaglu.configure.exception.ConfigException;

public class FileConfigSource implements ConfigSource {
	private Logger log= LoggerFactory.getLogger(FileConfigSource.class);
	
	private File file;
	private ConfigParser parser;
	
	private FileTime modifiedTime;
	
	private FileConfigSource(
			@NonNull String path,
			@NonNull ConfigParser parser)
	{
		this.file= new File(path);
		this.parser= parser;
	}
	
	public @NonNull static ConfigSource Create(
			@NonNull String path,
			@NonNull ConfigParser parser)
	{
		return new FileConfigSource(path, parser);
	}
	
	@Override
	public @NonNull Composite reload() throws ConfigException {
    	try (InputStream in= new FileInputStream(file)) {
    		try {
    			return parser.parse(in);
    		} catch (SchemaException se) {
    			throw new ConfigException(
    					"Parser was not able to parse file data", se);
    		}
    	} catch (IOException e) {
    		throw new ConfigException(
    				"Error reading configuration file " + file.getAbsolutePath());
    	}
	}

	@Override
	public void reportSuccess() {
	}

	@Override
	public void reportFailure(@NonNull String code, @NonNull String message) {
	}

	@Override
	public boolean needsReload() {
    	boolean needsReload= true;
    	try {
			@SuppressWarnings("null")
			BasicFileAttributes attr= Files.readAttributes(
					file.toPath(), BasicFileAttributes.class);
			
    		if ((modifiedTime != null) && (modifiedTime.equals(attr.lastModifiedTime()))) {
    			needsReload= false;
    		} else {
    			modifiedTime= attr.lastModifiedTime();
    		}
    	} catch (IOException e) {
    		log.error("Error reading route file attributes", e);
    	}
    	
    	return needsReload;
	}
}
