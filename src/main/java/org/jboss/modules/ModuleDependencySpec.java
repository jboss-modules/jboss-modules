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

/**
 * A specification for a module dependency.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleDependencySpec extends AbstractDependencySpec {
    private final ModuleIdentifier moduleIdentifier;
    private final boolean optional;

    private ModuleDependencySpec(final ModuleIdentifier moduleIdentifier, final boolean optional, final PathFilter importFilter, final PathFilter exportFilter) {
        super(importFilter, exportFilter);
        this.moduleIdentifier = moduleIdentifier;
        this.optional = optional;
    }

    /**
     * Get the module identifier specified for this dependency item.
     *
     * @return the module identifier
     */
    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    /**
     * Determine whether the dependency is optional.
     *
     * @return {@code true} if the dependency is optional, {@code false} otherwise
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * A module dependency specification builder.
     */
    public interface Builder {

        /**
         * Set the import filter for this dependency.  Defaults to {@link PathFilters#acceptAll()}.
         *
         * @param importFilter the import filter
         * @return this builder
         */
        Builder setImportFilter(PathFilter importFilter);

        /**
         * Set the export filter for this dependency.  Defaults to {@link PathFilters#rejectAll()}.
         *
         * @param exportFilter the export filter
         * @return this builder
         */
        Builder setExportFilter(PathFilter exportFilter);

        /**
         * Set whether the built dependency should be optional.  Defaults to {@code false}.
         *
         * @param optional {@code true} if the dependency should be optional
         * @return this builder
         */
        Builder setOptional(boolean optional);

        /**
         * Create the dependency specification from this builder's current state.
         *
         * @return the specification
         */
        ModuleDependencySpec create();
    }

    /**
     * Get a module dependency specification builder for a specific module.
     *
     * @param moduleIdentifier the module identifier
     * @return the new builder
     */
    public static Builder build(final ModuleIdentifier moduleIdentifier) {
        return new Builder() {
            private boolean optional = false;
            private PathFilter importFilter = PathFilters.acceptAll();
            private PathFilter exportFilter = PathFilters.rejectAll();

            public Builder setOptional(final boolean optional) {
                this.optional = optional;
                return this;
            }

            public Builder setImportFilter(final PathFilter importFilter) {
                this.importFilter = importFilter;
                return this;
            }

            public Builder setExportFilter(final PathFilter exportFilter) {
                this.exportFilter = exportFilter;
                return this;
            }

            public ModuleDependencySpec create() {
                return new ModuleDependencySpec(moduleIdentifier, optional, importFilter, exportFilter);
            }
        };
    }
}
