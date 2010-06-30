/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.modules;

import org.junit.BeforeClass;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertNotNull;

/**
 * Abstract Test Case used as a base for all module tests.
 *
 * @author John Bailey
 */
public class AbstractModuleTestCase {
    protected static final ModuleIdentifier MODULE_ID = new ModuleIdentifier("test", "test", "1.0");

    @BeforeClass
    public static void initUrlHandler() {
        // Hack just to kick off Module's static init
        assertNotNull(Module.SYSTEM);
    }

    protected byte[] readBytes(final InputStream is) throws IOException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            byte[] buff = new byte[1024];
            int read;
            while ((read = is.read(buff)) > -1) {
                os.write(buff, 0, read);
            }
        } finally {
            is.close();
        }
        return os.toByteArray();
    }

    protected File getResource(final String path) throws Exception {
        final URL url = getClass().getClassLoader().getResource(path);
        return new File(url.toURI());
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
        FileReader in = null;
        FileWriter out = null;
        try {
            in = new FileReader(resource);
            out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1)
                out.write(c);
        } finally {
            if(in != null) in.close();
            if(out != null) out.close();
        }
    }
}
