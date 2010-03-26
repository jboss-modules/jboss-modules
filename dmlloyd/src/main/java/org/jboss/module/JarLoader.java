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

package org.jboss.module;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarLoader extends ResourceLoader {
    private final JarFile jarFile;

    public JarLoader(final File jarFile) throws IOException {
        this.jarFile = new JarFile(jarFile.getAbsoluteFile());
    }

    public Resource getResource(final String name) {
        final JarEntry entry = jarFile.getJarEntry(name);
        return entry == null ? null : new JarEntryResource(jarFile, entry);
    }

    public ClassSpec getClassSpec(final String name) throws IOException {
        final String fileName = name + ".class";
        JarEntry entry = jarFile.getJarEntry(fileName);
        if (entry == null) {
            return null;
        }
        final long size = entry.getSize();
        if (size > Integer.MAX_VALUE) {
            throw new IOException("Class entry is too large");
        }
        final int isize = (int) size;
        final byte[] data = new byte[(int) size];
        final InputStream is = jarFile.getInputStream(entry);
        try {
            int i = 0;
            int res;
            do {
                res = is.read(data, i, isize - i);
                if (res == -1) {
                    if (i < isize) {
                        throw new IOException("File size shrunk unexpectedly");
                    }
                }
                i += res;
            } while (i < isize);
            if (is.read() != -1) {
                throw new IOException("File size grew unexpectedly");
            }
            final ClassSpec spec = new ClassSpec();
            spec.setBytes(data);
            final CodeSigner[] signers = entry.getCodeSigners();
            if (signers != null) {
                spec.setCodeSource(new CodeSource(/* (todo - get URL) */ null, signers));
            }
            return spec;
        } finally {
            safeClose(is);
        }
    }

    public PackageSpec getPackageSpec(final String name) {
        return null;
    }

    public String getLibrary(final String name) {
        return null;
    }

    private static void safeClose(final Closeable c) {
        if (c != null) try {
            c.close();
        } catch (IOException e) {
            // todo log
        }
    }
}
