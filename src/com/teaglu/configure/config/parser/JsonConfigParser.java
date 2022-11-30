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

package com.teaglu.configure.config.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.jdt.annotation.NonNull;

import com.teaglu.composite.Composite;
import com.teaglu.composite.exception.SchemaException;
import com.teaglu.composite.json.JsonComposite;
import com.teaglu.configure.config.ConfigParser;

public class JsonConfigParser implements ConfigParser {
	private JsonConfigParser() {}
	
	public static @NonNull ConfigParser Create() {
		return new JsonConfigParser();
	}
	
	@Override
	public @NonNull Composite parse(
			@NonNull InputStream input) throws SchemaException, IOException
	{
		try (InputStreamReader reader= new InputStreamReader(input)) {
    		return JsonComposite.Parse(reader);
		}
	}
}
