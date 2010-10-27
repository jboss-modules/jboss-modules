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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Static factory methods for path filter types.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PathFilters {
    private PathFilters() {}

    /**
     * Get a path filter which returns {@code true} if all of the given filters return {@code true}.
     *
     * @param filters the filters
     * @return the "all" filter
     */
    public static PathFilter all(PathFilter... filters) {
        return new AggregatePathFilter(false, filters);
    }

    /**
     * Get a path filter which returns {@code true} if all of the given filters return {@code true}.
     *
     * @param filters the filters
     * @return the "all" filter
     */
    public static PathFilter all(Collection<PathFilter> filters) {
        return all(filters.toArray(new PathFilter[filters.size()]));
    }

    /**
     * Get a path filter which returns {@code true} if any of the given filters return {@code true}.
     *
     * @param filters the filters
     * @return the "any" filter
     */
    public static PathFilter any(PathFilter... filters) {
        return new AggregatePathFilter(true, filters);
    }

    /**
     * Get a path filter which returns {@code true} if any of the given filters return {@code true}.
     *
     * @param filters the filters
     * @return the "any" filter
     */
    public static PathFilter any(Collection<PathFilter> filters) {
        return any(filters.toArray(new PathFilter[filters.size()]));
    }

    /**
     * Get a path filter which is {@code true} when the given filter is {@code false}, and vice-versa.
     *
     * @param filter the filter
     * @return the inverting filter
     */
    public static PathFilter not(PathFilter filter) {
        return new InvertingPathFilter(filter);
    }

    /**
     * Get a path filter which matches a glob.  The given glob is a path separated
     * by "{@code /}" characters, which may include the special "{@code *}" and "{@code **}" segment strings
     * which match any directory and any number of nested directories, respectively.
     *
     * @param glob the glob
     * @return a filter which returns {@code true} if the glob matches
     */
    public static PathFilter match(String glob) {
        return new GlobPathFilter(glob);
    }

    /**
     * Get a builder for a multiple-path filter.  Such a filter contains multiple filters, each associated
     * with a flag which indicates that matching paths should be included or excluded.
     *
     * @param defaultValue the value to return if none of the nested filters match
     * @return the builder
     */
    public static MultiplePathFilterBuilder multiplePathFilterBuilder(boolean defaultValue) {
        return new MultiplePathFilterBuilder(defaultValue);
    }

    /**
     * Get a filter which always returns {@code true}.
     *
     * @return the accept-all filter
     */
    public static PathFilter acceptAll() {
        return BooleanPathFilter.TRUE;
    }

    /**
     * Get a filter which always returns {@code false}.
     *
     * @return the reject-all filter
     */
    public static PathFilter rejectAll() {
        return BooleanPathFilter.FALSE;
    }

    /**
     * Get a filter which returns {@code true} if the tested path is contained within the given set.
     * Each member of the set is a path separated by "{@code /}" characters; {@code null}s are disallowed.
     *
     * @param paths the path set
     * @return the filter
     */
    public static PathFilter in(Set<String> paths) {
        return new SetPathFilter(new HashSet<String>(paths));
    }

    private static final PathFilter defaultExportFilter;

    static {
        final MultiplePathFilterBuilder builder = PathFilters.multiplePathFilterBuilder(true);
        builder.addFilter(PathFilters.match("META-INF/**"), false);
        builder.addFilter(PathFilters.match("META-INF"), false);
        defaultExportFilter = builder.create();
    }

    /**
     * Get the default export path filter, which excludes all of {@code META-INF} except for the {@code services} path.
     *
     * @return the default export path filter
     */
    public static PathFilter getDefaultExportFilter() {
        return defaultExportFilter;
    }
}
