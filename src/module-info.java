module com.teaglu.configure {
	requires org.eclipse.jdt.annotation;

	requires software.amazon.awssdk.awscore;
	requires software.amazon.awssdk.core;
	requires software.amazon.awssdk.auth;
	requires software.amazon.awssdk.regions;
	
	requires software.amazon.awssdk.services.appconfig;
	requires software.amazon.awssdk.services.appconfigdata;
	requires software.amazon.awssdk.services.secretsmanager;
	requires software.amazon.awssdk.utils;
	
	requires org.slf4j;
	
	requires transitive com.teaglu.composite;
	requires software.amazon.awssdk.services.cloudwatch;
	
	exports com.teaglu.configure.exception;
	
	exports com.teaglu.configure.config;
	exports com.teaglu.configure.config.manager;
	exports com.teaglu.configure.config.parser;
	exports com.teaglu.configure.config.source;

	exports com.teaglu.configure.secret;
	exports com.teaglu.configure.secret.provider;
	exports com.teaglu.configure.secret.replacer;
	
	exports com.teaglu.configure.uri;
}