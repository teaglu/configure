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