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
 * Static factory methods for various types of local loaders.
 *
 * @apiviz.exclude
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LocalLoaders {

    private LocalLoaders() {
    }

    /**
     * Create a filtered local loader.
     *
     * @param pathFilter the path filter to apply to resources
     * @param originalLoader the original loader
     * @return the filtered loader
     */
    public static LocalLoader createPathFilteredLocalLoader(final PathFilter pathFilter, final LocalLoader originalLoader) {
        return new FilteredLocalLoader(ClassFilters.acceptAll(), pathFilter, originalLoader);
    }

    /**
     * Create a filtered local loader.
     *
     * @param pathFilter the path filter to apply to resources
     * @param originalLoader the original loader
     * @return the filtered loader
     */
    public static IterableLocalLoader createIterablePathFilteredLocalLoader(final PathFilter pathFilter, final IterableLocalLoader originalLoader) {
        return new FilteredIterableLocalLoader(ClassFilters.acceptAll(), pathFilter, originalLoader);
    }

    /**
     * Create a filtered local loader.
     *
     * @param classFilter the class filter to apply to classes
     * @param originalLoader the original loader
     * @return the filtered loader
     */
    public static LocalLoader createClassFilteredLocalLoader(final ClassFilter classFilter, final LocalLoader originalLoader) {
        return new FilteredLocalLoader(classFilter, PathFilters.acceptAll(), originalLoader);
    }

    /**
     * Create a filtered local loader.
     *
     * @param classFilter the class filter to apply to classes
     * @param originalLoader the original loader
     * @return the filtered loader
     */
    public static IterableLocalLoader createIterableClassFilteredLocalLoader(final ClassFilter classFilter, final IterableLocalLoader originalLoader) {
        return new FilteredIterableLocalLoader(classFilter, PathFilters.acceptAll(), originalLoader);
    }

    /**
     * Create a filtered local loader.
     *
     * @param classFilter the class filter to apply to classes
     * @param resourcePathFilter the path filter to apply to resources
     * @param originalLoader the original loader
     * @return the filtered loader
     */
    public static LocalLoader createFilteredLocalLoader(final ClassFilter classFilter, final PathFilter resourcePathFilter, final LocalLoader originalLoader) {
        return new FilteredLocalLoader(classFilter, resourcePathFilter, originalLoader);
    }

    /**
     * Create a filtered local loader.
     *
     * @param classFilter the class filter to apply to classes
     * @param resourcePathFilter the path filter to apply to resources
     * @param originalLoader the original loader
     * @return the filtered loader
     */
    public static IterableLocalLoader createIterableFilteredLocalLoader(final ClassFilter classFilter, final PathFilter resourcePathFilter, final IterableLocalLoader originalLoader) {
        return new FilteredIterableLocalLoader(classFilter, resourcePathFilter, originalLoader);
    }
}
