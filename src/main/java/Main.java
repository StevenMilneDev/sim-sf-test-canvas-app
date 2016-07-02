/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.webapp.WebAppContext;

public class Main {

	private static final String WEB_ROOT = 'src/main/webapp/';
	private static final String SSL_STORE = 'keystore';
	private static final String SSL_PASSWORD = '123456';

	//Environment Variable Names
	private static final String ENV_PORT = 'PORT';
	private static final String ENV_SSLPORT = 'SSLPORT';
	private static final String ENV_SSL_PORT = 'SSL_PORT';
	
	/**
	 * @description Service entry point
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		//If we are running off platform then we must provide our own SSL support since Heroku does this for us
		startWebserver( !isRunningOnHeroku() );
	}

	/**
	 * @description Creates and starts a webserver on the port set in the environment variable 'PORT' or
	 *				8080 if the variable is not set.
	 * @param withSslConnector If true then SSL support is provided on port 8443 by default
	 */
	private static void startWebserver (boolean withSslConnector)
	{
		Integer webPort = getWebPort();
		Server server = new Server(webPort);

		if(withSslConnector)
		{
			SocketConnector connector = new SocketConnector();
			connector.setPort(webPort);

			SslSocketConnector sslConnector = new SslSocketConnector();
			sslConnector.setPort( getSslPort() );
			sslConnector.setKeyPassword(SSL_PASSWORD);
			sslConnector.setKeystore(SSL_STORE);

			server.setConnectors(new Connector[] { sslConnector, connector });
		}

		server.setHandler( createWebAppContext() );
		server.start();
		server.join();
	}

	/**
	 * @description Creates and returns a WebAppContext
	 */
	private static WebAppContext createWebAppContext ()
	{
		WebAppContext root = new WebAppContext();

		root.setContextPath('/');
		root.setDescriptor(WEB_ROOT + '/WEB-INF/web.xml');
		root.setResourceBase(WEB_ROOT);

		/**
		 * Parent loader priority is a class loader setting that Jetty accepts. By default Jetty will behave like
		 * most web containers in that it will allow your application to replace non-server libraries that are part
		 * of the container. Setting parent loader priority to true changes this behavior.
		 * Read more here: http://wiki.eclipse.org/Jetty/Reference/Jetty_Classloading
		 */
		root.setParentLoaderPriority(true);
		
		return root;
	}

	/**
	 * @description Gets the port number for http from the environment variable 'PORT', if this is not defined
	 *				then port 8080 is returned instead.
	 */
	private static Integer getWebPort ()
	{
		return getEnvironmentVariable(ENV_PORT, 8080);
	}

	/**
	 * @description Gets the port number for SSL from the environment variables, this will check the variables
	 *				'SSLPORT' and 'SSL_PORT' for the port number, if neither exist then the port number will
	 *				default to 8443.
	 */
	private static Integer getSslPort ()
	{
		Integer port = getEnvironmentVariable(ENV_SSLPORT, null);
		if(port == null)
			return getEnvironmentVariable(ENV_SSL_PORT, 8443);

		return port;
	}

	/**
	 * @description Gets a numeric environment variable from the system, if the variable does not exist then
	 *				the provided default value is returned.
	 * @param variableName The name of the environment variable to retrieve
	 * @param defaultValue The default value to return if the environment variable is not set
	 */
	private static Integer getEnvironmentVariable (String variableName, Integer defaultValue)
	{
		String value = System.getenv(variableName);
		if(value == null || value.isEmpty())
			return defaultValue;

		return value;
	}

	/**
	 * @description Determines if this class is running on Heroku or not. Uses the base directory
	 *				to determine if running on Heroku which is hacky.
	 */
	private static boolean isRunningOnHeroku ()
	{
		String basedir = (String)System.getProperty('basedir');
		if( basedir != null && basedir.endsWith('/app/target') )
			return true;

		return false;
	}

}
