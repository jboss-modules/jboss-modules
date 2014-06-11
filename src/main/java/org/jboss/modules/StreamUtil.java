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
