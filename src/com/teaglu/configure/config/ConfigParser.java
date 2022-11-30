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
