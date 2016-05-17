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

import static org.jboss.modules.util.Util.readBytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
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
    protected abstract void assertResource(final Resource resource, final String fileName) throws Exception;

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
        PackageSpec spec = loader.getPackageSpec("org.jboss.modules.test");
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
        PackageSpec spec = loader.getPackageSpec("org.jboss.modules.bogus");
        assertNotNull(spec);

        assertEquals("MODULES-89", spec.getSpecTitle());
        assertNull(spec.getSpecVersion());
        assertNull(spec.getSpecVendor());
        assertNull(spec.getImplTitle());
        assertNull(spec.getImplVersion());
        assertNull(spec.getImplVendor());
    }

    @Test
    public void testIterateResourcesRootRecursive() throws Exception {
        Set<String> expected = new HashSet<String>();
        expected.add("test.txt");
        expected.add("nested/nested.txt");
        expected.add("org/jboss/modules/test/TestClass.class");
        expected.add("META-INF/MANIFEST.MF");
        assertEquals(expected, getResourceNames("/", true));
        assertEquals(expected, getResourceNames("", true));
    }

    @Test
    public void testIterateResourcesRoot() throws Exception {
        Set<String> expected = new HashSet<String>();
        expected.add("test.txt");
        assertEquals(expected, getResourceNames("/", false));
        assertEquals(expected, getResourceNames("", false));
    }

    @Test
    public void testIterateResourcesNested() throws Exception {
        Set<String> expected = new HashSet<String>();
        expected.add("nested/nested.txt");
        assertEquals(expected, getResourceNames("/nested", true));
        assertEquals(expected, getResourceNames("/nested", false));
        assertEquals(expected, getResourceNames("nested", true));
        assertEquals(expected, getResourceNames("nested", false));
    }

    @Test
    public void testIterateResourcesClasses() throws Exception {
        Set<String> expected = new HashSet<String>();
        expected.add("org/jboss/modules/test/TestClass.class");
        assertEquals(expected, getResourceNames("/org/jboss/modules", true));
        assertEquals(expected, getResourceNames("org/jboss/modules", true));
        expected = Collections.<String>emptySet();
        assertEquals(expected, getResourceNames("/org/jboss/modules", false));
        assertEquals(expected, getResourceNames("org/jboss/modules", false));
    }

    private Set<String> getResourceNames(String startPath, boolean recursive) {
        Set<String> result = new HashSet<String>();
        IterableResourceLoader itloader = (IterableResourceLoader) loader;
        Iterator<Resource> itres = itloader.iterateResources(startPath, recursive);
        while(itres.hasNext()) {
            result.add(itres.next().getName());
        }
        return result;
    }
}
