package org.vertx.maven.plugin.server.profile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public abstract class VertxServerProfile implements Runnable {

	protected List<String> serverArgs;

	protected URL[] urls;

	protected Log log;

	protected VertxServerProfile(final List<String> serverArgs,
			final String classpath, final Log log) {
		this.log = log;
		this.serverArgs = serverArgs;
		classpathToURLs(classpath);
	}

	public void stop() {
		// empty, N/A
	};

	public List<String> getServerArgs() {
		return serverArgs;
	}

	public URL[] getUrls() {
		return urls;
	}

	public Log getLog() {
		return this.log;
	}

	String readConfigFile(final String strFile) {
		final File file = new File(strFile);
		final URI uri = file.toURI();
		byte[] bytes = null;
		try {
			bytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths
					.get(uri));
		} catch (final IOException e) {
			e.printStackTrace();
			return "ERROR loading file " + strFile;
		}

		return new String(bytes);
	}

	private void classpathToURLs(final String classpath) {
		final String[] paths = classpath.split(":");
		this.urls = new URL[paths.length];
		for (int i = 0; i < paths.length; i++) {
			try {
				this.urls[i] = new URL("file:///" + paths[i]);
			} catch (final MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}
}
