package com.teaglu.configure.config.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.jdt.annotation.NonNull;

import com.teaglu.composite.Composite;
import com.teaglu.composite.exception.SchemaException;
import com.teaglu.composite.yaml.YamlComposite;
import com.teaglu.configure.config.ConfigParser;

public class YamlConfigParser implements ConfigParser {
	private YamlConfigParser() {}
	
	public static @NonNull ConfigParser Create() {
		return new YamlConfigParser();
	}
	
	@Override
	public @NonNull Composite parse(
			@NonNull InputStream input) throws SchemaException, IOException
	{
		try (InputStreamReader reader= new InputStreamReader(input)) {
    		return YamlComposite.Parse(reader);
		}
	}
}
