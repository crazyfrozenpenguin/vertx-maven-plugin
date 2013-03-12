package org.vertx.maven.plugin.server;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class VertxServerLauncher implements Runnable {

	private final List<String> args;
	private final URL[] urls;
	private final Log log;
	private Class<?> deployer;
	private Object deployerObj;
	private Method isDeployed;
	private Method undeployAll;
	private Method stopPM;
	private Method deploy;

	private String command = "nop";

	public VertxServerLauncher(final List<String> args, final URL[] urls, final Log log) {
		this.args = args;
		this.urls = urls;
		this.log = log;
	}

	@Override
	public void run() {

		while (!interrupted()) {

			try {

				switch (command) {
				case "runMod":
					command = "nop";
					initDeployer();
					deploy = deployer.getMethod("deploy");
					deploy.invoke(deployerObj);
					break;
				case "pullInDeps":
					command = "nop";
					initDeployer();
					final Method pullInDependencies = deployer.getMethod("pullInDependencies",
							new Class[] { String.class });
					pullInDependencies.invoke(deployerObj, new Object[] { args.get(1) });
					break;
				default:
					sleep(1000);
				}
			} catch (final InterruptedException e) {
				log.debug("Vert.x: Thread interrupted");
				break;
			} catch (final ClassNotFoundException | IllegalAccessException | SecurityException | NoSuchMethodException
					| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
				log.error("Vert.x: Failed to initialize", e);
				break;
			} catch (final Exception e) {
				log.error("Vert.x: An unexpected error has occured", e);
				break;
			}
		}
		log.info("Vert.x: shutdown initiated");
	}

	public void deploy() {
		command = "runMod";
	}

	public void pullInDependencies() {
		command = "pullInDeps";
	}

	public boolean isDeployed() {
		try {
			return deployerObj == null ? false : (Boolean) isDeployed.invoke(deployerObj);
		} catch (final Exception e) {
			log.error(e);
		}
		return false;
	}

	public void stop() {
		try {
			if (deployerObj != null) {
				undeployAll.invoke(deployerObj);
				log.info("Vert.x: Undeploying all verticles and modules");
				stopPM.invoke(deployerObj);
			}
			log.info("Vert.x: shutdown complete");
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("Vert.x: An unexpected error as occurred during shutdown", e);
		}
	}

	public URL[] getUrls() {
		return urls;
	}

	public Log getLog() {
		return log;
	}

	private void initDeployer() throws Exception {

		if (deployerObj == null) {
			deployer = currentThread().getContextClassLoader().loadClass("org.vertx.maven.plugin.server.VertxDeployer");

			undeployAll = deployer.getMethod("undeployAll");
			stopPM = deployer.getMethod("stop");
			isDeployed = deployer.getMethod("isDeployed");
			deployerObj = deployer.getConstructor(new Class[] { List.class, URL[].class }).newInstance(
					new Object[] { args, urls });
		}
	}
}
