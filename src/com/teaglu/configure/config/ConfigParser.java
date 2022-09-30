package com.teaglu.configure.config;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNull;

import com.teaglu.composite.Composite;
import com.teaglu.composite.exception.SchemaException;

/**
 * ConfigParser
 * 
 * A configuration parser reads from an input stream and converts that stream to a configuration
 * in the form of a Composite.
 * 
 */
public interface ConfigParser {
	/**
	 * parse
	 * 
	 * Parse the input data stream until EOF and convert to a configuration in the form of
	 * a Composite.
	 * 
	 * @param input						Input data stream
	 * 
	 * @return							Configuration
	 * 
	 * @throws SchemaException			Something wrong with the file format
	 * @throws IOException				Unable to read the data stream
	 */
	public @NonNull Composite parse(
			@NonNull InputStream input) throws SchemaException, IOException;
}
