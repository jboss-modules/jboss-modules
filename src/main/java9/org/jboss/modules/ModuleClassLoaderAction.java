/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.lang.Module;
import java.security.PrivilegedAction;

/**
 */
final class ModuleClassLoaderAction implements PrivilegedAction<ClassLoader> {
    static final ClassLoader SAFE_CL;

    static {
        ClassLoader safeClassLoader = ModuleClassLoaderAction.class.getClassLoader();
        if (safeClassLoader == null) {
            safeClassLoader = ClassLoader.getSystemClassLoader();
        }
        if (safeClassLoader == null) {
            safeClassLoader = new ClassLoader() {
            };
        }
        SAFE_CL = safeClassLoader;
    }

    private final Module module;

    ModuleClassLoaderAction(final Module module) {
        this.module = module;
    }

    public ClassLoader run() {
        final ClassLoader classLoader = module.getClassLoader();
        return classLoader == null ? SAFE_CL : classLoader;
    }
}
