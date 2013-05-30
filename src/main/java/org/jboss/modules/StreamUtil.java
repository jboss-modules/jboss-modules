/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class StreamUtil {

    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[16384];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.flush();
    }

    static void safeClose(Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable ignored) {}
    }

    // ZipFile didn't implement Closeable in 1.6
    static void safeClose(ZipFile closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable ignored) {}
    }

    static final void unzip(File src, File destDir) throws IOException {
        final String absolutePath = destDir.getAbsolutePath();
        final ZipFile zip = new ZipFile(src);

        try {
            final Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                final File fp = new File(absolutePath, PathUtils.canonicalize(PathUtils.relativize(entry.getName())));
                final File parent = fp.getParentFile();
                if (! parent.exists()) {
                    parent.mkdirs();
                }
                final InputStream is = zip.getInputStream(entry);
                try {
                    final FileOutputStream os = new FileOutputStream(fp);
                    try {
                        copy(is, os);
                    } finally {
                        safeClose(os);
                    }
                } finally {
                    safeClose(is);
                }
            }
        } finally {
            safeClose(zip);
        }
    }
}
