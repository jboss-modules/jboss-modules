/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
        jarFile = new JarFile(outputFile);
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
