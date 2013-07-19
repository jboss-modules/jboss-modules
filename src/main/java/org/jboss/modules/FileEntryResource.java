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

import static java.security.AccessController.doPrivileged;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * A file entry resource.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class FileEntryResource implements Resource {

    private final String name;
    private final File file;
    private final URL url;
    private final AccessControlContext context;

    FileEntryResource(final String name, final File file, final URL url, final AccessControlContext context) {
        this.name = name;
        this.file = file;
        this.url = url;
        this.context = context;
    }

    public long getSize() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return doPrivileged(new PrivilegedAction<Long>() {
                public Long run() {
                    return Long.valueOf(file.length());
                }
            }, context).longValue();
        } else {
            return file.length();
        }
    }

    public String getName() {
        return name;
    }

    public URL getURL() {
        return url;
    }

    public InputStream openStream() throws IOException {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                return doPrivileged(new PrivilegedExceptionAction<FileInputStream>() {
                    public FileInputStream run() throws IOException {
                        return new FileInputStream(file);
                    }
                }, context);
            } catch (PrivilegedActionException e) {
                try {
                    throw e.getException();
                } catch (RuntimeException e1) {
                    throw e1;
                } catch (IOException e1) {
                    throw e1;
                } catch (Exception e1) {
                    throw new UndeclaredThrowableException(e1);
                }
            }
        } else {
            return new FileInputStream(file);
        }
    }
}
