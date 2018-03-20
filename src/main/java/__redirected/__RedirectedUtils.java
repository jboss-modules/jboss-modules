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
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.log.ModuleLogger;

/**
 * Common utilities for redirected factories
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jason T. Greene
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

    static <T> Supplier<T> loadProvider(String id, Class<T> intf, ModuleLoader moduleLoader) {
        return loadProvider(id, intf, moduleLoader, null);
    }

    static <T> Supplier<T> loadProvider(String id, Class<T> intf, ModuleLoader moduleLoader, String name) {
        Module module;
        try {
            module = moduleLoader.loadModule(id);
        } catch (ModuleLoadException e) {
            getModuleLogger().providerUnloadable(id, null);
            return null;
        }

        ModuleClassLoader classLoader = module.getClassLoader();
        return loadProvider(intf, classLoader, name);
    }

    static <T> Supplier<T> loadProvider(Class<T> intf, ClassLoader classLoader) {
        return loadProvider(intf, classLoader, null);
    }

    static <T> Supplier<T> loadProvider(Class<T> intf, ClassLoader classLoader, String name) {
        List<String> names = findProviderClassNames(intf, classLoader, name);

        if (names.isEmpty()) {
            getModuleLogger().providerUnloadable("Not found", classLoader);
            return null;
        }

        String clazzName = names.get(0);
        try {
            return new ConstructorSupplier<>(classLoader.loadClass(clazzName).asSubclass(intf).getConstructor());
        } catch (Exception ignore) {
            getModuleLogger().providerUnloadable(clazzName, classLoader);
            return null;
        }
    }

    static <T> List<Supplier<T>> loadProviders(Class<T> intf, ClassLoader classLoader) {
        return loadProviders(intf, classLoader, null);
    }

    static <T> List<Supplier<T>> loadProviders(Class<T> intf, ClassLoader classLoader, String name) {
        List<String> names = findProviderClassNames(intf, classLoader, name);

        if (names.size() < 1) {
            getModuleLogger().providerUnloadable("Not found", classLoader);
            return Collections.emptyList();
        }

        List<Supplier<T>> suppliers = new ArrayList<>();

        for (String className : names) {
            try {
                suppliers.add(new ConstructorSupplier<>(classLoader.loadClass(className).asSubclass(intf).getConstructor()));
            } catch (Exception ignore) {
                getModuleLogger().providerUnloadable(className, classLoader);
            }
        }

        return suppliers;
    }

    static <T> List<String> findProviderClassNames(Class<T> intf, ClassLoader loader, String name) {
        if (name == null)
            name = intf.getName();

        try {
            final Enumeration<URL> resources = loader.getResources("META-INF/services/" + name);
            if (resources.hasMoreElements()) {
                List<String> list = new ArrayList<String>();
                do {
                    final URL url = resources.nextElement();
                    final URLConnection connection = url.openConnection();
                    try (InputStream is = connection.getInputStream()) {
                        try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                            try (BufferedReader reader = new BufferedReader(isr)) {
                                String line;
                                while ((line = readLine(reader)) != null) {
                                    final int i = line.indexOf('#');
                                    if (i != -1) {
                                        line = line.substring(0, i);
                                    }
                                    line = line.trim();
                                    if (line.length() == 0)
                                        continue;
                                    if (line.startsWith("__redirected"))
                                        continue;

                                    list.add(line);
                                }
                            }
                        }
                    } catch (IOException ignored) {
                        // go on to next item
                    }
                } while (resources.hasMoreElements());
                return list;
            }
        } catch (IOException ignored) {
        }
        return Collections.emptyList();
    }

    private static String readLine(final BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException ignore) {
            return null;
        }
    }

}
