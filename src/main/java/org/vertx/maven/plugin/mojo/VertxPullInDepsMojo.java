package org.vertx.maven.plugin.mojo;

import static java.lang.Thread.currentThread;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.lang.reflect.Method;
import java.net.URLClassLoader;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "pullInDeps", requiresProject = true, threadSafe = false, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class VertxPullInDepsMojo extends BaseVertxMojo {

    @Override
    public void execute() throws MojoExecutionException {
        args = getArgs();
        args.add(0, VERTX_RUNMOD_COMMAND);
        pullInDependencies();
    }

    public void pullInDependencies() {
        try {
            ClassLoader classLoader = new URLClassLoader(getClassPathUrls(args));
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);

            Class<?> deployer = currentThread().getContextClassLoader()
                    .loadClass("org.vertx.maven.plugin.mojo.VertxManager");
            Object deployerObj = deployer.newInstance();

            final Method pullInDependencies = deployer.getMethod(
                    "pullInDependencies", new Class[] { String.class });
            pullInDependencies
                    .invoke(deployerObj, new Object[] { args.get(1) });
            Thread.currentThread().setContextClassLoader(cl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}