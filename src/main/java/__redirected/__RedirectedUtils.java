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
        Module module;
        try {
            module = moduleLoader.loadModule(id);
        } catch (ModuleLoadException e) {
            Module.getModuleLogger().providerUnloadable(id.toString(), null);
            return null;
        }

        ModuleClassLoader classLoader = module.getClassLoader();
        return loadProvider(intf, classLoader);
    }

     static <T> Class<? extends T> loadProvider(Class<T> intf, ClassLoader classLoader) {
        String name = findProviderClassName(intf, classLoader);

        if (name == null) {
            Module.getModuleLogger().providerUnloadable(name, classLoader);
            return null;
        }

        Class<? extends T> clazz = null;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> t = (Class<? extends T>) classLoader.loadClass(name);
            clazz = t;
        } catch (ClassNotFoundException ignore) {
        }

        if (clazz == null || !intf.isAssignableFrom(clazz)) {
            Module.getModuleLogger().providerUnloadable(name, classLoader);
            return null;
        }

        return clazz;
    }

     static <T> String findProviderClassName(Class<T> intf, ClassLoader loader) {
        final InputStream stream = loader.getResourceAsStream("META-INF/services/" + intf.getName());
        if (stream == null)
            return null;

        String name = null;
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
                name = line;
                break;
            }
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
        return name;
    }

    private static String readLine(final BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException ignore) {
            return null;
        }
    }

}
