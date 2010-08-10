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
import java.util.regex.Pattern;

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
     * Get a path filter which only includes file names which match the given glob.
     *
     * @param glob the glob
     * @return the filter
     */
    public static PathFilter include(String glob) {
        return new GlobPathFilter(true, glob);
    }

    /**
     * Get a path filter which only includes file names which match the given pattern.
     *
     * @param pattern the pattern
     * @return the filter
     */
    public static PathFilter include(Pattern pattern) {
        return new GlobPathFilter(true, pattern);
    }

    /**
     * Get a path filter which only includes file names which do not match the given glob.
     *
     * @param glob the glob
     * @return the filter
     */
    public static PathFilter exclude(String glob) {
        return new GlobPathFilter(false, glob);
    }

    /**
     * Get a path filter which only includes file names which do not match the given pattern.
     *
     * @param pattern the pattern
     * @return the filter
     */
    public static PathFilter exclude(Pattern pattern) {
        return new GlobPathFilter(false, pattern);
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
}
