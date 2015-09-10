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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
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

    public boolean isDirectory() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return doPrivileged(new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    return file.isDirectory();
                }
            }, context).booleanValue();
        } else {
            return file.isDirectory();
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
