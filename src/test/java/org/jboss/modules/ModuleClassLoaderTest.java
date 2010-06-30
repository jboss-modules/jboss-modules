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

import org.jboss.modules.test.ImportedClass;
import org.jboss.modules.test.TestClass;
import org.jboss.modules.util.ModuleSpecBuilder;
import org.jboss.modules.util.TestModuleLoader;
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

    private static final ModuleIdentifier MODULE_WITH_CONTENT_ID = new ModuleIdentifier("test", "test-with-content", "1.0");
    private static final ModuleIdentifier MODULE_TO_IMPORT_ID = new ModuleIdentifier("test", "test-to-import", "1.0");
    private static final ModuleIdentifier MODULE_WITH_EXPORT_ID = new ModuleIdentifier("test", "test-with-export", "1.0");
    private static final ModuleIdentifier MODULE_WITH_FILTERED_EXPORT_ID = new ModuleIdentifier("test", "test-with-filtered-export", "1.0");

    private TestModuleLoader moduleLoader;

    @Before
    public void setupModuleLoader() throws Exception {
        moduleLoader = new TestModuleLoader();

        final ModuleSpecBuilder moduleWithContentBuilder = moduleLoader.buildModuleSpec(MODULE_WITH_CONTENT_ID);
        moduleWithContentBuilder.addRoot("rootOne")
            .addClass(TestClass.class);
        moduleWithContentBuilder
            .addDependency(MODULE_TO_IMPORT_ID);
        moduleWithContentBuilder.install();

        final ModuleSpecBuilder moduleToImportBuilder = moduleLoader.buildModuleSpec(MODULE_TO_IMPORT_ID);
        moduleToImportBuilder.addRoot("rootOne")
            .addClass(ImportedClass.class);
        moduleToImportBuilder.install();

        final ModuleSpecBuilder moduleWithExportBuilder = moduleLoader.buildModuleSpec(MODULE_WITH_EXPORT_ID);
        moduleWithExportBuilder
            .addDependency(MODULE_TO_IMPORT_ID).setExport(true);
        moduleWithExportBuilder.install();

        final ModuleSpecBuilder moduleWithExportFilterBuilder = moduleLoader.buildModuleSpec(MODULE_WITH_FILTERED_EXPORT_ID);
        moduleWithExportFilterBuilder
            .addDependency(MODULE_TO_IMPORT_ID)
                .setExport(true)
                .addExportExclude("org/jboss/**");
        moduleWithExportFilterBuilder.install();
    }

    @Test
    public void testLocalClassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
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
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
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
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            classLoader.loadExportedClass("org.jboss.modules.test.ImportedClass");
            fail("Should have thrown ClassNotFoundException");
        } catch (ClassNotFoundException expected) {
        }

        final Module exportingModule = moduleLoader.loadModule(MODULE_WITH_EXPORT_ID);
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
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_FILTERED_EXPORT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            classLoader.loadExportedClass("org.jboss.modules.test.ImportedClass");
            fail("Should have thrown ClassNotFoundException");
        } catch (ClassNotFoundException expected) {
        }
    }

}
