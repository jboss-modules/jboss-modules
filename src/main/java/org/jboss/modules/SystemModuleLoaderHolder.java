/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

final class SystemModuleLoaderHolder {

    static final ModuleLoader INSTANCE;

    private SystemModuleLoaderHolder() {
    }

    static {
        INSTANCE = AccessController.doPrivileged(new ModuleLoaderLookupAction());
    }

    private static class ModuleLoaderLookupAction implements PrivilegedAction<ModuleLoader> {

        private static final String SERVICE_DEFINITION_PATH = "META-INF/services/org.jboss.modules.ModuleLoader";
        private static final String SYSTEM_MODULE_LOADER_KEY = "system.module.loader";

        public ModuleLoader run() {
            ModuleLoader moduleLoader = lookupUsingSystemProperty();
            if (moduleLoader == null) moduleLoader = lookupUsingServiceDefinition();
            if (moduleLoader == null) moduleLoader = new LocalModuleLoader();

            return moduleLoader;
        }

        private ModuleLoader lookupUsingSystemProperty() {
            final String loaderClass = System.getProperty(SYSTEM_MODULE_LOADER_KEY);
            if (loaderClass == null) return null;

            return instantiateModuleLoader(loaderClass);
        }

        private ModuleLoader lookupUsingServiceDefinition() {
            final String loaderClass = readModuleLoaderClass(SERVICE_DEFINITION_PATH);
            if (loaderClass == null) return null;
            System.setProperty(SYSTEM_MODULE_LOADER_KEY, loaderClass);

            return instantiateModuleLoader(loaderClass);
        }

        private String readModuleLoaderClass(final String serviceDefintionPath) {
            BufferedReader serviceDefintionReader = null;
            try {
                final InputStream serviceDefinitionStream = SystemModuleLoaderHolder.class.getResourceAsStream(serviceDefintionPath);
                if (serviceDefinitionStream == null) return null;

                serviceDefintionReader = new BufferedReader(new InputStreamReader(serviceDefinitionStream));
                String name = null;
                String line;
                while ((line = serviceDefintionReader.readLine()) != null) {
                    final int i = line.indexOf('#');
                    if (i != -1) {
                        line = line.substring(0, i);
                    }
                    line = line.trim();
                    if (line.length() == 0) continue;
                    name = line;
                    break;
                }

                return name;
            } catch (IOException e) {
                throw new RuntimeException("Failed to read ModuleLoader implementation class from [" + serviceDefintionPath + "]: " + e.getMessage(), e);
            } finally {
                if (serviceDefintionReader != null) {
                    try {
                        serviceDefintionReader.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }

        }

        private ModuleLoader instantiateModuleLoader(String loaderClass) {
            try {
                return Class.forName(loaderClass, true, SystemModuleLoaderHolder.class.getClassLoader()).asSubclass(ModuleLoader.class).getConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new InstantiationError(e.getMessage());
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            } catch (InvocationTargetException e) {
                try {
                    throw e.getCause();
                } catch (RuntimeException cause) {
                    throw cause;
                } catch (Error cause) {
                    throw cause;
                } catch (Throwable t) {
                    throw new Error(t);
                }
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError(e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new NoClassDefFoundError(e.getMessage());
            }
        }
    }
}
