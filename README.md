vertx-maven-plugin
==================

Maven Plugin for running verticles in their own vert.x instance.

Install
-----
This plugin is now available on Maven Central.

Group ID: org.anacoders.plugins

Artifact ID: vertx-maven-plugin

Current release version: 2.0.0.0-SNAPSHOT


Versions
--------

This plugin's versions are aligned with vert.x versions with the minor version number to indicate increments of the plugin.
e.g. vert.x 1.2.3.FINAL would be 1.2.3.x

Usage
-----

### vertx:start
This goal will start a verticle or vert.x module in it's own vert.x instance. By default this goal will be executed in daemon mode, meaning that it will not block the maven phase execution.
To start it, type:

	mvn vertx:start

### vertx:run

Executes the vertx:start goal in application mode (non-daemon). vert.x will continue to run until the plugin is explicitly stopped.  
To start it, type:

	mvn vertx:run

### vertx:stop

Executes the vertx:stop goal, used to cleanly stop vert.x.
To start it, type:

	mvn vertx:start vertx:stop

For example, if you want to run some tests on a vert.x app, then you can do something like:

	mvn vertx:start [call-test:goal] vertx:stop

This is normally set as a plugin in the pom.xml file for automatic execution, as shown below:

	<plugin>
		<groupId>org.anacoders.plugins</groupId>
		<artifactId>vertx-maven-plugin</artifactId>
		<version>2.0.0.0-SNAPSHOT</version>
		<configuration>
			<verticleName>org.vertx.maven.plugin.test.ServerExample</verticleName>
		</configuration>
		<executions>
			<execution>
				<id>start-vertx</id>
				<phase>pre-integration-test</phase>
				<goals>
					<goal>start</goal>
				</goals>
			</execution>
			<execution>
				<id>stop-vertx</id>
				<phase>post-integration-test</phase>
				<goals>
					<goal>stop</goal>
				</goals>
			</execution>
		</executions>
	</plugin>

For Java or Scala verticles, the plugin will need to be configured in your project's POM as follows:

	<plugin>
		<groupId>org.anacoders.plugins</groupId>
		<artifactId>vertx-maven-plugin</artifactId>
		<version>2.0.0.0-SNAPSHOT</version>
		<configuration>
			<verticleName>com.acme.MyVerticle</verticleName>
		</configuration>
	</plugin>  
	
For Groovy verticles, the plugin will need to be configured in your project's POM as follows:

	<plugin>
		<groupId>org.anacoders.plugins</groupId>
		<artifactId>vertx-maven-plugin</artifactId>
		<version>2.0.0.0-SNAPSHOT</version>
		<configuration>
			<verticleName>com/acme/MyVerticle.groovy</verticleName>
		</configuration>
	</plugin>  
	
For Javascript verticles, the plugin will need to be configured in your project's POM as follows:

	<plugin>
		<groupId>org.anacoders.plugins</groupId>
		<artifactId>vertx-maven-plugin</artifactId>
		<version>2.0.0.0-SNAPSHOT</version>
		<configuration>
			<verticleName>src/main/javascript/com/acme/MyVerticle.js</verticleName>
		</configuration>
	</plugin>  

For modules, the plugin will need to be configured in your project's POM as follows:

	<plugin>
		<groupId>org.anacoders.plugins</groupId>
		<artifactId>vertx-maven-plugin</artifactId>
		<version>2.0.0.0-SNAPSHOT</version>
		<configuration>
			<moduleName>some-module-name</moduleName>
			<moduleRepoUrl>http://some.module.repo.url</moduleRepoUrl>
			<vertxModulesDirectory>${basedir}/mods</vertxModulesDirectory>
		</configuration>
	</plugin>  

Note that:

* the moduleRepoUrl parameter is optional, the default value is: http://github.com/vert-x/vertx2-mods
* the vertxModulesDirectory parameter is optional, the default value is: ${project.build.directory}
	
If you need to use any of the out-of-the-box mods then you need a local vert.x install and set the vertxHomeDirectory Maven configuration option. 


Configuration Options
---------------------

	<configuration>
		<daemon>true</daemon>
		<verticleName>my.package.MyVerticle</verticleName>
		<moduleName>${project.artifactId}-${project.version}-mod</moduleName>
		<vertxHomeDirectory>/path/to/vertx2</vertxHomeDirectory>
		<vertxModulesDirectory>${basedir}/mods</vertxModulesDirectory>
		<classpath>file:///extra/entries</classpath>
		<worker>false</worker>
		<configFile>/path/to/MyVerticle.conf</configFile>
		<instances>1</instances>
	</configuration>

* daemon: Sets plugin execution mode. Not applicable to the "run" goal.
* verticleName: The verticle to be executed.
* moduleName: The module to be executed.
* vertxHomeDirectory: The directory where vertx2 is installed (not required).
* vertxModulesDirectory: The directory where modules are to be installed.
* classpath: Extra entries for the classpath. Notice that maven artifacts will be included automatically in the classpath.
* worker: Indicates thar the verticle to be deployed is a worker verticle.
* configFile: The config file to be used.
* instances: The number of verticle instances.
