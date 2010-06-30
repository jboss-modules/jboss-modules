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

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test to verify module functionality.
 *
 * @author John Bailey
 */
public class ModuleClassLoaderTest extends AbstractModuleTestCase {

    private ModuleLoader moduleLoader;

    @Before
    public void setupModuleLoader() throws Exception {
        final File repoRoot = getResource("test/repo");
        moduleLoader = new LocalModuleLoader(new File[]{repoRoot});

        // Move some classes over
        copyResource("org/jboss/modules/test/TestClass.class", "test/repo/test/test-with-content/1.0", "rootOne/org/jboss/modules/test");
        copyResource("org/jboss/modules/test/ImportedClass.class", "test/repo/test/test-to-import/1.0", "rootOne/org/jboss/modules/test");
    }

    @Test
    public void testLocalClassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(new ModuleIdentifier("test", "test-with-content", "1.0"));
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            Class<?> testClass = classLoader.loadClass("org.jboss.modules.test.TestClass");
            assertNotNull(testClass);
        } catch (ClassNotFoundException e) {
            fail("Should have loaded local class");
        }
    }

    @Test
    public void testImportClassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(new ModuleIdentifier("test", "test-with-content", "1.0"));
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            Class<?> testClass = classLoader.loadClass("org.jboss.modules.test.ImportedClass");
            assertNotNull(testClass);
        } catch (ClassNotFoundException e) {
            fail("Should have loaded imported class");
        }
    }

    @Test
    public void testExportClassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(new ModuleIdentifier("test", "test-with-content", "1.0"));
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            classLoader.loadExportedClass("org.jboss.modules.test.ImportedClass");
            fail("Should have thrown ClassNotFoundException");
        } catch (ClassNotFoundException expected) {
        }

        final Module exportingModule = moduleLoader.loadModule(new ModuleIdentifier("test", "test-with-export", "1.0"));
        final ModuleClassLoader exportingClassLoader = exportingModule.getClassLoader();

        try {
            Class<?> testClass = exportingClassLoader.loadExportedClass("org.jboss.modules.test.ImportedClass");
            assertNotNull(testClass);
        } catch (ClassNotFoundException e) {
            fail("Should have loaded exported class");
        }
    }

    @Test
    public void testFilteredExportClassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(new ModuleIdentifier("test", "test-with-filtered-export", "1.0"));
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            classLoader.loadExportedClass("org.jboss.modules.test.ImportedClass");
            fail("Should have thrown ClassNotFoundException");
        } catch (ClassNotFoundException expected) {
        }
    }

}
