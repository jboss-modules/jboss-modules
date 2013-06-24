/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import org.jboss.modules.filter.PathFilter;

/**
 * A dependency specification on a module.
 */
public final class ModuleDependencySpec extends DependencySpec {

    private final ModuleLoader moduleLoader;
    private final ModuleIdentifier identifier;
    private final boolean optional;

    ModuleDependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final PathFilter resourceImportFilter, final PathFilter resourceExportFilter, final ClassFilter classImportFilter, final ClassFilter classExportFilter, final ModuleLoader moduleLoader, final ModuleIdentifier identifier, final boolean optional) {
        super(importFilter, exportFilter, resourceImportFilter, resourceExportFilter, classImportFilter, classExportFilter);
        this.moduleLoader = moduleLoader;
        this.identifier = identifier;
        this.optional = optional;
    }

    Dependency getDependency(final Module module) {
        final ModuleLoader loader = moduleLoader;
        return new ModuleDependency(exportFilter, importFilter, resourceExportFilter, resourceImportFilter, classExportFilter, classImportFilter, loader == null ? module.getModuleLoader() : loader, identifier, optional);
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
     */
    public ModuleIdentifier getIdentifier() {
        return identifier;
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
        return "dependency on " + identifier;
    }
}
