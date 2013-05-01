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

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class Unzip {

    private static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;

        try {
            while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        } finally {
            in.close();
            out.close();
        }
    }

    static final void execute(File src, File destdir) throws IOException {
        ZipFile zip = new ZipFile(src);

        try {
            Enumeration entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                File fp = new File(destdir, entry.getName());
                if (entry.isDirectory() && !fp.exists()) fp.mkdirs();
                else {
                    File parent = fp.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    copyInputStream(zip.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(fp)));
                }
            }
        } finally {
            zip.close();
        }
    }
}
