# Configure

This project is a generic library to deal with configuration and secrets.  Configurations are
in the form of a Composite, and secrets are a map from string to string.  A replacer is included
to make it easy to use replacement sequences in the configuration to refer to secrets.

## Configuration Managers

A configuration manager retrieves configurations and sends them to a target to be applied.  The
manager continues to run as a background thread during the operation of the program, and monitors
the configuration for changes.  When a change is detected, the configuration will be sent to
the target again.

Your target may decide to merge the new configuration with the running one for a more seamless
in-place update, or it may delete all running items and recreate them.  If you configuration
target throws an exception, the manager will consider this as a rejection of the configuration
and will report it back to the source if the source supports that function.

## Configuration Manager Factory

The configuration manager factory takes a URI string, which can either be passed in or read from
an environment variable.  The default environment variable is named CONFIGURATION.  The following
formats are available:

### docker://{secret}

This creates a configuration based on a docker secret.  The format may be either json or yaml,
and if omitted json will be assumed.  The configuration will be read once and not monitored
because secrets are immutable.

### aws://appconfig/{application}/{configuration}/{environment}

This creates a configuration based on AWS AppConfig.  The format may be either json or yaml, and
is determined based on the content type returned from AppConfig.  The {application},
{configuration}, and {environment} variables refer to the entities of the same name in AWS
AppConfig.

If the alarm name is set and non-blank, then a cloudwatch alarm with the given name will be
triggered if the configuration fails to apply.  The name should correspond to a cloudwatch
alarm linked to the configuration so that rollback is triggered.

### smbtrack://{host}/{token}

This creates a configuration based on SMBTrack managed configurations.  The {host} variable
is the hostname of your SMBTrack installation.  If the polltime is omitted 300 seconds is
used as the default.

### debug://{path}

This creates a configuration based on reading a static file.  The path is the absolute or
relative path to the configuration file.  The format may be either "json" or "yaml" and defaults
based on the file extension.  The file will be checked every 15 seconds for changes.

### http:// and https://

Using a full URL starting with "http" or "https" will create a configuration based on reading
from a remote webserver.  The configuration will be polled every 300 seconds.

## Secrets Manager Factory

The configuration manager factory takes a URI string, which can either be passed in or read from
an environment variable.  The default environment variable is named SECRETS.  The following
URI formats are available:

### docker://{secret}

This creates a configuration based on a docker secret.  The format may be either json or yaml,
and if omitted json will be assumed.  The configuration will be read once and not monitored
because secrets are immutable.

### aws://appconfig/{application}/{configuration}/{environment}

This creates a configuration based on AWS AppConfig.  The format may be either json or yaml, and
is determined based on the content type returned from AppConfig.  The {application},
{configuration}, and {environment} variables refer to the entities of the same name in AWS
AppConfig.  If the pollTime is omitted 300 seconds is used as the default.

If the alarm name is set and non-blank, then a cloudwatch alarm with the given name will be
triggered if the configuration fails to apply.  The name should correspond to a cloudwatch
alarm linked to the configuration so that rollback is triggered.

### smbtrack://{host}/{token}

This creates a configuration based on SMBTrack managed configurations.  The {host} variable
is the hostname of your SMBTrack installation.  If the polltime is omitted 300 seconds is
used as the default.

### debug://{path}

This creates a configuration based on reading a static file.  The path is the absolute or
relative path to the configuration file.  The format may be either "json" or "yaml" and defaults
based on the file extension.  The file will be checked every 15 seconds for changes.

### http:// and https://

Using a full URL starting with "http" or "https" will create a configuration based on reading
from a remote webserver.  The configuration will be polled every 300 seconds.
