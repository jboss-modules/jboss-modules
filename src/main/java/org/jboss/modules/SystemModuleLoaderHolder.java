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

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

final class SystemModuleLoaderHolder {

    static final ModuleLoader INSTANCE;

    private SystemModuleLoaderHolder() {
    }

    static {
        INSTANCE = AccessController.doPrivileged(new PrivilegedAction<ModuleLoader>() {
            public ModuleLoader run() {
                final String loaderClass = System.getProperty("system.module.loader", LocalModuleLoader.class.getName());
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
        });
    }
}
