package org.vertx.maven.plugin.server.profile;

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

	private final PlatformManager pm;
	private Boolean deployed = false;

	public VertxDeployer() {
		pm = PlatformLocator.factory.createPlatformManager();
	}

	public void deploy(final List<String> serverArgs, final URL[] urls) throws Exception {
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

		final CompletionHandler handler = new CompletionHandler();
		if (serverArgs.get(1).endsWith(".jar") || serverArgs.get(1).endsWith(".zip")) {
			pm.deployModuleFromZip(serverArgs.get(1), config, instances, handler);
		} else {
			pm.deployModule(serverArgs.get(1), config, instances, handler);
		}
	}

	public void pullInDependencies(final String moduleName) {
		pm.pullInDependencies(moduleName);
	}

	public void undeployAll() {
		pm.undeployAll(null);
	}

	public void stop() {
		pm.stop();
	}

	public boolean isDeployed() throws Exception {
		if (deployed == null) {
			throw new Exception("Vert.x has failed to deploy module");
		}
		return deployed;
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
		@Override
		public void handle(final String event) {
			if (event == null) {
				deployed = null;
			} else {
				System.err.println("Vert.x has finished deploying");
				deployed = true;
			}
		}

	}

}
