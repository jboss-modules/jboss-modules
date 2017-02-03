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
    static final ResourceLoaderSpec[] NO_RESOURCE_LOADERS = new ResourceLoaderSpec[0];
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
