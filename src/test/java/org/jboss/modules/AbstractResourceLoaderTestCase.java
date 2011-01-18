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

import static org.jboss.modules.util.Util.readBytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;


/**
 * Abstract Test Case used as the base for all resource loader tests.
 *
 * @author John Bailey
 */
public abstract class AbstractResourceLoaderTestCase extends AbstractModuleTestCase {

    protected ResourceLoader loader;

    @Before
    public void setupLoader() throws Exception {
        loader = createLoader(PathFilters.acceptAll());
    }

    protected abstract ResourceLoader createLoader(final PathFilter exportFilter) throws Exception;
    protected abstract void assertResource(final Resource resource, final String fileName);

    @Test
    public void testBasicResource() throws Exception {
        Resource resource = loader.getResource("/test.txt");
        assertNotNull(resource);
        assertResource(resource, "test.txt");
        resource = loader.getResource("/nested/nested.txt");
        assertNotNull(resource);
        assertResource(resource, "nested/nested.txt");
    }

    @Test
    public void testMissingResource() throws Exception {
        Resource resource = loader.getResource("/test-bogus.txt");
        assertNull(resource);
    }

    @Test
    public void testIndexPaths() throws Exception {
        final Collection<String> paths = loader.getPaths();
        assertFalse(paths.isEmpty());

        assertTrue(paths.contains(""));
        assertTrue(paths.contains("META-INF"));
        assertTrue(paths.contains("nested"));
        assertTrue(paths.contains("org"));
        assertTrue(paths.contains("org/jboss"));
        assertTrue(paths.contains("org/jboss/modules"));
        assertTrue(paths.contains("org/jboss/modules/test"));
    }

    @Test
    public void testGetClassSpec() throws Exception {
        ClassSpec spec = loader.getClassSpec(Module.fileNameOfClass("org.jboss.modules.test.TestClass"));
        assertNotNull(spec);
        byte[] bytes = spec.getBytes();

        final URL classResource = getClass().getClassLoader().getResource("org/jboss/modules/test/TestClass.class");
        final byte[] expectedBytes = readBytes(classResource.openStream());
        assertArrayEquals(expectedBytes, bytes);
    }

    @Test
    public void testMissingClassSpec() throws Exception {
        ClassSpec spec = loader.getClassSpec(Module.fileNameOfClass("org.jboss.modules.test.BogusClass"));
        assertNull(spec);
    }

    @Test
    public void testGetPackageSpec() throws Exception {
        PackageSpec spec = loader.getPackageSpec("org/jboss/modules/test");
        assertNotNull(spec);

        assertEquals("JBoss Modules Test Classes", spec.getSpecTitle());
        assertEquals("0.1", spec.getSpecVersion());
        assertEquals("JBoss", spec.getSpecVendor());
        assertEquals("org.jboss.modules.test", spec.getImplTitle());
        assertEquals("1.0", spec.getImplVersion());
        assertEquals("JBoss", spec.getImplVendor());
    }

    @Test
    public void testMissingPackageSpec() throws Exception {
        PackageSpec spec = loader.getPackageSpec("org/jboss/modules/bogus");
        assertNotNull(spec);

        assertNull(spec.getSpecTitle());
        assertNull(spec.getSpecVersion());
        assertNull(spec.getSpecVendor());
        assertNull(spec.getImplTitle());
        assertNull(spec.getImplVersion());
        assertNull(spec.getImplVendor());
    }
}
