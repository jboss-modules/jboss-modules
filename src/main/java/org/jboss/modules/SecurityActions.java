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

import java.security.PrivilegedAction;

import static java.lang.System.getSecurityManager;
import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;

/**
 * This class must not be public.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SecurityActions {

    private static final PrivilegedAction<ClassLoader> GET_LOADER_ACTION = new PrivilegedAction<ClassLoader>() {
        public ClassLoader run() {
            return currentThread().getContextClassLoader();
        }
    };

    static ClassLoader setContextClassLoader(final ClassLoader classLoader) {
        final SecurityManager sm = getSecurityManager();
        if (sm != null) {
            return doPrivileged(new PrivilegedAction<ClassLoader>() {
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

    static ClassLoader getClassLoaderOf(Class<?> clazz) {
        final SecurityManager sm = getSecurityManager();
        if (sm != null) {
            return doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            });
        } else {
            return clazz.getClassLoader();
        }
    }
}
