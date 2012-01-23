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

package __redirected;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * Common utilities for redirected factories
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @authore Jason T. Greene
 */
public final class __RedirectedUtils {

    static RuntimeException rethrowCause(Throwable t) throws Error {
        try {
            throw t.getCause();
        } catch (Error e) {
            throw e;
        } catch (RuntimeException re) {
            return re;
        } catch (Throwable throwable) {
            return new UndeclaredThrowableException(throwable);
        }
    }

    static <E extends Throwable> E wrapped(E e, Throwable orig) {
        Throwable cause = orig.getCause();
        if (cause != null) {
            e.initCause(cause);
        }
        e.setStackTrace(orig.getStackTrace());
        return e;
    }

    static <T> Class<? extends T> loadProvider(ModuleIdentifier id, Class<T> intf, ModuleLoader moduleLoader) {
        return loadProvider(id, intf, moduleLoader, null);
    }

    static <T> Class<? extends T> loadProvider(ModuleIdentifier id, Class<T> intf, ModuleLoader moduleLoader, String name) {
        Module module;
        try {
            module = moduleLoader.loadModule(id);
        } catch (ModuleLoadException e) {
            Module.getModuleLogger().providerUnloadable(id.toString(), null);
            return null;
        }

        ModuleClassLoader classLoader = module.getClassLoader();
        return loadProvider(intf, classLoader, name);
    }

    static <T> Class<? extends T> loadProvider(Class<T> intf, ClassLoader classLoader) {
        return loadProvider(intf, classLoader, null);
    }

    static <T> Class<? extends T> loadProvider(Class<T> intf, ClassLoader classLoader, String name) {
        List<String> names = findProviderClassNames(intf, classLoader, name);

        if (names.isEmpty()) {
            Module.getModuleLogger().providerUnloadable("Not found", classLoader);
            return null;
        }

        String clazzName = names.get(0);
        try {
            return classLoader.loadClass(clazzName).asSubclass(intf);
        } catch (Exception ignore) {
            Module.getModuleLogger().providerUnloadable(clazzName, classLoader);
            return null;
        }
    }

    static <T> List<Class<? extends T>> loadProviders(Class<T> intf, ClassLoader classLoader) {
        return loadProviders(intf, classLoader, null);
    }

    static <T> List<Class<? extends T>> loadProviders(Class<T> intf, ClassLoader classLoader, String name) {
        List<String> names = findProviderClassNames(intf, classLoader, name);

        if (names.size() < 1) {
            Module.getModuleLogger().providerUnloadable("Not found", classLoader);
            return Collections.emptyList();
        }

        List<Class<? extends T>> classes = new ArrayList<Class<? extends T>>();

        for (String className : names) {
            try {
                classes.add(classLoader.loadClass(className).asSubclass(intf));
            } catch (Exception ignore) {
                Module.getModuleLogger().providerUnloadable(className, classLoader);
            }
        }

        return classes;
    }

    static <T> List<String> findProviderClassNames(Class<T> intf, ClassLoader loader, String name) {
        if (name == null)
            name = intf.getName();

        final InputStream stream = loader.getResourceAsStream("META-INF/services/" + name);
        if (stream == null)
            return Collections.emptyList();


        List<String> list = new ArrayList<String>();
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            String line;
            while ((line = readLine(reader)) != null) {
                final int i = line.indexOf('#');
                if (i != -1) {
                    line = line.substring(0, i);
                }
                line = line.trim();
                if (line.length() == 0)
                    continue;

                list.add(line);
            }
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
        return list;
    }

    private static String readLine(final BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException ignore) {
            return null;
        }
    }

}
