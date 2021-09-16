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
import org.jboss.modules.filter.ClassFilters;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

/**
 * A dependency item.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
abstract class Dependency {

    private final PathFilter exportFilter;
    private final PathFilter importFilter;
    private final PathFilter resourceExportFilter;
    private final PathFilter resourceImportFilter;
    private final ClassFilter classExportFilter;
    private final ClassFilter classImportFilter;

    Dependency(final PathFilter exportFilter, final PathFilter importFilter) {
        this(exportFilter, importFilter, PathFilters.acceptAll(), PathFilters.acceptAll(), ClassFilters.acceptAll(), ClassFilters.acceptAll());
    }

    protected Dependency(final PathFilter exportFilter, final PathFilter importFilter, final PathFilter resourceExportFilter, final PathFilter resourceImportFilter, final ClassFilter classExportFilter, final ClassFilter classImportFilter) {
        if (exportFilter == null) {
            throw new IllegalArgumentException("exportFilter is null");
        }
        if (importFilter == null) {
            throw new IllegalArgumentException("importFilter is null");
        }
        if (resourceExportFilter == null) {
            throw new IllegalArgumentException("resourceExportFilter is null");
        }
        if (resourceImportFilter == null) {
            throw new IllegalArgumentException("resourceImportFilter is null");
        }
        if (classExportFilter == null) {
            throw new IllegalArgumentException("classExportFilter is null");
        }
        if (classImportFilter == null) {
            throw new IllegalArgumentException("classImportFilter is null");
        }
        this.exportFilter = exportFilter;
        this.importFilter = importFilter;
        this.resourceExportFilter = resourceExportFilter;
        this.resourceImportFilter = resourceImportFilter;
        this.classExportFilter = classExportFilter;
        this.classImportFilter = classImportFilter;
    }

    /**
     * Get the export filter for this dependency.  This filter determines what imported paths are re-exported by this
     * dependency.  All exported paths must also satisfy the import filter.
     *
     * @return the export filter
     */
    final PathFilter getExportFilter() {
        return exportFilter;
    }

    /**
     * Get the import filter for this dependency.  This filter determines what exported paths are imported from the
     * dependency to the dependent.
     *
     * @return the import filter
     */
    final PathFilter getImportFilter() {
        return importFilter;
    }

    final PathFilter getResourceExportFilter() {
        return resourceExportFilter;
    }

    final PathFilter getResourceImportFilter() {
        return resourceImportFilter;
    }

    final ClassFilter getClassExportFilter() {
        return classExportFilter;
    }

    final ClassFilter getClassImportFilter() {
        return classImportFilter;
    }
}
