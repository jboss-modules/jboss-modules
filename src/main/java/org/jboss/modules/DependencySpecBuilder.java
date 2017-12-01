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
import org.jboss.modules.filter.ClassFilters;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

/**
 * The base class of dependency specification builders.
 */
public abstract class DependencySpecBuilder {
    PathFilter importFilter = PathFilters.getDefaultImportFilter();
    PathFilter exportFilter = PathFilters.rejectAll();
    PathFilter resourceImportFilter = PathFilters.acceptAll();
    PathFilter resourceExportFilter = PathFilters.acceptAll();
    ClassFilter classImportFilter = ClassFilters.acceptAll();
    ClassFilter classExportFilter = ClassFilters.acceptAll();

    /**
     * Construct a new instance.
     */
    public DependencySpecBuilder() {
    }

    /**
     * Get the import filter to use.  The default value is {@link PathFilters#getDefaultImportFilter()}.
     *
     * @return the import filter to use
     */
    public PathFilter getImportFilter() {
        return importFilter;
    }

    /**
     * Set the import filter to use.
     *
     * @param importFilter the import filter to use (must not be {@code null})
     * @return this builder
     */
    public DependencySpecBuilder setImportFilter(final PathFilter importFilter) {
        if (importFilter == null) {
            throw new IllegalArgumentException("importFilter is null");
        }
        this.importFilter = importFilter;
        return this;
    }

    /**
     * Set a simple import filter, based on a {@code boolean} flag specifying whether services should be
     * imported.  If the flag is {@code true}, the import filter is set to {@link PathFilters#getDefaultImportFilterWithServices()},
     * otherwise it is set to {@link PathFilters#getDefaultImportFilter()}.  Any previous import filter setting is
     * overwritten.
     *
     * @param services the services flag
     * @return this builder
     */
    public DependencySpecBuilder setImportServices(final boolean services) {
        return setImportFilter(services ? PathFilters.getDefaultImportFilterWithServices() : PathFilters.getDefaultImportFilter());
    }

    /**
     * Get the export filter to use.  The default value is {@link PathFilters#rejectAll()}.
     *
     * @return the export filter to use
     */
    public PathFilter getExportFilter() {
        return exportFilter;
    }

    /**
     * Set the export filter to use.
     *
     * @param exportFilter the export filter to use (must not be {@code null})
     * @return this builder
     */
    public DependencySpecBuilder setExportFilter(final PathFilter exportFilter) {
        if (exportFilter == null) {
            throw new IllegalArgumentException("exportFilter is null");
        }
        this.exportFilter = exportFilter;
        return this;
    }

    /**
     * Set a simple export filter, based on a {@code boolean} flag.  If the flag is {@code true}, the
     * export filter is set to {@link PathFilters#acceptAll()}, otherwise it is set to {@link PathFilters#rejectAll()}.
     * Any previous export filter setting is overwritten.
     *
     * @param export the export flag
     * @return this builder
     */
    public DependencySpecBuilder setExport(final boolean export) {
        return setExportFilter(export ? PathFilters.acceptAll() : PathFilters.rejectAll());
    }

    /**
     * Get the resource import filter to use.  The default value is {@link PathFilters#acceptAll()}.
     *
     * @return the resource import filter to use
     */
    public PathFilter getResourceImportFilter() {
        return resourceImportFilter;
    }

    /**
     * Set the resource import filter to use.
     *
     * @param resourceImportFilter the resource import filter to use (must not be {@code null})
     * @return this builder
     */
    public DependencySpecBuilder setResourceImportFilter(final PathFilter resourceImportFilter) {
        if (resourceImportFilter == null) {
            throw new IllegalArgumentException("resourceImportFilter is null");
        }
        this.resourceImportFilter = resourceImportFilter;
        return this;
    }

    /**
     * Get the resource export filter to use.  The default value is {@link PathFilters#acceptAll()}.
     *
     * @return the resource export filter to use
     */
    public PathFilter getResourceExportFilter() {
        return resourceExportFilter;
    }

    /**
     * Set the resource export filter to use.  The default value is {@link PathFilters#acceptAll()}.
     *
     * @param resourceExportFilter the resource export filter to use (must not be {@code null})
     * @return this builder
     */
    public DependencySpecBuilder setResourceExportFilter(final PathFilter resourceExportFilter) {
        if (resourceExportFilter == null) {
            throw new IllegalArgumentException("resourceExportFilter is null");
        }
        this.resourceExportFilter = resourceExportFilter;
        return this;
    }

    /**
     * Get the class import filter to use.  The default value is {@link ClassFilters#acceptAll()}.
     *
     * @return the class import filter to use
     */
    public ClassFilter getClassImportFilter() {
        return classImportFilter;
    }

    /**
     * Set the class import filter to use.
     *
     * @param classImportFilter the class import filter to use (must not be {@code null})
     * @return this builder
     */
    public DependencySpecBuilder setClassImportFilter(final ClassFilter classImportFilter) {
        if (classImportFilter == null) {
            throw new IllegalArgumentException("classImportFilter is null");
        }
        this.classImportFilter = classImportFilter;
        return this;
    }

    /**
     * Get the class export filter to use.  The default value is {@link ClassFilters#acceptAll()}.
     *
     * @return the class export filter to use
     */
    public ClassFilter getClassExportFilter() {
        return classExportFilter;
    }

    /**
     * Set the class export filter to use.
     *
     * @param classExportFilter the class export filter to use (must not be {@code null})
     * @return this builder
     */
    public DependencySpecBuilder setClassExportFilter(final ClassFilter classExportFilter) {
        if (classExportFilter == null) {
            throw new IllegalArgumentException("classExportFilter is null");
        }
        this.classExportFilter = classExportFilter;
        return this;
    }

    /**
     * Construct the dependency specification.
     *
     * @return the dependency specification
     */
    public abstract DependencySpec build();
}
