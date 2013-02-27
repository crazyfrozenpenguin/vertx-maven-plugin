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

import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.vertx.maven.plugin.server.profile.VertxServerProfile;

public class VertxServer {

	private static final String VERTX_RUN_COMMAND = "run";

	private static final String VERTX_RUNMOD_COMMAND = "runmod";

	private static Thread bootstrapThread;

	private static final List<VertxServerProfile> runnables = new LinkedList<VertxServerProfile>();

	private boolean daemon = false;

	private final VertxServerProfile runnable;

	private static Log log;

	public VertxServer(final VertxServerProfile runnable) {
		this.runnable = runnable;
		log = runnable.getLog();
		runnables.add(this.runnable);
	}

	private void run(final boolean daemon) {
		this.daemon = daemon;

		try {
			final ClassLoader classLoader = new URLClassLoader(runnable.getUrls());
			executeWithClassLoader(runnable, classLoader);
		} catch (final Exception e) {
			log.debug("Vert.x: Unexpected classload URL", e);
		}

	}

	public static void shutdown() {
		try {
			if (bootstrapThread != null && bootstrapThread.isAlive()) {
				log.info("Vert.x: shutdown requested");
				bootstrapThread.interrupt();

				for (final VertxServerProfile runnableProfile : runnables) {
					runnableProfile.stop();
				}

				// Wait until the thread exits
				try {
					bootstrapThread.join();
				} catch (final InterruptedException ex) {
					// Unexpected interruption
					ex.printStackTrace();
				}
			}
		} catch (final Exception e) {
		}
	}

	private void executeWithClassLoader(final Runnable runnable, final ClassLoader classLoader)
			throws MojoExecutionException {

		final IsolatedThreadGroup threadGroup = new IsolatedThreadGroup(runnable.getClass().getName());

		bootstrapThread = new Thread(threadGroup, runnable, runnable.getClass().getName() + ".run()");
		bootstrapThread.setContextClassLoader(classLoader);
		bootstrapThread.start();

		if (!daemon) {
			try {
				bootstrapThread.join();
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt(); // good practice if don't
													// throw
				log.debug("Vert.x: interrupted while joining against thread " + bootstrapThread, e); // not
																										// expected!
			}

			synchronized (threadGroup) {
				if (threadGroup.uncaughtException != null) {
					throw new MojoExecutionException("An exception occured while executing the Java class. "
							+ threadGroup.uncaughtException.getMessage(), threadGroup.uncaughtException);
				}
			}
		}
	}

	public void runVerticle(final boolean daemon) {
		runnable.getServerArgs().add(0, VERTX_RUN_COMMAND);
		this.run(daemon);
	}

	public void runModule(final boolean daemon) {
		runnable.getServerArgs().add(0, VERTX_RUNMOD_COMMAND);
		this.run(daemon);
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
