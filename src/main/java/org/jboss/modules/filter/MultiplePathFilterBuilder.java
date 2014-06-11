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
