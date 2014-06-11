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
 * Static factory methods for class filter types.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ClassFilters {

    private ClassFilters() {
    }

    /**
     * Get a filter which always returns {@code true}.
     *
     * @return the accept-all filter
     */
    public static ClassFilter acceptAll() {
        return BooleanClassFilter.TRUE;
    }

    /**
     * Get a filter which always returns {@code false}.
     *
     * @return the reject-all filter
     */
    public static ClassFilter rejectAll() {
        return BooleanClassFilter.FALSE;
    }

    /**
     * Get a class filter which uses a resource path filter to filter classes.
     *
     * @param resourcePathFilter the resource path filter
     * @return the class filter
     */
    public static ClassFilter fromResourcePathFilter(final PathFilter resourcePathFilter) {
        return resourcePathFilter == PathFilters.acceptAll() ? acceptAll() : new PathClassFilter(resourcePathFilter);
    }
}
