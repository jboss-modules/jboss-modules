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
final class ChildPathFilter implements PathFilter {

    private final String prefix;

    ChildPathFilter(final String path) {
        prefix = path.charAt(path.length() - 1) == '/' ? path : path + "/";
    }

    public boolean accept(final String path) {
        return path.startsWith(prefix);
    }

    public boolean equals(final Object obj) {
        return obj instanceof ChildPathFilter && equals((ChildPathFilter) obj);
    }

    public boolean equals(final ChildPathFilter obj) {
        return obj != null && obj.prefix.equals(prefix);
    }

    public String toString() {
        return "children of \"" + prefix + "\"";
    }

    public int hashCode() {
        return prefix.hashCode();
    }
}
