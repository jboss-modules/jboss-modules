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
     * @param resourcePathFilter the path filter to apply to resources
     * @param originalLoader the original loader
     * @return the filtered loader
     */
    public static LocalLoader createFilteredLocalLoader(final ClassFilter classFilter, final PathFilter resourcePathFilter, final LocalLoader originalLoader) {
        return new FilteredLocalLoader(classFilter, resourcePathFilter, originalLoader);
    }
}
