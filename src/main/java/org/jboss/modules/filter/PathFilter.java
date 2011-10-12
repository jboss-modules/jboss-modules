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
 * Filter used to determine whether a path should be included or excluded from imports and exports.
 *
 * @author John Bailey
 */
public interface PathFilter {

    /**
     * Determine whether a path should be accepted.  The given name is a path separated
     * by "{@code /}" characters.
     *
     * @param path the path to check
     * @return true if the path should be accepted, false if not
     */
    boolean accept(String path);

    /**
     * Calculate a unique hash code for this path filter.  Equal path filters must yield identical hash codes.
     *
     * @return the hash code
     */
    int hashCode();

    /**
     * Determine whether this filter is equal to another.  Filters must implement meaningful (non-identity) equality
     * semantics.
     *
     * @param other the other object
     * @return {@code true} if this filter is the same
     */
    boolean equals(Object other);
}