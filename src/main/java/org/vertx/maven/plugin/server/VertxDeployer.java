package org.vertx.maven.plugin.server;

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
	private Boolean deployed = false;
	private final List<String> args;
	private final URL[] urls;

	public VertxDeployer(final List<String> args, final URL[] urls) {
		this.args = args;
		this.urls = urls;
		if (args.contains("-cluster-host")) {
			pm = PlatformLocator.factory.createPlatformManager(
					Integer.valueOf(args.get(args.indexOf("-cluster-port") + 1)),
					args.get(args.indexOf("-cluster-host") + 1));
		} else {
			pm = PlatformLocator.factory.createPlatformManager();
		}
	}

	public void deploy() throws Exception {
		JsonObject config = null;
		deployed = false;

		if (args.contains("-conf")) {
			final String conf = args.get(args.indexOf("-conf") + 1);

			final String confContent = readConfigFile(conf);

			if (confContent != null && !confContent.isEmpty()) {
				config = new JsonObject(confContent);
			}
		}

		int instances = 1;
		if (args.contains("-instances")) {
			final String instancesStr = args.get(args.indexOf("-instances") + 1);
			instances = Integer.valueOf(instancesStr);
		}

		final CompletionHandler handler = new CompletionHandler();
		if (args.get(1).endsWith(".jar") || args.get(1).endsWith(".zip")) {
			pm.deployModuleFromZip(args.get(1), config, instances, handler);
		} else {
			pm.deployModule(args.get(1), config, instances, handler);
		}
	}

	public void pullInDependencies(final String moduleName) {
		pm.pullInDependencies(moduleName);
		deployed = true;
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
