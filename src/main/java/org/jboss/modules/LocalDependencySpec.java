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

import java.util.Set;

/**
 * A local dependency specification.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LocalDependencySpec extends AbstractDependencySpec {

    private final LocalLoader localLoader;
    private final Set<String> loaderPaths;

    private LocalDependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final LocalLoader localLoader, final Set<String> loaderPaths) {
        super(importFilter, exportFilter);
        this.localLoader = localLoader;
        this.loaderPaths = loaderPaths;
    }

    /**
     * Get the local loader for this specification.
     *
     * @return the local loader
     */
    public final LocalLoader getLocalLoader() {
        return localLoader;
    }

    /**
     * Get the loader paths for this specification.
     *
     * @return the loader paths
     */
    public final Set<String> getLoaderPaths() {
        return loaderPaths;
    }

    /**
     * A builder for local dependency specifications.
     */
    public interface Builder {
        /**
         * Set the import filter for this dependency.  Defaults to {@link PathFilter#ACCEPT_ALL}.
         *
         * @param importFilter the import filter
         * @return this builder
         */
        Builder setImportFilter(PathFilter importFilter);

        /**
         * Set the export filter for this dependency.  Defaults to {@link PathFilter#REJECT_ALL}.
         *
         * @param exportFilter the export filter
         * @return this builder
         */
        Builder setExportFilter(PathFilter exportFilter);

        /**
         * Create an instance of the dependency specification from this builder's current state.
         *
         * @return the specification
         */
        LocalDependencySpec create();
    }

    /**
     * Get a builder for a new local dependency specification.
     *
     * @param localLoader the local loader
     * @param loaderPaths the loader paths
     * @return the new builder
     */
    public static Builder build(final LocalLoader localLoader, final Set<String> loaderPaths) {
        return new Builder() {
            private PathFilter importFilter;
            private PathFilter exportFilter;

            public Builder setImportFilter(final PathFilter importFilter) {
                this.importFilter = importFilter;
                return this;
            }

            public Builder setExportFilter(final PathFilter exportFilter) {
                this.exportFilter = exportFilter;
                return this;
            }

            public LocalDependencySpec create() {
                return new LocalDependencySpec(importFilter, exportFilter, localLoader, loaderPaths);
            }
        };
    }
}
