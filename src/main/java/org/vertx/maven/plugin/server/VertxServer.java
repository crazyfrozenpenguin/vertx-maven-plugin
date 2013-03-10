package org.vertx.maven.plugin.server;

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

import static java.lang.Thread.sleep;

import java.net.URLClassLoader;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.vertx.maven.plugin.server.profile.VertxServerLauncher;

public enum VertxServer {
	VertxServer;

	private static final String VERTX_RUN_COMMAND = "run";
	private static final String VERTX_RUNMOD_COMMAND = "runmod";

	private Thread bootstrapThread;
	private VertxServerLauncher runnable;
	private Log log;

	private boolean daemon = false;
	private ClassLoader classLoader;

	public void init(final VertxServerLauncher runnable) {
		if (VertxServer.runnable == null) {
			VertxServer.runnable = runnable;
			VertxServer.runnable.getServerArgs().add(0, VERTX_RUNMOD_COMMAND);
			log = runnable.getLog();
			initClassLoader();
		}
	}

	public void runModule(final boolean daemon) {
		runnable.deploy();
		this.run(daemon);
	}

	public void pullInDependencies() {
		runnable.pullInDependencies();
		this.run(true);
	}

	public void shutdown() {
		try {
			if (bootstrapThread != null && bootstrapThread.isAlive()) {
				log.info("Vert.x: shutdown requested");
				bootstrapThread.interrupt();

				runnable.stop();

				try {
					bootstrapThread.join();
				} catch (final InterruptedException ex) {
					log.debug("bootstrap thread interrupted", ex);
				}
			}
		} catch (final Exception e) {
			// empty
		}
	}

	private void run(final boolean daemon) {
		this.daemon = daemon;

		try {
			executeWithClassLoader(runnable, classLoader);
		} catch (final Exception e) {
			log.debug("Vert.x: Unexpected classload URL", e);
		}

	}

	private void executeWithClassLoader(final VertxServerLauncher profile, final ClassLoader classLoader)
			throws MojoExecutionException {

		final IsolatedThreadGroup threadGroup = new IsolatedThreadGroup(profile.getClass().getName());

		bootstrapThread = new Thread(threadGroup, profile, profile.getClass().getName() + ".run()");
		bootstrapThread.setContextClassLoader(classLoader);
		bootstrapThread.start();

		// wait for deployment to complete
		while (bootstrapThread.isAlive() && !profile.isDeployed()) {
			try {
				sleep(1000);
			} catch (final InterruptedException e) {
				// empty
			}
		}

		if (!daemon) {
			try {
				bootstrapThread.join();
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				log.debug("Vert.x: interrupted while joining against thread " + bootstrapThread, e);
			}

			synchronized (threadGroup) {
				if (threadGroup.uncaughtException != null) {
					throw new MojoExecutionException("An exception occured while executing the Java class. "
							+ threadGroup.uncaughtException.getMessage(), threadGroup.uncaughtException);
				}
			}
		}
	}

	private void initClassLoader() {
		if (classLoader == null) {
			classLoader = new URLClassLoader(runnable.getUrls());
		}
	}

	public class IsolatedThreadGroup extends ThreadGroup {

		private Throwable uncaughtException;

		public IsolatedThreadGroup(final String name) {
			super(name);
		}

		@Override
		public void uncaughtException(final Thread thread, final Throwable throwable) {

			if (throwable instanceof ThreadDeath) {
				return;
			}

			boolean doLog = false;
			synchronized (this) {
				if (uncaughtException == null) {
					uncaughtException = throwable;
				} else {
					doLog = true;
				}
			}
			if (doLog) {
				log.debug("Vert.x: Isolated Thread Group uncaught exception: ", throwable);
			}
		}
	}

}
