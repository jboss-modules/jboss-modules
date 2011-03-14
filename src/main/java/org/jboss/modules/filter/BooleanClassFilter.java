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

package org.jboss.modules.filter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class BooleanClassFilter implements ClassFilter {

    private final boolean result;

    private BooleanClassFilter(final boolean result) {
        this.result = result;
    }

    public boolean accept(final String path) {
        return result;
    }

    static final BooleanClassFilter TRUE = new BooleanClassFilter(true);
    static final BooleanClassFilter FALSE = new BooleanClassFilter(false);

    public int hashCode() {
        return Boolean.valueOf(result).hashCode();
    }

    public boolean equals(final Object obj) {
        return obj == this;
    }

    public String toString() {
        return result ? "Accept" : "Reject";
    }
}
