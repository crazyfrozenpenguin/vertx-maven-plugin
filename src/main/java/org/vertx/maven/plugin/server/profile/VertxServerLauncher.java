package org.vertx.maven.plugin.server.profile;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class VertxServerLauncher extends VertxServerProfile {

	private Object platformManager;
	private Method undeployAll;
	private Method stopPM;

	public VertxServerLauncher(final List<String> serverArgs, final String classpath, final Log log) {
		super(serverArgs, classpath, log);
	}

	@Override
	public void run() {

		while (!Thread.interrupted()) {

			try {
				if (platformManager == null) {
					final Class<?> platformLocator = Thread.currentThread().getContextClassLoader()
							.loadClass("org.vertx.java.platform.PlatformLocator");

					final Field factoryField = platformLocator.getField("factory");
					factoryField.setAccessible(true);

					final Object factory = factoryField.get(null);

					final Method createPlatformManager = factory.getClass().getMethod("createPlatformManager");
					createPlatformManager.setAccessible(true);

					platformManager = createPlatformManager.invoke(factory);

					final Class<?> jsonObjectClass = Thread.currentThread().getContextClassLoader()
							.loadClass("org.vertx.java.core.json.JsonObject");

					final Class<?> handlerClass = Thread.currentThread().getContextClassLoader()
							.loadClass("org.vertx.java.core.Handler");

					undeployAll = platformManager.getClass().getMethod("undeployAll", new Class[] { handlerClass });

					stopPM = platformManager.getClass().getMethod("stop");

					Object jsonObject = null;
					if (this.serverArgs.contains("-conf")) {
						final String conf = this.serverArgs.get(this.serverArgs.indexOf("-conf") + 1);

						final String confContent = readConfigFile(conf);

						if (confContent != null && !confContent.isEmpty()) {
							jsonObject = jsonObjectClass.getConstructor(String.class).newInstance(confContent);
						}
					}

					int instances = 1;
					if (this.serverArgs.contains("-instances")) {
						final String instancesStr = this.serverArgs.get(this.serverArgs.indexOf("-instances") + 1);
						instances = Integer.valueOf(instancesStr);
					}

					if (serverArgs.get(0).equals("run")) {

						if (this.serverArgs.contains("-worker")) {

							final Method deployWorkerVerticle = platformManager.getClass().getMethod(
									"deployWorkerVerticle",
									new Class[] { boolean.class, String.class, jsonObjectClass, URL[].class, int.class,
											String.class, handlerClass });
							deployWorkerVerticle.setAccessible(true);

							deployWorkerVerticle.invoke(platformManager, new Object[] { false, serverArgs.get(1),
									jsonObject, urls, instances, null, null });

						} else {

							final Method deployVerticle = platformManager.getClass().getMethod(
									"deployVerticle",
									new Class[] { String.class, jsonObjectClass, URL[].class, int.class, String.class,
											handlerClass });
							deployVerticle.setAccessible(true);

							deployVerticle.invoke(platformManager, new Object[] { serverArgs.get(1), jsonObject, urls,
									instances, null, null });

						}

					} else if (serverArgs.get(1).endsWith(".jar") || serverArgs.get(1).endsWith(".zip")) {

						final Method deployModuleAsZip = platformManager.getClass().getMethod("deployModuleFromZip",
								new Class[] { String.class, jsonObjectClass, int.class, handlerClass });
						deployModuleAsZip.setAccessible(true);

						deployModuleAsZip.invoke(platformManager, new Object[] { serverArgs.get(1), jsonObject,
								instances, null });

					} else {

						final Method deployModule = platformManager.getClass().getMethod("deployModule",
								new Class[] { String.class, jsonObjectClass, int.class, handlerClass });
						deployModule.setAccessible(true);

						deployModule.invoke(platformManager, new Object[] { serverArgs.get(1), jsonObject, instances,
								null });

					}

				}

				Thread.sleep(1000);

			} catch (final InterruptedException e) {
				log.debug("Vert.x: Thread interrupted");
				break;
			} catch (final ClassNotFoundException | IllegalAccessException | NoSuchFieldException | SecurityException
					| NoSuchMethodException | IllegalArgumentException | InvocationTargetException
					| InstantiationException e) {
				log.debug("Vert.x: Failed to initialize", e);
				break;
			} catch (final Exception e) {
				log.debug("Vert.x: An unexpected error has occured", e);
				break;
			}
		}

		log.info("Vert.x: shutdown initiated");

	}

	@Override
	public void stop() {

		try {
			undeployAll.invoke(platformManager, new Object[] { null });
			log.info("Vert.x: Undeploying all verticles and modules");
			stopPM.invoke(platformManager);
			log.info("Vert.x: shutdown complete");
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.debug("Vert.x: An unexpected error as occurred during shutdown", e);

		}
	}
}
