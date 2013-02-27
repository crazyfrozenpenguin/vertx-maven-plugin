package org.vertx.maven.plugin.mojo;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.vertx.maven.plugin.server.VertxServer;
import org.vertx.maven.plugin.server.profile.VertxServerLauncher;

/**
 * <p>
 * This goal is used to start a vert.x verticle or module in it's own instance.
 * </p>
 * q
 * <p>
 * The plugin forks a parallel lifecycle to ensure that the "package" phase has
 * been completed before invoking vert.x. This means that you do not need to
 * explicitly execute a "mvn package" first. It also means that a
 * "mvn clean vertx:start" will ensure that a full fresh compile and package is
 * done before invoking vert.x.
 * </p>
 * 
 * @description Starts vert.x directly from a Maven project.
 */
@Mojo(name = "start", requiresProject = true, threadSafe = true, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class VertxStartMojo extends AbstractMojo {

	private static final String CP_SEPARATOR = System.getProperty("os.name").startsWith("Windows") ? ";" : ":";

	private static final String VERTX_INSTALL_SYSTEM_PROPERTY = "vertx.install";

	private static final String VERTX_MODS_SYSTEM_PROPERTY = "vertx.mods";

	/**
	 * The Maven project.
	 * 
	 */
	@Component
	protected MavenProject mavenProject;

	/**
	 * The name of the verticle to run.
	 * 
	 * If you're running a verticle written in JavaScript, Ruby, or Groovy then
	 * it's just the name of the script, e.g. server.js, server.rb, or
	 * server.groovy.
	 * 
	 * If the verticle is written in Java, the name is the fully qualified class
	 * name of the Main class e.g. com.acme.MyVerticle.
	 * 
	 */
	@Parameter(property = "run.verticleClass")
	protected String verticleName;

	/**
	 * The name of the module to run.
	 * 
	 * If you're running a module, it's the name of the module to be run.
	 * 
	 */
	@Parameter(property = "run.moduleName")
	protected String moduleName;

	/**
	 * The host on which to run the cluster. If this is not specified, then the
	 * cluster mode will not be activated.
	 * 
	 */
	@Parameter(property = "run.clusterHost")
	protected String clusterHost;

	/**
	 * The cluster port to use, if not specified the vertx default is used.
	 * 
	 */
	@Parameter(property = "run.clusterPort")
	protected final Integer clusterPort = -1;

	/**
	 * Determines whether the verticle is a worker verticle or not. The default
	 * is false.
	 * 
	 */
	@Parameter(property = "run.worker", defaultValue = "false")
	protected boolean worker;

	/**
	 * <p>
	 * Determines whether or not the server blocks when started. The default
	 * behaviour (daemon = false) will cause the server to pause other processes
	 * while it continues to run the verticle. This is useful when starting the
	 * server with the intent to work with it interactively.
	 * </p>
	 * <p>
	 * Often, it is desirable to let the server start and continue running
	 * subsequent processes in an automated build environment. This can be
	 * facilitated by setting daemon to true.
	 * </p>
	 * 
	 */
	@Parameter(property = "run.daemon", defaultValue = "true")
	protected boolean daemon;

	/**
	 * <p>
	 * The config file for this verticle.
	 * </p>
	 * <p>
	 * If the path is relative (does not start with / or a drive letter like
	 * C:), the path is relative to the directory containing the POM.
	 * </p>
	 * <p>
	 * An example value would be src/main/resources/com/acme/MyVerticle.conf
	 * </p>
	 * 
	 */
	@Parameter(property = "run.configFile")
	protected File configFile;

	/**
	 * The number of instances of the verticle to instantiate in the vert.x
	 * server. The default is 1.
	 * 
	 */
	@Parameter(property = "run.instances", defaultValue = "1")
	protected Integer instances;

	/**
	 * <p>
	 * The path on which to search for the main and any other resources used by
	 * the verticle.
	 * </p>
	 * <p>
	 * If your verticle references other scripts, classes or other resources
	 * (e.g. jar files) then make sure these are on this path. The path can
	 * contain multiple path entries separated by : (colon).
	 * </p>
	 * 
	 */
	@Parameter(property = "run.classpath")
	protected String classpath;

	/**
	 * <p>
	 * The home directory of your vert.x installation i.e. where you unzipped
	 * the vert.x distro. For example C:/vert.x/vert.x-1.0.1.final
	 * </p>
	 * <p>
	 * You will need to set this configuration option if you want to run any
	 * out-of-the box modules like web-server.
	 * </p>
	 * 
	 */
	@Parameter(property = "run.vertxHomeDirectory")
	protected String vertxHomeDirectory;

	/**
	 * <p>
	 * The home directory of your project modules i.e. target/mods
	 * </p>
	 * <p>
	 * You will need to set this configuration option if you want to run any
	 * out-of-the box modules like web-server.
	 * </p>
	 * 
	 */
	@Parameter(property = "run.vertxModulesDirectory", defaultValue = "${project.build.directory}")
	protected String vertxModulesDirectory;

	@Override
	public void execute() throws MojoExecutionException {

		if (vertxHomeDirectory != null) {
			System.setProperty(VERTX_INSTALL_SYSTEM_PROPERTY, vertxHomeDirectory);
			System.setProperty(VERTX_MODS_SYSTEM_PROPERTY, vertxHomeDirectory + "/mods");
			getLog().info("Vert.X home: " + vertxHomeDirectory);
		}

		if (vertxModulesDirectory != null) {
			System.setProperty(VERTX_MODS_SYSTEM_PROPERTY, vertxModulesDirectory);
			getLog().info("Vert.X modules: " + vertxModulesDirectory);
		}

		final List<String> args = new ArrayList<>();
		boolean isModule = false;

		if (moduleName != null) {
			getLog().info("Launching module [" + moduleName + "]");
			args.add(moduleName);
			isModule = true;
		} else if (verticleName != null) {
			getLog().info("Launching verticle [" + verticleName + "]");
			args.add(verticleName);
		} else {
			throw new MojoExecutionException("You have to specify either verticleName or moduleName parameter.");
		}

		final String classpath = getFullClasspath();
		args.add("-cp");
		args.add(classpath);

		if (worker) {
			args.add("-worker");
		}

		if (configFile != null) {
			args.add("-conf");
			args.add(configFile.getAbsolutePath());
		}

		if (clusterHost != null) {
			args.add("-cluster");
			args.add("-cluster-host");
			args.add(clusterHost);
			if (clusterPort > 0) {
				args.add("-cluster-port");
				args.add(Integer.toString(clusterPort));
			}
		}

		args.add("-instances");
		args.add(instances.toString());

		final VertxServer server = new VertxServer(new VertxServerLauncher(args, classpath, getLog()));
		if (isModule) {
			server.runModule(daemon);
		} else {
			server.runVerticle(daemon);
		}
	}

	/**
	 * @return classpath including Maven dependencies
	 * @throws MojoExecutionException
	 */
	private String getFullClasspath() throws MojoExecutionException {
		final StringBuilder classpathBuilder = new StringBuilder(getDefaultClasspathString());
		if (classpath != null) {
			classpathBuilder.append(CP_SEPARATOR).append(classpath);
		}
		try {
			@SuppressWarnings("unchecked")
			final List<String> runtimeClasspathElements = mavenProject.getRuntimeClasspathElements();
			for (final String element : runtimeClasspathElements) {
				classpathBuilder.append(CP_SEPARATOR).append(element);
			}
		} catch (final DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("Could not list runtime classpath elements");
		}

		final String fullClasspath = classpathBuilder.toString();

		getLog().debug("Full classpath [" + fullClasspath + "]");

		return fullClasspath;
	}

	/**
	 * Build a default classpath.
	 * 
	 * If the packaging of the Maven project is jar then add the path of the
	 * compiled jar file to the default classpath.
	 * 
	 * @return default classpath
	 */
	private String getDefaultClasspathString() {
		String defaultClasspath = ".";

		if (mavenProject.getPackaging().toUpperCase().equals("JAR")) {
			defaultClasspath += CP_SEPARATOR + mavenProject.getBuild().getDirectory().replace("\\", "/") + "/"
					+ mavenProject.getBuild().getFinalName() + "." + mavenProject.getPackaging();
		}

		getLog().debug("Default classpath [" + defaultClasspath + "]");
		return defaultClasspath;
	}
}
