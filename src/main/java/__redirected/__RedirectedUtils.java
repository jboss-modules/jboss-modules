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

package __redirected;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.log.ModuleLogger;

/**
 * Common utilities for redirected factories
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @authore Jason T. Greene
 */
public final class __RedirectedUtils {

    static ModuleLogger getModuleLogger() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ModuleLogger>() {
                public ModuleLogger run() {
                    return Module.getModuleLogger();
                }
            });
        } else {
            return Module.getModuleLogger();
        }
    }

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
            getModuleLogger().providerUnloadable(id.toString(), null);
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
            getModuleLogger().providerUnloadable("Not found", classLoader);
            return null;
        }

        String clazzName = names.get(0);
        try {
            return classLoader.loadClass(clazzName).asSubclass(intf);
        } catch (Exception ignore) {
            getModuleLogger().providerUnloadable(clazzName, classLoader);
            return null;
        }
    }

    static <T> List<Class<? extends T>> loadProviders(Class<T> intf, ClassLoader classLoader) {
        return loadProviders(intf, classLoader, null);
    }

    static <T> List<Class<? extends T>> loadProviders(Class<T> intf, ClassLoader classLoader, String name) {
        List<String> names = findProviderClassNames(intf, classLoader, name);

        if (names.size() < 1) {
            getModuleLogger().providerUnloadable("Not found", classLoader);
            return Collections.emptyList();
        }

        List<Class<? extends T>> classes = new ArrayList<Class<? extends T>>();

        for (String className : names) {
            try {
                classes.add(classLoader.loadClass(className).asSubclass(intf));
            } catch (Exception ignore) {
                getModuleLogger().providerUnloadable(className, classLoader);
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
