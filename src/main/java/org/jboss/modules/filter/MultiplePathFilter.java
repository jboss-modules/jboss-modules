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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class MultiplePathFilter implements PathFilter {
    private final PathFilter[] filters;
    private final boolean[] includeFlag;
    private final boolean defaultVal;

    MultiplePathFilter(final PathFilter[] filters, final boolean[] includeFlag, final boolean defaultVal) {
        this.filters = filters;
        this.includeFlag = includeFlag;
        this.defaultVal = defaultVal;
    }

    public boolean accept(final String path) {
        final int len = filters.length;
        for (int i = 0; i < len; i++) {
            if (filters[i].accept(path)) return includeFlag[i];
        }
        return defaultVal;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("multi-path filter {");
        int len = filters.length;
        for (int i = 0; i < len; i++) {
            final PathFilter filter = filters[i];
            final boolean include = includeFlag[i];
            builder.append(include ? "include " : "exclude ");
            builder.append(filter);
            builder.append(", ");
        }
        builder.append("default ").append(defaultVal ? "accept" : "reject");
        builder.append('}');
        return builder.toString();
    }
}
