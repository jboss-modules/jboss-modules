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

import org.jboss.modules.filter.ClassFilter;
import org.jboss.modules.filter.PathFilter;

/**
 * A builder for dependency specifications that refer to other modules.
 */
public final class ModuleDependencySpecBuilder extends DependencySpecBuilder {

    ModuleLoader moduleLoader;
    String name;
    boolean optional;

    /**
     * Construct a new instance.
     */
    public ModuleDependencySpecBuilder() {
    }

    // covariant overrides

    public ModuleDependencySpecBuilder setImportFilter(final PathFilter importFilter) {
        super.setImportFilter(importFilter);
        return this;
    }

    public ModuleDependencySpecBuilder setImportServices(final boolean services) {
        super.setImportServices(services);
        return this;
    }

    public ModuleDependencySpecBuilder setExportFilter(final PathFilter exportFilter) {
        super.setExportFilter(exportFilter);
        return this;
    }

    public ModuleDependencySpecBuilder setExport(final boolean export) {
        super.setExport(export);
        return this;
    }

    public ModuleDependencySpecBuilder setResourceImportFilter(final PathFilter resourceImportFilter) {
        super.setResourceImportFilter(resourceImportFilter);
        return this;
    }

    public ModuleDependencySpecBuilder setResourceExportFilter(final PathFilter resourceExportFilter) {
        super.setResourceExportFilter(resourceExportFilter);
        return this;
    }

    public ModuleDependencySpecBuilder setClassImportFilter(final ClassFilter classImportFilter) {
        super.setClassImportFilter(classImportFilter);
        return this;
    }

    public ModuleDependencySpecBuilder setClassExportFilter(final ClassFilter classExportFilter) {
        super.setClassExportFilter(classExportFilter);
        return this;
    }

    /**
     * Get the module loader of the dependency.  Defaults to {@code null}, indicating that the module loader
     * to use should be the loader of the module being defined.
     *
     * @return the module loader of the dependency, or {@code null} to use the module's own module loader
     */
    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    /**
     * Set the module loader of the dependency.
     *
     * @param moduleLoader the module loader of the dependency, or {@code null} to use the module's own module loader
     * @return this builder
     */
    public ModuleDependencySpecBuilder setModuleLoader(final ModuleLoader moduleLoader) {
        this.moduleLoader = moduleLoader;
        return this;
    }

    /**
     * Get the module name.
     *
     * @return the module name, or {@code null} if was not yet set
     */
    public String getName() {
        return name;
    }

    /**
     * Set the module name.
     *
     * @param name the module name (must not be {@code null})
     * @return this builder
     */
    public ModuleDependencySpecBuilder setName(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        this.name = name;
        return this;
    }

    /**
     * Determine whether this dependency will be optional.  The default value is {@code false}.
     *
     * @return {@code true} if the dependency will be optional, {@code false} if it will be required
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Establish whether this dependency will be optional.
     *
     * @param optional {@code true} if the dependency will be optional, {@code false} if it will be required
     * @return this builder
     */
    public ModuleDependencySpecBuilder setOptional(final boolean optional) {
        this.optional = optional;
        return this;
    }

    public ModuleDependencySpec build() {
        final String name = this.name;
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        return new ModuleDependencySpec(importFilter, exportFilter, resourceImportFilter, resourceExportFilter, classImportFilter, classExportFilter, moduleLoader, name, optional);
    }
}
