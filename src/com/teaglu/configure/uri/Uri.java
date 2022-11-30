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