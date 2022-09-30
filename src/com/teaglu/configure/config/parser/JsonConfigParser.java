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
