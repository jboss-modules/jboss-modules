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

import org.jboss.modules.filter.PathFilter;
import org.junit.Assert;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Test the functionality of the JarResourceLoader.
 *
 * @author John Bailey
 */
public class JarResourceLoaderTest extends AbstractResourceLoaderTestCase {

    private JarFile jarFile;

    protected ResourceLoader createLoader(final PathFilter exportFilter) throws Exception {
        File fileResourceRoot = getResource("test/fileresourceloader");
        // Copy the classfile over
        copyResource("org/jboss/modules/test/TestClass.class", "test/fileresourceloader", "org/jboss/modules/test");

        // Build a jar to match the fileresource loader
        final File outputFile = new File(getResource("test"), "jarresourceloader/test.jar");
        outputFile.getParentFile().mkdirs();
        buildJar(fileResourceRoot, outputFile);
        // Create the jar file and resource loader
        jarFile = new JarFile(outputFile, true);
        return new JarFileResourceLoader("test-root", jarFile);
    }

    @Override
    protected void assertResource(Resource resource, String fileName) {
        final JarEntry entry = jarFile.getJarEntry(fileName);
        Assert.assertEquals(entry.getSize(), resource.getSize());
    }

    private void buildJar(final File source, final File targetFile) throws IOException {
        final JarOutputStream target = new JarOutputStream(new FileOutputStream(targetFile));
        final String sourceBase = source.getPath();
        add(sourceBase, source, target);
        target.close();
    }

    private void add(final String sourceBase, final File source, final JarOutputStream target) throws IOException {
        BufferedInputStream in = null;
        String entryName = source.getPath().replace(sourceBase, "").replace("\\", "/");
        if(entryName.startsWith("/"))
            entryName = entryName.substring(1);
        try {
            if (source.isDirectory()) {
                if (!entryName.isEmpty()) {
                    if (!entryName.endsWith("/"))
                        entryName += "/";
                    final JarEntry entry = new JarEntry(entryName);
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
                for (File nestedFile : source.listFiles())
                    add(sourceBase, nestedFile, target);
                return;
            }

            final JarEntry entry = new JarEntry(entryName);
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));

            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = in.read(buffer)) != -1) {
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        }
        finally {
            if (in != null) in.close();
        }
    }
}
