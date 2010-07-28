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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test to verify the PathFilter functionality.
 * 
 * @author John E. Bailey
 */
public class PathFilterTest {

    private static final String[] EMPTY = new String[0];

    @Test
    public void testExcludes() throws Exception {
        PathFilter pathFilter = new PathFilterImpl(EMPTY, new String[] {"foo/**"});
        assertTrue(pathFilter.accept("foo"));
        assertFalse(pathFilter.accept("foo/bar"));
        assertFalse(pathFilter.accept("foo/bar/baz"));

        pathFilter = new PathFilterImpl(EMPTY, new String[] {"foo/*"});
        assertTrue(pathFilter.accept("foo"));
        assertFalse(pathFilter.accept("foo/bar"));
        assertTrue(pathFilter.accept("foo/bar/baz"));

        pathFilter = new PathFilterImpl(EMPTY, new String[] {"foo"});
        assertFalse(pathFilter.accept("foo"));
        assertTrue(pathFilter.accept("foo/bar"));
        assertTrue(pathFilter.accept("foo/bar/baz"));

        pathFilter = new PathFilterImpl(EMPTY, new String[] {"**/bar/**"});
        assertTrue(pathFilter.accept("foo"));
        assertTrue(pathFilter.accept("foo/bar"));
        assertFalse(pathFilter.accept("foo/bar/baz"));
        assertFalse(pathFilter.accept("foo/baz/bar/biff"));
    }

    @Test
    public void testIncludes() throws Exception {
        PathFilter pathFilter = new PathFilterImpl(new String[] {"foo/**"}, new String[] {"**"});
        assertFalse(pathFilter.accept("foo"));
        assertTrue(pathFilter.accept("foo/bar"));
        assertTrue(pathFilter.accept("foo/bar/baz"));

        pathFilter = new PathFilterImpl(new String[] {"foo/*"}, new String[] {"**"});
        assertFalse(pathFilter.accept("foo"));
        assertTrue(pathFilter.accept("foo/bar"));
        assertFalse(pathFilter.accept("foo/bar/baz"));

        pathFilter = new PathFilterImpl(new String[] {"foo"}, new String[] {"**"});
        assertTrue(pathFilter.accept("foo"));
        assertFalse(pathFilter.accept("foo/bar"));
        assertFalse(pathFilter.accept("foo/bar/baz"));

        pathFilter = new PathFilterImpl(new String[] {"**/bar/**"}, new String[] {"**"});
        assertFalse(pathFilter.accept("foo"));
        assertFalse(pathFilter.accept("foo/bar"));
        assertTrue(pathFilter.accept("foo/bar/baz"));
        assertTrue(pathFilter.accept("foo/baz/bar/biff"));
    }

    @Test
    public void testDelegating() throws Exception {
        PathFilter pathFilterOne = new PathFilterImpl(EMPTY, new String[] {"foo/*"});
        PathFilter pathFilterTwo = new PathFilterImpl(EMPTY, new String[] {"**/bar/*"});
        PathFilter pathFilterThree = new PathFilterImpl(EMPTY, new String[] {"baz/**"});
        PathFilter pathFilter = new DelegatingPathFilter(pathFilterOne, pathFilterTwo, pathFilterThree);
        assertTrue(pathFilter.accept("foo"));
        assertFalse(pathFilter.accept("foo/bar"));
        assertFalse(pathFilter.accept("foo/bar/baz"));
        assertFalse(pathFilter.accept("baz/foo/bar"));
    }
}
