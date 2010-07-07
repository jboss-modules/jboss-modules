/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
 * Test to verify the ExportFilter functionality.
 * 
 * @author John E. Bailey
 */
public class ExportFilterTest {

    private static final String[] EMPTY = new String[0];

    @Test
    public void testExcludes() throws Exception {
        ExportFilter exportFilter = new ExportFilter(EMPTY, new String[] {"foo/**"});
        assertTrue(exportFilter.shouldExport("foo"));
        assertFalse(exportFilter.shouldExport("foo/bar"));
        assertFalse(exportFilter.shouldExport("foo/bar/baz"));

        exportFilter = new ExportFilter(EMPTY, new String[] {"foo/*"});
        assertTrue(exportFilter.shouldExport("foo"));
        assertFalse(exportFilter.shouldExport("foo/bar"));
        assertTrue(exportFilter.shouldExport("foo/bar/baz"));

        exportFilter = new ExportFilter(EMPTY, new String[] {"foo"});
        assertFalse(exportFilter.shouldExport("foo"));
        assertTrue(exportFilter.shouldExport("foo/bar"));
        assertTrue(exportFilter.shouldExport("foo/bar/baz"));

        exportFilter = new ExportFilter(EMPTY, new String[] {"**/bar/**"});
        assertTrue(exportFilter.shouldExport("foo"));
        assertTrue(exportFilter.shouldExport("foo/bar"));
        assertFalse(exportFilter.shouldExport("foo/bar/baz"));
        assertFalse(exportFilter.shouldExport("foo/baz/bar/biff"));
    }

    @Test
    public void testIncludes() throws Exception {
        ExportFilter exportFilter = new ExportFilter(new String[] {"foo/**"}, new String[] {"**"});
        assertFalse(exportFilter.shouldExport("foo"));
        assertTrue(exportFilter.shouldExport("foo/bar"));
        assertTrue(exportFilter.shouldExport("foo/bar/baz"));

        exportFilter = new ExportFilter(new String[] {"foo/*"}, new String[] {"**"});
        assertFalse(exportFilter.shouldExport("foo"));
        assertTrue(exportFilter.shouldExport("foo/bar"));
        assertFalse(exportFilter.shouldExport("foo/bar/baz"));

        exportFilter = new ExportFilter(new String[] {"foo"}, new String[] {"**"});
        assertTrue(exportFilter.shouldExport("foo"));
        assertFalse(exportFilter.shouldExport("foo/bar"));
        assertFalse(exportFilter.shouldExport("foo/bar/baz"));

        exportFilter = new ExportFilter(new String[] {"**/bar/**"}, new String[] {"**"});
        assertFalse(exportFilter.shouldExport("foo"));
        assertFalse(exportFilter.shouldExport("foo/bar"));
        assertTrue(exportFilter.shouldExport("foo/bar/baz"));
        assertTrue(exportFilter.shouldExport("foo/baz/bar/biff"));
    }
}
