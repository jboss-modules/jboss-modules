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

/**
 * A locator for module definitions.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ModuleFinder {

    /**
     * Find a module specification for the given name.  The default implementation delegates to the legacy
     * {@link #findModule(ModuleIdentifier, ModuleLoader)} method.
     *
     * @param name the module name
     * @param delegateLoader the module loader from which dependencies should be resolved
     * @return the module specification, or {@code null} if no specification is found for this identifier
     */
    default ModuleSpec findModule(String name, ModuleLoader delegateLoader) throws ModuleLoadException {
        return findModule(ModuleIdentifier.fromString(name), delegateLoader);
    }

    /**
     * Find a module specification for the given identifier.  The default implementation returns {@code null}.  This
     * method will never be called by the module system unless {@link #findModule(String, ModuleLoader)} is left
     * unimplemented.
     *
     * @param moduleIdentifier the module identifier
     * @param delegateLoader the module loader from which dependencies should be resolved
     * @return the module specification, or {@code null} if no specification is found for this identifier
     */
    default ModuleSpec findModule(ModuleIdentifier moduleIdentifier, ModuleLoader delegateLoader) throws ModuleLoadException {
        return null;
    }
}
