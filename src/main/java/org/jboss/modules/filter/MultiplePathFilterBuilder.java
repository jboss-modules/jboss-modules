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

package org.jboss.modules.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * A builder for a multiple-path filter.
 *
 * @apiviz.exclude
 * @see PathFilters#multiplePathFilterBuilder(boolean)
 */
public class MultiplePathFilterBuilder {
    private final List<PathFilter> filters = new ArrayList<PathFilter>();
    private final List<Boolean> includeFlags = new ArrayList<Boolean>();
    private final boolean defaultVal;

    MultiplePathFilterBuilder(final boolean defaultVal) {
        this.defaultVal = defaultVal;
    }

    /**
     * Add a filter to this builder.
     *
     * @param filter the filter to add
     * @param include {@code true} if matching paths should be included, {@code false} for excluded
     */
    public void addFilter(final PathFilter filter, final boolean include) {
        if (filter == null) {
            throw new IllegalArgumentException("filter is null");
        }
        filters.add(filter);
        includeFlags.add(Boolean.valueOf(include));
    }

    /**
     * Create the path filter from this builder's current state.
     *
     * @return the path filter
     */
    public PathFilter create() {
        final PathFilter[] filters = this.filters.toArray(new PathFilter[this.filters.size()]);
        final boolean[] includeFlags = new boolean[this.includeFlags.size()];
        for (int i = 0, includeFlagsSize = this.includeFlags.size(); i < includeFlagsSize; i++) {
            includeFlags[i] = this.includeFlags.get(i).booleanValue();
        }
        if (filters.length == 0) {
            return defaultVal ? PathFilters.acceptAll() : PathFilters.rejectAll();
        } else {
            return new MultiplePathFilter(filters, includeFlags, defaultVal);
        }
    }

    /**
     * Determine if this filter builder is empty (i.e. has no path filters set on it).
     *
     * @return {@code true} if this builder is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return filters.isEmpty();
    }
}
