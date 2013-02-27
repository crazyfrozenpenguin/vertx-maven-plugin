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
import static org.vertx.maven.plugin.server.VertxServer.shutdown;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

/**
 * <p>
 * This goal is used to run a vert.x verticle in it's own instance.
 * </p>
 * q
 * <p>
 * The plugin forks a parallel lifecycle to ensure that the "package" phase has
 * been completed before invoking vert.x. This means that you do not need to
 * explicitly execute a "mvn package" first. It also means that a
 * "mvn clean vertx:run" will ensure that a full fresh compile and package is
 * done before invoking vert.x.
 * </p>
 * 
 * @description Stops vert.x directly from a Maven project at
 *              post-integration-test phase.
 */
@Mojo(name = "stop", requiresProject = true, threadSafe = true, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class VertxStopMojo extends AbstractMojo {

	/**
	 * The Maven project.
	 * 
	 * @parameter property="${project}"
	 * @required
	 */
	@Component
	protected MavenProject mavenProject;

	@Override
	public void execute() throws MojoExecutionException {
		shutdown();
	}
}
