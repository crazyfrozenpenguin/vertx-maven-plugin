package org.vertx.maven.plugin.server.profile;

import static java.lang.Thread.currentThread;
import static java.nio.file.Files.readAllBytes;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

public class VertxDeployer {

	private PlatformManager pm;

	public void deploy(final List<String> serverArgs, final URL[] urls) throws Exception {

		pm = PlatformLocator.factory.createPlatformManager();

		JsonObject config = null;

		if (serverArgs.contains("-conf")) {
			final String conf = serverArgs.get(serverArgs.indexOf("-conf") + 1);

			final String confContent = readConfigFile(conf);

			if (confContent != null && !confContent.isEmpty()) {
				config = new JsonObject(confContent);
			}
		}

		int instances = 1;
		if (serverArgs.contains("-instances")) {
			final String instancesStr = serverArgs.get(serverArgs.indexOf("-instances") + 1);
			instances = Integer.valueOf(instancesStr);
		}

		final CompletionHandler handler = new CompletionHandler(currentThread());
		try {
			if (serverArgs.get(0).equals("run")) {
				if (serverArgs.contains("-worker")) {
					pm.deployWorkerVerticle(false, serverArgs.get(1), config, urls, instances, null, handler);
				} else {
					pm.deployVerticle(serverArgs.get(1), config, urls, instances, null, handler);
				}
			} else if (serverArgs.get(1).endsWith(".jar") || serverArgs.get(1).endsWith(".zip")) {
				pm.deployModuleFromZip(serverArgs.get(1), config, instances, handler);
			} else {
				pm.deployModule(serverArgs.get(1), config, instances, handler);
			}
			currentThread().join();
		} catch (final InterruptedException e) {
			// empty
		}
	}

	public void undeployAll() {
		pm.undeployAll(null);
	}

	public void stop() {
		pm.stop();
	}

	private String readConfigFile(final String strFile) throws Exception {
		final File file = new File(strFile);
		final URI uri = file.toURI();
		byte[] bytes = null;
		try {
			bytes = readAllBytes(java.nio.file.Paths.get(uri));
		} catch (final IOException e) {
			throw new Exception("Failed to read config file: ", e);
		}

		return new String(bytes);
	}

	private class CompletionHandler implements Handler<String> {

		private Thread t = null;

		public CompletionHandler(final Thread t) {
			this.t = t;
		}

		@Override
		public void handle(final String event) {
			t.interrupt();
		}

	}

}
