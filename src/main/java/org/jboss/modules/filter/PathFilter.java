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