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

import org.jboss.modules.test.TestClass;
import org.jboss.modules.util.TestResourceLoader;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jboss.modules.util.Util.readBytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test to verify the functionality of the ModuleContentLoader.
 *
 * @author John Bailey
 */
public class ModuleContentLoaderTest extends AbstractModuleTestCase {

    private ModuleContentLoader moduleContentLoader;

    @Before
    public void setupContentLoader() throws Exception {
        ModuleContentLoader.Builder builder = ModuleContentLoader.build();

        final TestResourceLoader.TestResourceLoaderBuilder rootOneBuilder = TestResourceLoader.build();
        rootOneBuilder.addResources(getResource("test/modulecontentloader/rootOne"))
            .addClass(TestClass.class);

        final TestResourceLoader.TestResourceLoaderBuilder rootTwoBuilder = TestResourceLoader.build();
        rootTwoBuilder.addResources(getResource("test/modulecontentloader/rootTwo"));

        builder.add("rootOne", rootOneBuilder.create());
        builder.add("rootTwo", rootTwoBuilder.create());

        moduleContentLoader = builder.create();
    }

    @Test
    public void testDelegatedResourceLoad() throws Exception {
        Resource resource = moduleContentLoader.getResource("test.txt");
        assertNotNull(resource);
        resource = moduleContentLoader.getResource("testTwo.txt");
        assertNotNull(resource);

        Iterable<Resource> resources = moduleContentLoader.getResources("/nested/nested.txt");
        assertNotNull(resource);
        List<String> roots = new ArrayList<String>(2);

        final Pattern pattern = Pattern.compile("root\\w\\w\\w");
        for(Resource aResource : resources) {
            final String path = aResource.getURL().getPath();
            final Matcher matcher = pattern.matcher(path);
            if(matcher.find()) {
                roots.add(matcher.group());
            }
        }
        assertEquals(2, roots.size());
        assertTrue(roots.contains("rootOne"));
        assertTrue(roots.contains("rootTwo"));
    }

    @Test
    public void testDelegatedResourceLoadNotFound() throws Exception {
        Resource resource = moduleContentLoader.getResource("bogus.txt");
        assertNull(resource);
    }

     @Test
    public void testDelegatedResourceFromRoot() throws Exception {
        Resource resource = moduleContentLoader.getResource("rootTwo", "test.txt");
        assertNull(resource);
        resource = moduleContentLoader.getResource("rootTwo", "testTwo.txt");
        assertNotNull(resource);
        resource = moduleContentLoader.getResource("rootTwo", "nested/nested.txt");
        assertNotNull(resource);
    }

    @Test
    public void testGetLocalPaths() throws Exception {
        final Collection<String> paths = moduleContentLoader.getLocalPaths();
        assertFalse(paths.isEmpty());

        assertTrue(paths.contains("META-INF"));
        assertTrue(paths.contains("nested"));
        assertTrue(paths.contains("nestedTwo"));
        assertTrue(paths.contains("org"));
        assertTrue(paths.contains("org/jboss"));
        assertTrue(paths.contains("org/jboss/modules"));
        assertTrue(paths.contains("org/jboss/modules/test"));
    }

    @Test
    public void testFilteredLocalPaths() throws Exception {
        final Collection<String> paths = moduleContentLoader.getFilteredLocalPaths();
        assertFalse(paths.isEmpty());

        assertFalse(paths.contains("META-INF"));
        assertTrue(paths.contains("nested"));
        assertTrue(paths.contains("nestedTwo"));
        assertTrue(paths.contains("org"));
        assertTrue(paths.contains("org/jboss"));
        assertTrue(paths.contains("org/jboss/modules"));
        assertTrue(paths.contains("org/jboss/modules/test"));

        final ModuleContentLoader.Builder builder = ModuleContentLoader.build();

        final TestResourceLoader.TestResourceLoaderBuilder rootBuilder = TestResourceLoader.build();
            rootBuilder
                .addResources(getResource("test/modulecontentloader/rootOne"))
                .addClass(TestClass.class)
                .addExportExclude("org/**")
                .addExportInclude("META-INF")
                .addExportInclude("org/jboss/modules/**");

        builder.add("rootOne", rootBuilder.create());

        final ModuleContentLoader filteredLoader = builder.create();

        final Collection<String> filteredPaths = filteredLoader.getFilteredLocalPaths();
        assertFalse(filteredPaths.isEmpty());

        assertTrue(filteredPaths.contains("META-INF"));
        assertTrue(filteredPaths.contains("nested"));
        assertTrue(filteredPaths.contains("org"));
        assertFalse(filteredPaths.contains("org/jboss"));
        assertFalse(filteredPaths.contains("org/jboss/modules"));
        assertTrue(filteredPaths.contains("org/jboss/modules/test"));

    }

    @Test
    public void testDelegatingGetClassSpec() throws Exception {
        ClassSpec spec = moduleContentLoader.getClassSpec("org.jboss.modules.test.TestClass");
        assertNotNull(spec);
        byte[] bytes = spec.getBytes();

        final URL classResource = getClass().getClassLoader().getResource("org/jboss/modules/test/TestClass.class");
        final byte[] expectedBytes = readBytes(classResource.openStream());
        assertArrayEquals(expectedBytes, bytes);
    }

    @Test
    public void testDelegatingGetClassSpecNotFound() throws Exception {
        ClassSpec spec = moduleContentLoader.getClassSpec("org.jboss.modules.test.BogusClass");
        assertNull(spec);
    }

    @Test
    public void testDelegatingGetPackageSpec() throws Exception {
        PackageSpec spec = moduleContentLoader.getPackageSpec("org/jboss/modules/test");
        assertNotNull(spec);

        assertEquals("JBoss Modules Test Classes", spec.getSpecTitle());
        assertEquals("0.1", spec.getSpecVersion());
        assertEquals("JBoss", spec.getSpecVendor());
        assertEquals("org.jboss.modules.test", spec.getImplTitle());
        assertEquals("1.0", spec.getImplVersion());
        assertEquals("JBoss", spec.getImplVendor());
    }

    @Test
    public void testDelegatingGetPackageSpecNotFound() throws Exception {
        PackageSpec spec = moduleContentLoader.getPackageSpec("org/jboss/modules/bogus");
        assertNotNull(spec);

        assertNull(spec.getSpecTitle());
        assertNull(spec.getSpecVersion());
        assertNull(spec.getSpecVendor());
        assertNull(spec.getImplTitle());
        assertNull(spec.getImplVersion());
        assertNull(spec.getImplVendor());
    }
}
