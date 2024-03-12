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

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlContext;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static java.security.AccessController.doPrivileged;
import static java.security.AccessController.getContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ClassLoaderLocalLoader implements LocalLoader {

    static final LocalLoader SYSTEM = JDKSpecific.getSystemLocalLoader();

    private final ClassLoader classLoader;
    private final AccessControlContext context;

    /**
     * Construct a new instance.
     *
     * @param classLoader the classloader to which we delegate
     */
    ClassLoaderLocalLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        context = getContext();
    }

    // Public members

    public Class<?> loadClassLocal(final String name, final boolean resolve) {
        try {
            return Class.forName(name, resolve, classLoader);
        } catch (ClassNotFoundException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                //unlikely
                throw (RuntimeException) cause;
            }
            return null;
        }
    }

    public Package loadPackageLocal(final String name) {
        return classLoader.getDefinedPackage(name);
    }

    public List<Resource> loadResourceLocal(final String name) {
        final Enumeration<URL> urls;
        ClassLoader classLoader = this.classLoader;
        try {
            if (classLoader == null) {
                urls = JDKSpecific.getSystemResources(name);
            } else {
                urls = classLoader.getResources(name);
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
        final List<Resource> list = new ArrayList<>();
        while (urls.hasMoreElements()) {
            final URL url = urls.nextElement();
            URLConnection connection = null;
            try {
                connection = doPrivileged(new GetURLConnectionAction(url), context);
            } catch (PrivilegedActionException e) {
                try {
                    throw e.getException();
                } catch (IOException e2) {
                    // omit from list
                } catch (RuntimeException re) {
                    throw re;
                } catch (Exception e2) {
                    throw new UndeclaredThrowableException(e2);
                }
            }
            list.add(new URLConnectionResource(connection));
        }
        return list;
    }
}
