/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
