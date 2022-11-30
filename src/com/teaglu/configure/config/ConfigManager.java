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

/**
 * ConfigManager
 * 
 * A config manager monitors a configuration source and sends updates to a ConfigTarget on
 * startup and if anything changes.  You don't have to use a manager - you can just call the
 * methods of the ConfigSource yourself.
 * 
 */
public interface ConfigManager {
	/**
	 * start
	 * 
	 * Start any threads for monitoring, and send the initial configuration as soon as it is
	 * available.  The initial configuration may be sent from this thread as well, so be sure
	 * the target is ready to receive configuration as soon as you call start().
	 */
	public void start();
	
	/**
	 * stop
	 * 
	 * Stop any background threads and release any resources.  It's guaranteed that no calls
	 * to targets will be made after this returns.
	 */
	public void stop();
}
