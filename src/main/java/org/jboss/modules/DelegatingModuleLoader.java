/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

/**
 * A module loader which searches its finders first, and then delegates to another loader if the module is not found.
 */
public class DelegatingModuleLoader extends ModuleLoader {
    private final ModuleLoader delegate;

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate module loader, or {@code null} to skip delegation
     * @param finders the module finders (must not be {@code null})
     */
    public DelegatingModuleLoader(final ModuleLoader delegate, final ModuleFinder[] finders) {
        super(finders);
        this.delegate = delegate;
    }

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate module loader, or {@code null} to skip delegation
     * @param finder the single module finder (must not be {@code null})
     */
    public DelegatingModuleLoader(final ModuleLoader delegate, final ModuleFinder finder) {
        super(finder);
        this.delegate = delegate;
    }

    /**
     * Preload the named module.
     *
     * @param name the module name (must not be {@code null})
     * @return the loaded module, or {@code null} if it is not found in this loader or the delegate
     * @throws ModuleLoadException if the module was found but failed to be loaded
     */
    protected Module preloadModule(final String name) throws ModuleLoadException {
        Module module = loadModuleLocal(name);
        if (module == null) {
            final ModuleLoader delegate = this.delegate;
            if (delegate != null) {
                module = preloadModule(name, delegate);
            }
        }
        return module;
    }
}
