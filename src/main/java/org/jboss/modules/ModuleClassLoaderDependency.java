/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.Set;
import org.jboss.modules.filter.ClassFilter;
import org.jboss.modules.filter.PathFilter;

/**
* @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
*/
final class ModuleClassLoaderDependency extends Dependency {
    private final ModuleClassLoader moduleClassLoader;

    ModuleClassLoaderDependency(final PathFilter exportFilter, final PathFilter importFilter, final ModuleClassLoader moduleClassLoader) {
        super(exportFilter, importFilter);
        this.moduleClassLoader = moduleClassLoader;
    }

    ModuleClassLoaderDependency(final PathFilter exportFilter, final PathFilter importFilter, final PathFilter resourceExportFilter, final PathFilter resourceImportFilter, final ClassFilter classExportFilter, final ClassFilter classImportFilter, final ModuleClassLoader moduleClassLoader) {
        super(exportFilter, importFilter, resourceExportFilter, resourceImportFilter, classExportFilter, classImportFilter);
        this.moduleClassLoader = moduleClassLoader;
    }

    LocalLoader getLocalLoader() {
        return moduleClassLoader.getLocalLoader();
    }

    Set<String> getPaths() {
        return moduleClassLoader.getPaths();
    }

    public String toString() {
        return "dependency on " + moduleClassLoader;
    }
}
