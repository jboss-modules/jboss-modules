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

import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

/**
 * A specification of a resource loader within a module.  A resource loader may optionally be associated with a
 * path filter which can be used to decide which paths of a resource loader to include.
 *
 * @apiviz.exclude
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ResourceLoaderSpec {
    private final ResourceLoader resourceLoader;
    private final PathFilter pathFilter;

    ResourceLoaderSpec(final ResourceLoader resourceLoader, final PathFilter pathFilter) {
        this.resourceLoader = resourceLoader;
        this.pathFilter = pathFilter;
    }

    /**
     * Construct a new instance.
     *
     * @param resourceLoader the resource loader to include
     * @param pathFilter the path filter to apply to the resource loader's paths
     * @return the specification
     */
    public static ResourceLoaderSpec createResourceLoaderSpec(final ResourceLoader resourceLoader, final PathFilter pathFilter) {
        return new ResourceLoaderSpec(resourceLoader, pathFilter);
    }

    /**
     * Construct a new instance which accepts all paths in the resource loader.
     *
     * @param resourceLoader the resource loader to include
     * @return the specification
     */
    public static ResourceLoaderSpec createResourceLoaderSpec(final ResourceLoader resourceLoader) {
        return new ResourceLoaderSpec(resourceLoader, PathFilters.acceptAll());
    }

    ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    PathFilter getPathFilter() {
        return pathFilter;
    }
}
