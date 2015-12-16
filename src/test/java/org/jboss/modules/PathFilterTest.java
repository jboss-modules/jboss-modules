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

package org.jboss.modules;

import org.jboss.modules.filter.MultiplePathFilterBuilder;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test to verify the PathFilter functionality.
 *
 * @author John E. Bailey
 */
public class PathFilterTest {

    @Test
    public void testMatch() throws Exception {
        PathFilter pathFilter = PathFilters.match("foo/**");
        assertFalse(pathFilter.accept("foo"));
        assertTrue(pathFilter.accept("foo/bar"));
        assertTrue(pathFilter.accept("foo/bar/baz"));

        pathFilter = PathFilters.match("foo/*");
        assertFalse(pathFilter.accept("foo"));
        assertTrue(pathFilter.accept("foo/bar"));
        assertTrue(pathFilter.accept("foo/bar/baz"));

        pathFilter = PathFilters.match("foo");
        assertTrue(pathFilter.accept("foo"));
        assertTrue(pathFilter.accept("foo/bar"));
        assertTrue(pathFilter.accept("foo/bar/baz"));

        pathFilter = PathFilters.match("**/bar/**");
        assertFalse(pathFilter.accept("foo"));
        assertFalse(pathFilter.accept("foo/bar"));
        assertTrue(pathFilter.accept("foo/bar/baz"));
        assertTrue(pathFilter.accept("foo/baz/bar/biff"));
    }

    @Test
    public void testDelegating() throws Exception {
        final MultiplePathFilterBuilder builder = PathFilters.multiplePathFilterBuilder(true);
        builder.addFilter(PathFilters.match("foo/*"), false);
        builder.addFilter(PathFilters.match("**/bar/**"), false);
        builder.addFilter(PathFilters.match("baz/**"), false);
        PathFilter pathFilter = builder.create();
        assertTrue(pathFilter.accept("foo"));
        assertFalse(pathFilter.accept("foo/bar"));
        assertFalse(pathFilter.accept("foo/bar/baz"));
        assertFalse(pathFilter.accept("baz/foo/bar"));
    }
}
