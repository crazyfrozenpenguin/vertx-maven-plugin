package org.vertx.maven.plugin.mojo;

import static java.lang.Thread.currentThread;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

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
 * @description Runs vert.x directly from a Maven project.
 */
@Mojo(name = "runmod", requiresProject = true, threadSafe = false, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class VertxRunModMojo extends BaseVertxMojo {

    @Override
    public void execute() throws MojoExecutionException {
        this.daemon = false;
        args = getArgs();
        args.add(0, VERTX_RUNMOD_COMMAND);

        ClassLoader classLoader = new URLClassLoader(getClassPathUrls(args));
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            Class<?> deployer = currentThread().getContextClassLoader()
                    .loadClass("org.vertx.maven.plugin.mojo.VertxManager");
            Object deployerObj = deployer.newInstance();
            Method deploy;

            deploy = deployer.getMethod("deploy", new Class[] { List.class,
                    URL[].class });
            deploy.invoke(deployerObj, new Object[] { args,
                    getClassPathUrls(args) });

            Thread.currentThread().setContextClassLoader(cl);

        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }

    }

}
