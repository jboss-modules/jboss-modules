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

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

final class DefaultBootModuleLoaderHolder {

    static final ModuleLoader INSTANCE;

    private DefaultBootModuleLoaderHolder() {
    }

    static {
        INSTANCE = AccessController.doPrivileged(new PrivilegedAction<ModuleLoader>() {
            public ModuleLoader run() {
                final String loaderClass = System.getProperty("boot.module.loader", LocalModuleLoader.class.getName());
                try {
                    return Class.forName(loaderClass, true, DefaultBootModuleLoaderHolder.class.getClassLoader()).asSubclass(ModuleLoader.class).getConstructor().newInstance();
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
