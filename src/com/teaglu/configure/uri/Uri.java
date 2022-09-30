package com.teaglu.configure.uri;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Uri
 * 
 * A resource URI as used by the config module.
 *
 */
public interface Uri {
	/**
	 * getSchema
	 * 
	 * Return the schema section of the URI
	 * 
	 * @return							Schema
	 */
	public @NonNull String getSchema();

	/**
	 * getPathPartCount
	 * 
	 * Return the number of path sections.
	 * 
	 * @return							Number of sections
	 */
	public int getPathSectionCount();
	
	/**
	 * getPathSection
	 * 
	 * Get a path section by 0-based index
	 * 
	 * @param part
	 * 
	 * @return							The requested path section
	 */
	public @NonNull String getPathSection(int part) throws IndexOutOfBoundsException;

	/**
	 * getLocalArgument
	 * 
	 * Retrieve a named argument from the local section
	 * 
	 * @param name						Name
	 * 
	 * @return							Argument value or null if unknown
	 */
	public @Nullable String getLocalArgument(@NonNull String name);
	
	/**
	 * getPathAsLocal
	 * 
	 * Return the path parts converted to a local filename
	 * 
	 * @return							Local filename
	 */
	public @NonNull String getPathAsLocal();

	/**
	 * getLocalArgument
	 * 
	 * Retrieve a named argument from the local section, or the given default value if the
	 * named argument is not present.
	 * 
	 * @param name						Argument name
	 * @param defaultValue				Default value to use
	 * 
	 * @return							The argument value
	 */
	@NonNull String getLocalArgument(@NonNull String name, @NonNull String defaultValue);
}