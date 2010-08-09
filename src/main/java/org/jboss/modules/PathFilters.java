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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PathFilters {
    private PathFilters() {}

    public static PathFilter all(PathFilter... filters) {
        return new AggregatePathFilter(false, filters);
    }

    public static PathFilter all(Collection<PathFilter> filters) {
        return all(filters.toArray(new PathFilter[filters.size()]));
    }

    public static PathFilter any(PathFilter... filters) {
        return new AggregatePathFilter(true, filters);
    }

    public static PathFilter any(Collection<PathFilter> filters) {
        return any(filters.toArray(new PathFilter[filters.size()]));
    }

    public static PathFilter not(PathFilter filter) {
        return new InvertingPathFilter(filter);
    }

    public static PathFilter include(String path) {
        return new GlobPathFilter(true, path);
    }

    public static PathFilter include(Pattern pattern) {
        return new GlobPathFilter(true, pattern);
    }

    public static PathFilter exclude(String path) {
        return new GlobPathFilter(false, path);
    }

    public static PathFilter exclude(Pattern pattern) {
        return new GlobPathFilter(false, pattern);
    }

    public static PathFilter acceptAll() {
        return PathFilter.ACCEPT_ALL;
    }

    public static PathFilter rejectAll() {
        return PathFilter.REJECT_ALL;
    }
}
