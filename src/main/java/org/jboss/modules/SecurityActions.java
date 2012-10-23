/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static java.lang.System.getSecurityManager;
import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;

/**
 * This class must not be public.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SecurityActions {

    private SecurityActions() {
        throw new UnsupportedOperationException("No instances permitted");
    }

    private static final PrivilegedAction<ClassLoader> GET_LOADER_ACTION = new PrivilegedAction<ClassLoader>() {
        @Override
        public ClassLoader run() {
            return currentThread().getContextClassLoader();
        }
    };

    static ClassLoader setContextClassLoader(final ClassLoader classLoader) {
        final SecurityManager sm = getSecurityManager();
        if (sm != null) {
            return doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    try {
                        return currentThread().getContextClassLoader();
                    } finally {
                        currentThread().setContextClassLoader(classLoader);
                    }
                }
            });
        } else {
            try {
                return currentThread().getContextClassLoader();
            } finally {
                currentThread().setContextClassLoader(classLoader);
            }
        }
    }

    static ClassLoader getContextClassLoader() {
        final SecurityManager sm = getSecurityManager();
        if (sm != null) {
            return doPrivileged(GET_LOADER_ACTION);
        } else {
            return currentThread().getContextClassLoader();
        }
    }

    static String getSystemProperty(final String key) {
        assert key != null && key.length() > 0 : "Key must be specified";
        if (System.getSecurityManager() == null) {
            return System.getProperty(key);
        } else {
            return doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(key);
                }
            });
        }
    }
}