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

import org.jboss.modules.filter.ClassFilter;
import org.jboss.modules.filter.PathFilter;

/**
 * A dependency specification on a module.
 */
public final class ModuleDependencySpec extends DependencySpec {

    private final ModuleLoader moduleLoader;
    private final String name;
    private final boolean optional;

    ModuleDependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final PathFilter resourceImportFilter, final PathFilter resourceExportFilter, final ClassFilter classImportFilter, final ClassFilter classExportFilter, final ModuleLoader moduleLoader, final String name, final boolean optional) {
        super(importFilter, exportFilter, resourceImportFilter, resourceExportFilter, classImportFilter, classExportFilter);
        this.moduleLoader = moduleLoader;
        this.name = name;
        this.optional = optional;
    }

    Dependency getDependency(final Module module) {
        final ModuleLoader loader = moduleLoader;
        return new ModuleDependency(exportFilter, importFilter, resourceExportFilter, resourceImportFilter, classExportFilter, classImportFilter, loader == null ? module.getModuleLoader() : loader, name, optional);
    }

    /**
     * Get the module loader of this dependency, or {@code null} if the defined module's loader is to be used.
     *
     * @return the module loader
     */
    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    /**
     * Get the module identifier of the dependency.
     *
     * @return the module identifier
     * @deprecated Use {@code #getName()} instead.
     */
    @Deprecated
    public ModuleIdentifier getIdentifier() {
        return ModuleIdentifier.fromString(name);
    }

    /**
     * Get the module name of the dependency.
     *
     * @return the module name
     */
    public String getName() {
        return name;
    }

    /**
     * Determine whether this dependency is optional.
     *
     * @return {@code true} if the dependency is optional, {@code false} if it is required
     */
    public boolean isOptional() {
        return optional;
    }

    public String toString() {
        return "dependency on " + name;
    }
}
