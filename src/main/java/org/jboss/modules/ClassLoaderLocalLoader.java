/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ClassLoaderLocalLoader implements LocalLoader {

    static final ClassLoaderLocalLoader SYSTEM = new ClassLoaderLocalLoader(ClassLoaderLocalLoader.class.getClassLoader());

    private static final Method getPackage;

    private final ClassLoader classLoader;

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
            list.add(new URLResource(urls.nextElement()));
        }
        return list;
    }
}
