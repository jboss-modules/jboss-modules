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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ClassLoaderLocalLoader implements LocalLoader {

    static final ClassLoaderLocalLoader SYSTEM = new ClassLoaderLocalLoader(ClassLoaderLocalLoader.class.getClassLoader());

    private static final Method getPackage;

    private final ClassLoader classLoader;
    private final AccessControlContext accessControlContext;

    static {
        getPackage = AccessController.doPrivileged(new PrivilegedAction<Method>() {
            public Method run() {
                for (Method method : ClassLoader.class.getDeclaredMethods()) {
                    if (method.getName().equals("getPackage")) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1 && parameterTypes[0] == String.class) {
                            method.setAccessible(true);
                            return method;
                        }
                    }
                }
                throw new IllegalStateException("No getPackage method found on ClassLoader");
            }
        });
    }

    /**
     * Construct a new instance.
     *
     * @param classLoader the classloader to which we delegate
     */
    ClassLoaderLocalLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.accessControlContext = AccessController.getContext();
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
        try {
            return (Package) getPackage.invoke(classLoader, name);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException re) {
                throw re;
            } catch (Error er) {
                throw er;
            } catch (Throwable throwable) {
                throw new UndeclaredThrowableException(throwable);
            }
        }
    }

    public List<Resource> loadResourceLocal(final String name) {
        final Enumeration<URL> urls;
        ClassLoader classLoader = this.classLoader;
        try {
            if (classLoader == null) {
                urls = ClassLoader.getSystemResources(name);
            } else {
                urls = classLoader.getResources(name);
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
        final List<Resource> list = new ArrayList<Resource>();
        while (urls.hasMoreElements()) {
            list.add(new URLResource(urls.nextElement(), accessControlContext));
        }
        return list;
    }
}
