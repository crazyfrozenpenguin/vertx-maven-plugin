package org.vertx.maven.plugin.server.profile;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class VertxServerLauncher extends VertxServerProfile {

	private Class<?> deployer;
	private Object deployerObj;
	private Method undeployAll;
	private Method stopPM;
	private Method deploy;

	private boolean doDeploy = false;
	private boolean doPullInDeps = false;

	public VertxServerLauncher(final List<String> serverArgs, final Log log) {
		super(serverArgs, log);
	}

	@Override
	public void run() {

		while (!interrupted()) {
			try {
				if (deployerObj == null) {
					initDeployer();

					if (doDeploy == true) {
						doDeploy = false;
						final Method isDeployed = deployer.getMethod("isDeployed");
						deploy = deployer.getMethod("deploy", new Class[] { List.class, URL[].class });
						deploy.invoke(deployerObj, new Object[] { serverArgs, urls });

						while (!deployed) {
							deployed = (Boolean) isDeployed.invoke(deployerObj);
							sleep(1000);
						}

					} else if (doPullInDeps == true) {
						doPullInDeps = false;
						final Method pullInDependencies = deployer.getMethod("pullInDependencies",
								new Class[] { String.class });
						pullInDependencies.invoke(deployerObj, new Object[] { serverArgs.get(1) });
						deployed = true;
					}

				}
				sleep(1000);
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
		deployed = false;
		doDeploy = true;
	}

	public void pullInDependencies() {
		deployed = false;
		doPullInDeps = true;
	}

	@Override
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

	private void initDeployer() throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
			IllegalAccessException {

		if (deployerObj == null) {
			deployer = currentThread().getContextClassLoader().loadClass(
					"org.vertx.maven.plugin.server.profile.VertxDeployer");

			undeployAll = deployer.getMethod("undeployAll");
			stopPM = deployer.getMethod("stop");

			deployerObj = deployer.newInstance();
		}
	}
}
