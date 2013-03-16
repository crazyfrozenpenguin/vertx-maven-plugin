package org.vertx.maven.plugin.mojo;

import static java.nio.file.Files.readAllBytes;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

public class VertxManager {

    private final PlatformManager pm;
    private Boolean deployed = false;

    public VertxManager() {
        pm = PlatformLocator.factory.createPlatformManager();
    }

    public void deploy(final List<String> args, final URL[] urls)
            throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        pm.deployModule(args.get(1), getConf(args), getInstances(args),
                new Handler<String>() {
                    public void handle(String deploymentID) {
                        if (deploymentID != null) {
                            System.out.println("CTRL-C to stop server");
                        } else {
                            System.out.println("Could not find the module.");
                            System.out
                                    .println("Press CTRL-C to exit and do `mvn package`");
                            // latch.countDown();
                        }
                    }
                });
        latch.await(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
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

    private JsonObject getConf(List<String> args) {
        JsonObject config = null;

        if (args.contains("-conf")) {
            final String conf = args.get(args.indexOf("-conf") + 1);

            final String confContent = readConfigFile(conf);

            if (confContent != null && !confContent.isEmpty()) {
                config = new JsonObject(confContent);
            }
        }
        return config;
    }

    private String readConfigFile(final String strFile) {
        final File file = new File(strFile);
        final URI uri = file.toURI();
        byte[] bytes = null;
        try {
            bytes = readAllBytes(java.nio.file.Paths.get(uri));
        } catch (final IOException e) {
            // just returns an empty string. Nothing to be thrown
        }

        return new String(bytes);
    }

    private int getInstances(List<String> args) {

        int instances = 1;
        if (args.contains("-instances")) {
            final String instancesStr = args
                    .get(args.indexOf("-instances") + 1);
            instances = Integer.valueOf(instancesStr);
        }

        return instances;
    }
}
