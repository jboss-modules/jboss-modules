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

import java.util.Collections;
import java.util.Set;

import org.jboss.modules.filter.ClassFilter;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

/**
 * A local dependency specification builder, which includes a module's own content or some other, external content.
 */
public final class LocalDependencySpecBuilder extends DependencySpecBuilder {
    private LocalLoader localLoader;
    private Set<String> loaderPaths = Collections.emptySet();

    /**
     * Construct a new instance.
     */
    public LocalDependencySpecBuilder() {
        // different default import filter
        setImportFilter(PathFilters.acceptAll());
    }

    // covariant overrides

    /**
     * Get the import filter to use.  The default value is {@link PathFilters#acceptAll()}.
     *
     * @return the import filter to use
     */
    public PathFilter getImportFilter() {
        return super.getImportFilter();
    }

    public LocalDependencySpecBuilder setImportFilter(final PathFilter importFilter) {
        super.setImportFilter(importFilter);
        return this;
    }

    public LocalDependencySpecBuilder setImportServices(final boolean services) {
        super.setImportServices(services);
        return this;
    }

    public LocalDependencySpecBuilder setExportFilter(final PathFilter exportFilter) {
        super.setExportFilter(exportFilter);
        return this;
    }

    public LocalDependencySpecBuilder setExport(final boolean export) {
        super.setExport(export);
        return this;
    }

    public LocalDependencySpecBuilder setResourceImportFilter(final PathFilter resourceImportFilter) {
        super.setResourceImportFilter(resourceImportFilter);
        return this;
    }

    public LocalDependencySpecBuilder setResourceExportFilter(final PathFilter resourceExportFilter) {
        super.setResourceExportFilter(resourceExportFilter);
        return this;
    }

    public LocalDependencySpecBuilder setClassImportFilter(final ClassFilter classImportFilter) {
        super.setClassImportFilter(classImportFilter);
        return this;
    }

    public LocalDependencySpecBuilder setClassExportFilter(final ClassFilter classExportFilter) {
        super.setClassExportFilter(classExportFilter);
        return this;
    }

    /**
     * Get the local loader to use.  The default value is {@code null}, indicating that the content should come from
     * the module being defined.
     *
     * @return the local loader to use, or {@code null} to use the module's own content
     */
    public LocalLoader getLocalLoader() {
        return localLoader;
    }

    /**
     * Set the local loader to use.
     *
     * @param localLoader the local loader to use, or {@code null} to use the module's own content
     * @return this builder
     */
    public LocalDependencySpecBuilder setLocalLoader(final LocalLoader localLoader) {
        if (localLoader == null) {
            throw new IllegalArgumentException("localLoader is null");
        }
        this.localLoader = localLoader;
        return this;
    }

    /**
     * Get the loader paths set.  The default is the empty set.  This value is ignored if the dependency specification
     * refers to the module's own content.
     *
     * @return the loader paths set
     */
    public Set<String> getLoaderPaths() {
        return loaderPaths;
    }

    /**
     * Set the loader paths set.
     *
     * @param loaderPaths the loader paths set (must not be {@code null})
     * @return this builder
     */
    public LocalDependencySpecBuilder setLoaderPaths(final Set<String> loaderPaths) {
        if (loaderPaths == null) {
            throw new IllegalArgumentException("loaderPaths is null");
        }
        this.loaderPaths = loaderPaths;
        return this;
    }

    public DependencySpec build() {
        final LocalLoader localLoader = this.localLoader;
        final Set<String> loaderPaths = this.loaderPaths;
        if (localLoader == null) {
            return new DependencySpec(importFilter, exportFilter, resourceImportFilter, resourceExportFilter, classImportFilter, classExportFilter) {
                Dependency getDependency(final Module module) {
                    return new ModuleClassLoaderDependency(getExportFilter(), getImportFilter(), getResourceExportFilter(), getResourceImportFilter(), getClassExportFilter(), getClassImportFilter(), module.getClassLoaderPrivate());
                }

                public String toString() {
                    return "dependency on local resources";
                }
            };
        } else {
            return new DependencySpec(importFilter, exportFilter, resourceImportFilter, resourceExportFilter, classImportFilter, classExportFilter) {
                Dependency getDependency(final Module module) {
                    return new LocalDependency(getExportFilter(), getImportFilter(), getResourceExportFilter(), getResourceImportFilter(), getClassExportFilter(), getClassImportFilter(), localLoader, loaderPaths);
                }

                public String toString() {
                    return "dependency on local loader " + localLoader;
                }
            };
        }
    }
}
