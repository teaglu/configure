package com.teaglu.configure.secret;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.teaglu.configure.exception.ConfigException;
import com.teaglu.configure.secret.provider.AwsSecretProvider;
import com.teaglu.configure.secret.provider.DebugSecretProvider;
import com.teaglu.configure.secret.provider.DockerSecretProvider;
import com.teaglu.configure.secret.provider.NullSecretProvider;
import com.teaglu.configure.uri.Uri;
import com.teaglu.configure.uri.UriImpl;


/**
 * SecretProviderFactory
 * 
 * Factory for building secret providers, since there are several implementations
 * 
 */
public class SecretProviderFactory {
	private SecretProviderFactory() {}

	// This doesn't have a huge build cost, so just build the singleton statically
	private static @NonNull SecretProviderFactory instance= new SecretProviderFactory();	
	public static @NonNull SecretProviderFactory getInstance() { return instance; }
	
	/**
	 * createFromString
	 * 
	 * Create a new secret provider.  The input is a string that specifies the secret
	 * configuration.  For container builds this is normally taken from an environment variable,
	 * but that's not forced.
	 * 
	 * The configuration is expected to start with a type string, with a colon delimiting any
	 * other information needed to create the provider.
	 * 
	 * @param config					Configuration string
	 * @return							New secret provider
	 * 
	 * @throws ConfigException			Something went wrong
	 */
	public @NonNull SecretProvider createFromString(
			@Nullable String configUri) throws ConfigException
	{
		if (configUri == null) {
			return NullSecretProvider.Create();
		} else {
			Uri uri= UriImpl.CreateFromString(configUri);
			
			switch (uri.getSchema()) {
			case "docker":
				if (uri.getPathSectionCount() != 1) {
					throw new ConfigException("Docker secret URI requires a secret name");
				}
				String path= uri.getPathSection(0);
				
				return DockerSecretProvider.Create(path);
				
			case "debug":
				if (uri.getPathSectionCount() < 1) {
					throw new ConfigException("Debug secret URI requires a path");
				}
				
				return DebugSecretProvider.Create(uri.getPathAsLocal());
				
			case "aws":
				if (uri.getPathSectionCount() != 3) {
					throw new ConfigException(
							"AWS secret URI requires service, region, and secret name");
				}
				
				String service= uri.getPathSection(0);
				if (!service.equals("secretsmanager")) {
					throw new ConfigException("AWS service " + service + " is not known.");
				}
				
				String region= uri.getPathSection(1);
				String secretName= uri.getPathSection(2);
				
				return AwsSecretProvider.Create(region, secretName);
	
			default:
				throw new ConfigException(
						"Secret schema " + uri.getSchema() + " is not implemented.");
			}
		}
	}
	
	public @NonNull SecretProvider createFromEnvironment() throws ConfigException {
		String configString= System.getenv("SECRETS");
		if (configString == null) {
			// If there's no secrets variable, assume the caller put anything they need directly
			// in the configuration and use a null provider.
			return NullSecretProvider.Create();
		} else {
			return createFromString(configString);
		}
	}
}
