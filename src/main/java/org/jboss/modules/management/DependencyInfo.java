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

package org.jboss.modules.management;

import java.beans.ConstructorProperties;
import java.util.List;

/**
 * Information describing a dependency.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DependencyInfo {
    private final String dependencyType;
    private final String exportFilter;
    private final String importFilter;
    private final ModuleLoaderMXBean moduleLoader;
    private final String moduleName;
    private final boolean optional;
    private final String localLoader;
    private final List<String> localLoaderPaths;

    /**
     * Construct a new instance.
     *
     * @param dependencyType the dependency type class name
     * @param exportFilter the export filter, as a string
     * @param importFilter the import filter, as a string
     * @param moduleLoader the module loader MXBean of this dependency
     * @param moduleName the module name, as a string
     * @param optional {@code true} if this is an optional dependency
     * @param localLoader the local loader type class name
     * @param localLoaderPaths the list of paths made available by the local loader
     */
    @ConstructorProperties({"dependencyType", "exportFilter", "importFilter", "moduleLoader", "moduleName", "optional", "localLoader", "localLoaderPaths"})
    public DependencyInfo(final String dependencyType, final String exportFilter, final String importFilter, final ModuleLoaderMXBean moduleLoader, final String moduleName, final boolean optional, final String localLoader, final List<String> localLoaderPaths) {
        this.dependencyType = dependencyType;
        this.exportFilter = exportFilter;
        this.importFilter = importFilter;
        this.moduleLoader = moduleLoader;
        this.moduleName = moduleName;
        this.optional = optional;
        this.localLoader = localLoader;
        this.localLoaderPaths = localLoaderPaths;
    }

    /**
     * Get the dependency type class name.
     *
     * @return the dependency type class name
     */
    public String getDependencyType() {
        return dependencyType;
    }

    /**
     * Get the export filter, as a string.
     *
     * @return the export filter, as a string
     */
    public String getExportFilter() {
        return exportFilter;
    }

    /**
     * Get the import filter, as a string.
     *
     * @return the import filter, as a string
     */
    public String getImportFilter() {
        return importFilter;
    }

    /**
     * Get the module loader MXBean of this dependency.
     *
     * @return the module loader MXBean of this dependency
     */
    public ModuleLoaderMXBean getModuleLoader() {
        return moduleLoader;
    }

    /**
     * Get the module name, as a string.
     *
     * @return the module name, as a string
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Determine whether this is an optional dependency.
     *
     * @return {@code true} if this is an optional dependency
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Get the local loader type class name.
     *
     * @return the local loader type class name
     */
    public String getLocalLoader() {
        return localLoader;
    }

    /**
     * Get the list of paths made available by the local loader.
     *
     * @return the list of paths made available by the local loader
     */
    public List<String> getLocalLoaderPaths() {
        return localLoaderPaths;
    }
}
