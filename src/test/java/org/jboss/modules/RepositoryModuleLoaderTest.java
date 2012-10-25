/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.modules;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

/**
 * Test cases to ensure that the {@link RepositoryModuleLoader} is working as contracted
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 */
public class RepositoryModuleLoaderTest {

    private static final Logger log = Logger.getLogger(RepositoryModuleLoaderTest.class.getName());
    private static final File LOCAL_REPO = new File("target", ".jboss" + File.separatorChar + "modules"
        + File.separatorChar + "repo");

    private static final int HTTP_TEST_PORT = 12345;
    private static Server httpServer;

    @BeforeClass
    public static void clearLocalRepo() throws IOException {
        rm(LOCAL_REPO);
        if (!LOCAL_REPO.mkdirs()) {
            throw new IllegalStateException("Could not make clean local repository: " + LOCAL_REPO.getAbsolutePath());
        }

        // Start an Embedded HTTP Server
        final Handler handler = new StaticFileHandler();
        httpServer = new Server(HTTP_TEST_PORT);
        httpServer.setHandler(handler);
        try {
            httpServer.start();
            log.info("HTTP Server Started: " + httpServer);
        } catch (final Exception e) {
            throw new RuntimeException("Could not start server");
        }

    }

    private static void rm(final File root) {
        if (root.isDirectory()) {
            for (final File child : root.listFiles()) {
                rm(child);
            }
        }
        root.delete();
        log.info("Deleted: " + root.getAbsolutePath());
    }

    @AfterClass
    public static void shutdownHttpServer() {
        if (httpServer != null) {
            try {
                httpServer.stop();
            } catch (final Exception e) {
                // Swallow
                log.severe("Could not stop HTTP Server cleanly, " + e.getMessage());
            }
            log.info("HTTP Server Stopped: " + httpServer);
        }
    }

    @Test
    public void correctDefaultLocalRepo() throws MalformedURLException {
        RepositoryModuleLoader loader = RepositoryModuleLoader.create(new URL("http://localhost:" + HTTP_TEST_PORT));
        Assert.assertEquals("Default local repository not as expected", new File(System.getProperty("user.home"),
            ".jboss" + File.separatorChar + "modules" + File.separatorChar + "repo"), loader.localRepoRoot);
    }

    @Test
    public void loadClassFromModule() {
        final ModuleLoader moduleLoader;
        try {
            moduleLoader = RepositoryModuleLoader.create(new URL("http://localhost:" + HTTP_TEST_PORT), LOCAL_REPO);
        } catch (final MalformedURLException murle) {
            throw new RuntimeException(murle);
        }

        final String MODULE_ID_COMMON_CORE = "org.jboss.common-core";
        final ModuleIdentifier commonCoreId = ModuleIdentifier.create(MODULE_ID_COMMON_CORE);
        final Module commonCore;
        try {
            commonCore = moduleLoader.loadModule(commonCoreId);
        } catch (final ModuleLoadException mle) {
            throw new RuntimeException("Could not load module", mle);
        }

        final String commonCoreClassName = "org.jboss.util.collection.CachedList";

        // Can we load this class using TCCL? (we shouldn't be able to)
        boolean failedLoad = false;
        try {
            Class.forName(commonCoreClassName, false, Thread.currentThread().getContextClassLoader());
        } catch (final ClassNotFoundException cnfe) {
            failedLoad = true;
        }
        Assert.assertTrue("Class should not load from TCCL", failedLoad);

        // Load the class from the module CL
        final Class<?> cachedListClass;
        try {
            cachedListClass = commonCore.getClassLoader().loadClass(commonCoreClassName);
            Assert.assertEquals(commonCore.getClassLoader(), cachedListClass.getClassLoader());
        } catch (final ClassNotFoundException cnfe) {
            Assert.fail("Should be able to load class from modules");
        }

    }

    /**
     * Jetty Handler to serve a static character file from the web root
     */
    private static class StaticFileHandler extends AbstractHandler implements Handler {

        @Override
        public void handle(final String target, final HttpServletRequest request, final HttpServletResponse response,
            final int dispatch) throws IOException, ServletException {
            // Set content type and status before we write anything to the stream
            response.setContentType("application/java-archive"); // Not registered as MIME, but this seems to be the
                                                                 // most popular for JAR.
            response.setStatus(HttpServletResponse.SC_OK);

            // Obtain the requested file relative to the webroot
            final URL root = getCodebaseLocation();
            final URL fileUrl = new URL(root.toExternalForm() + target);
            URI uri = null;
            try {
                uri = fileUrl.toURI();
            } catch (final URISyntaxException urise) {
                throw new RuntimeException(urise);
            }
            final File file = new File(uri);

            // File not found, so 404
            if (!file.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                log.warning("Requested file is not found: " + file);
                return;
            }

            // Write out content
            final BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            final OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            // Close 'er up
            in.close();
            out.close();
        }

        private URL getCodebaseLocation() throws MalformedURLException {
            return new File("target/test-classes/remoteRepo").toURI().toURL();
        }
    }
}
