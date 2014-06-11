/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.modules;

import org.jboss.modules.log.StreamModuleLogger;
import org.jboss.modules.util.Util;
import org.junit.BeforeClass;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstract Test Case used as a base for all module tests.
 *
 * @author John Bailey
 */
public class AbstractModuleTestCase {
    protected static final ModuleIdentifier MODULE_ID = ModuleIdentifier.fromString("test.test");

    @BeforeClass
    public static void initUrlHandler() {
        // this also kicks off Module's static init
        Module.setModuleLogger(new StreamModuleLogger(System.err));
    }

    protected File getResource(final String path) throws Exception {
        return Util.getResourceFile(getClass(), path);
    }

    protected void copyResource(final String inputResource, final String outputBase, final String outputPath) throws Exception {
        final File resource = getResource(inputResource);
        final File outputDirectory = new File(getResource(outputBase), outputPath);

        if(!resource.exists())
            throw new IllegalArgumentException("Resource does not exist");
        if (outputDirectory.exists() && outputDirectory.isFile())
            throw new IllegalArgumentException("OutputDirectory must be a directory");
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs())
                throw new RuntimeException("Failed to create output directory");
        }
        final File outputFile = new File(outputDirectory, resource.getName());
        final InputStream in = new FileInputStream(resource);
        try {
            final OutputStream out = new FileOutputStream(outputFile);
            try {
                final byte[] b = new byte[8192];
                int c;
                while ((c = in.read(b)) != -1) {
                    out.write(b, 0, c);
                }
                out.close();
                in.close();
            } finally {
                safeClose(out);
            }
        } finally {
            safeClose(in);
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            // meh
        }
    }
}
