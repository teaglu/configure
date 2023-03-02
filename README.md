# Configure

This project is a generic library to deal with configuration and secrets.  Configurations are
in the form of a [Composite](https://github.com/teaglu/composite), and secrets are a map from
string to string.  A replacer is included to make it easy to use replacement sequences in the
configuration to refer to secrets.

Keeping credentials / secrets in a separate location and referencing them from the configuration
allows configurations to be centrally managed, while keeping the credentials onsite and in as
few places as possible.

## Configuration Managers

A configuration manager retrieves configurations and sends them to a target to be applied.  The
manager continues to run as a background thread during the operation of the program, and monitors
the configuration for changes.  When a change is detected, the configuration will be sent to
the target again.

Your target may decide to merge the new configuration with the running one for a more seamless
in-place update, or it may delete all running items and recreate them.  If you configuration
target throws an exception, the manager will consider this as a rejection of the configuration
and will report it back to the source if the source supports that function.

Configuration URIs may be followed by optional local parameters after a hash sign.  For example:

    aws://appconfig/helloworld/config/prod#pollTime=60&alarm=prodfail

## Configuration Manager Factory

The configuration manager factory takes a URI string, which can either be passed in or read from
an environment variable.  The default environment variable is named CONFIGURATION.  The following
formats are available.

### docker://{secret}

This creates a configuration based on a docker secret which can be in either JSON or YAML format.
If the format is not specified by using the `format` local parameter, then the library will
try to guess based on file extension.  If there is no file extension or the extension is not
`json`, `yaml`, or `yml` then JSON will be assumed.  The configuration will be read once
and not monitored because secrets are immutable.

### aws://appconfig/{application}/{configuration}/{environment}

This creates a configuration based on AWS AppConfig.  The format may be either json or yaml, and
is determined based on the content type returned from AppConfig.  The {application},
{configuration}, and {environment} variables refer to the entities of the same name in AWS
AppConfig.

If the `pollTime` local parameter is set, it is used as an integer number of seconds specifying
how often to re-query the AppConfig service.  The default value is 300 (5 minutes).

If the `alarm` local parameter is set, then a cloudwatch alarm with the given name will be
triggered if the configuration fails to apply.  The name should correspond to a cloudwatch
alarm linked to the configuration so that rollback is triggered.  (Not tested)

### smbtrack://{host}/{token}

This creates a configuration based on SMBTrack managed configurations.  The {host} variable
is the hostname of your SMBTrack installation, and the {token} variable is the direct access
token created in the SMBTrack application.

If the `pollTime` local parameter is set, it is used as an integer number of seconds specifying
how often to re-query the configuration endpoint.  If the polltime is omitted 15 seconds is
used as the default.

### file://{path}

This creates a configuration based on reading a static file.  The path is the absolute or
relative path to the configuration file.  If the format is not specified by using the `format`
local parameter, then the library will try to guess based on file extension.  If there is no
file extension or the extension is not `json`, `yaml`, or `yml` then JSON will be assumed.
The file will be checked every five minutes for changes.

### debug://{path}

This creates a configuration based on reading a static file.  The format is the same as the
`file` schema - the only difference is that the file is checked every 15 seconds for changes.

### http:// and https://

Using a full URL starting with "http" or "https" will create a configuration based on reading
from a remote webserver.  The configuration will be polled every 300 seconds (5 minutes).

## Secrets Manager Factory

The configuration manager factory takes a URI string, which can either be passed in or read from
an environment variable.  The default environment variable is named SECRETS.  The following
URI formats are available:

### docker://{secret}

This creates a secret provider based on a docker secret.  The secret file is formatted as a java
properties file, consisting of keys followed by a colon and a value.

### aws://secretsmanager/{region}/{secret}

This creates a secret provider based on AWS SecretsManager.  The region and secret name correspond
to the AWS region code and the name of the secret.

### file://{path}

This creates a secret provider based on reading a static file.  The path is the absolute or
relative path to the configuration file.  The file is formatted as a java properties file,
consisting of keys followed by a colon and a value.

### debug://{path}

This creates a secret provider based on reading a static file.  The format and behavior are the
same as the `file` schema.

## Example Startup Code

This is an example of a Main class using the Configuration library.  It supports live
reconfiguration, but does so by stopping all existing jobs before creating new jobs.  In actual
use it may be worthwhile to look for changes and apply them instead.

    public class Main {
        private static final Logger log= LoggerFactory.getLogger(Main.class);
        private static final CountDownLatch quitLatch= new CountDownLatch(1);
    
        public static void main(String args[]) {
            try {
                // Create a secret provider based on environment
                SecretProvider secretProvider= SecretProviderFactory
                        .getInstance()
                        .createFromEnvironment();
    
                // Create a target for the configuration manager to operate on
                ConfigTarget target= new ConfigTarget() {
                    @Override
                    public void apply(@NonNull Composite config) throws Exception {
                        Main.loadConfig(config, secretProvider);
                    }
    
                    @Override
                    public void shutdown() {
                        quitLatch.countDown();  
                    }
                };
    
                // Create a configuration manager based on environment
                ConfigManager configManager= ConfigManagerFactory
                        .getInstance()
                        .createFromEnvironment(target, AtIdSecretReplacer.Create(secretProvider));
                
                // Start the configuration manager
                configManager.start();
    
                // The main thread just waits on a latch until it's time to shut everything down.
                for (boolean run= true; run;) {
                    try {
                        quitLatch.await();
                        run= false;
                    } catch (InterruptedException e) {
                    }
                }
    
                // Shut down the manager normally
                configManager.stop();
                
                // Stop any running jobs
                stopJobs();
            } catch (Exception e) {
                log.error("Error in main startup", e);
            }
        }
    
        private static void loadConfig(
                @NonNull Composite config,
                @NonNull SecretProvider secretProvider) throws Exception
        {
            stopJobs();
            startJobs(config, secretProvider);
        }
        
        private static synchronized void stopJobs()
        {
            // FIXME
        }
        
        private static synchronized void startJobs(
                @NonNull Composite config,
                @NonNull SecretProvider secretProvider) throws Exception
        {
            // FIXME
        }
    }

    