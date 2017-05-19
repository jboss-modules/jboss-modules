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
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.test.ImportedClass;
import org.jboss.modules.test.ImportedInterface;
import org.jboss.modules.test.TestClass;
import org.jboss.modules.util.TestModuleLoader;
import org.jboss.modules.util.TestResourceLoader;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import static org.jboss.modules.util.Util.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test to verify module functionality.
 *
 * @author John Bailey
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ModuleClassLoaderTest extends AbstractModuleTestCase {

    private static final ModuleIdentifier MODULE_WITH_CONTENT_ID = ModuleIdentifier.fromString("test-with-content");
    private static final ModuleIdentifier MODULE_WITH_RESOURCE_ID = ModuleIdentifier.fromString("test-with-resource");
    private static final ModuleIdentifier MODULE_TO_IMPORT_ID = ModuleIdentifier.fromString("test-to-import");
    private static final ModuleIdentifier MODULE_WITH_EXPORT_ID = ModuleIdentifier.fromString("test-with-export");
    private static final ModuleIdentifier MODULE_WITH_DOUBLE_EXPORT_ID = ModuleIdentifier.fromString("test-with-double-export");
    private static final ModuleIdentifier MODULE_WITH_INVERTED_DOUBLE_EXPORT_ID = ModuleIdentifier.fromString("test-with-inverted-double-export");
    private static final ModuleIdentifier MODULE_WITH_FILTERED_EXPORT_ID = ModuleIdentifier.fromString("test-with-filtered-export");
    private static final ModuleIdentifier MODULE_WITH_FILTERED_IMPORT_ID = ModuleIdentifier.fromString("test-with-filtered-import");
    private static final ModuleIdentifier MODULE_WITH_FILTERED_DOUBLE_EXPORT_ID = ModuleIdentifier.fromString("test-with-filtered-double-export");

    private TestModuleLoader moduleLoader;

    @Before
    public void setupModuleLoader() throws Exception {
        moduleLoader = new TestModuleLoader();

        final ModuleSpec.Builder moduleWithContentBuilder = ModuleSpec.build(MODULE_WITH_CONTENT_ID);
        moduleWithContentBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(
                TestResourceLoader.build()
                .addClass(TestClass.class)
                .addResources(getResource("test/modulecontentloader/rootOne"))
                .create()
        ));
        moduleWithContentBuilder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_TO_IMPORT_ID));
        moduleWithContentBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleWithContentBuilder.create());

        final ModuleSpec.Builder moduleWithResourceBuilder = ModuleSpec.build(MODULE_WITH_RESOURCE_ID);
        moduleWithResourceBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(
                TestResourceLoader.build()
                .addClass(TestClass.class)
                .addResources(getResource("class-resources"))
                .create()
        ));
        moduleWithResourceBuilder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_TO_IMPORT_ID));
        moduleWithResourceBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleWithResourceBuilder.create());

        final ModuleSpec.Builder moduleToImportBuilder = ModuleSpec.build(MODULE_TO_IMPORT_ID);
        moduleToImportBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(
                TestResourceLoader.build()
                .addClass(ImportedClass.class)
                .addClass(ImportedInterface.class)
                .addResources(getResource("test/modulecontentloader/rootTwo"))
                .create()
        ));
        moduleToImportBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleToImportBuilder.create());

        final ModuleSpec.Builder moduleWithExportBuilder = ModuleSpec.build(MODULE_WITH_EXPORT_ID);
        moduleWithExportBuilder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_TO_IMPORT_ID, true, false));
        moduleWithExportBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleWithExportBuilder.create());

        final MultiplePathFilterBuilder nestedAndOrgJBossExcludingBuilder = PathFilters.multiplePathFilterBuilder(true);
        nestedAndOrgJBossExcludingBuilder.addFilter(PathFilters.match("org/jboss/**"), false);
        nestedAndOrgJBossExcludingBuilder.addFilter(PathFilters.match("nested"), false);

        final ModuleSpec.Builder moduleWithExportFilterBuilder = ModuleSpec.build(MODULE_WITH_FILTERED_EXPORT_ID);
        moduleWithExportFilterBuilder.addDependency(DependencySpec.createModuleDependencySpec(nestedAndOrgJBossExcludingBuilder.create(), null, MODULE_TO_IMPORT_ID, false));
        moduleWithExportFilterBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleWithExportFilterBuilder.create());

        final ModuleSpec.Builder moduleWithImportFilterBuilder = ModuleSpec.build(MODULE_WITH_FILTERED_IMPORT_ID);
        moduleWithImportFilterBuilder.addDependency(DependencySpec.createModuleDependencySpec(nestedAndOrgJBossExcludingBuilder.create(), PathFilters.rejectAll(), null, MODULE_TO_IMPORT_ID, false));
        moduleWithImportFilterBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleWithImportFilterBuilder.create());

        final ModuleSpec.Builder moduleWithDoubleExportBuilder = ModuleSpec.build(MODULE_WITH_DOUBLE_EXPORT_ID);
        moduleWithDoubleExportBuilder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_TO_IMPORT_ID, true));
        moduleWithDoubleExportBuilder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_WITH_CONTENT_ID, true));
        moduleWithDoubleExportBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleWithDoubleExportBuilder.create());

        final ModuleSpec.Builder moduleWithInvertedDoubleExportBuilder = ModuleSpec.build(MODULE_WITH_INVERTED_DOUBLE_EXPORT_ID);
        moduleWithInvertedDoubleExportBuilder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_WITH_CONTENT_ID, true));
        moduleWithInvertedDoubleExportBuilder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_TO_IMPORT_ID, true));
        moduleWithInvertedDoubleExportBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleWithInvertedDoubleExportBuilder.create());

        final ModuleSpec.Builder moduleWithFilteredDoubleExportBuilder = ModuleSpec.build(MODULE_WITH_FILTERED_DOUBLE_EXPORT_ID);
        moduleWithFilteredDoubleExportBuilder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.not(PathFilters.match("nested")), PathFilters.acceptAll(), null, MODULE_TO_IMPORT_ID, false));
        moduleWithFilteredDoubleExportBuilder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_WITH_EXPORT_ID, true));
        moduleWithFilteredDoubleExportBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleWithFilteredDoubleExportBuilder.create());
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
    public void testResourceLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_RESOURCE_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            Class<?> testClass = classLoader.loadClass("org.jboss.modules.test.TestClass");
            // direct
            assertNotNull(testClass.getResource("/file1.txt")); // translates to /file1.txt
            assertNotNull(testClass.getResource("file2.txt")); // translates to /org/jboss/modules/test/file2.txt
            // relative
            assertNotNull(testClass.getResource("../../../../file1.txt")); // should translate to /file1.txt
            assertNotNull(testClass.getResource("test/../file2.txt")); // should translate to /org/jboss/modules/test/file2.txt
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            fail("Should have loaded local class");
        }
    }

    @Test
    public void testLocalClassLoadNotFound() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            classLoader.loadClass("org.jboss.modules.test.BogusClass");
            fail("Should have thrown ClassNotFoundException");
        } catch (ClassNotFoundException expected) {
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
    public void testFilteredImportClassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_FILTERED_IMPORT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            classLoader.loadClass("org.jboss.modules.test.ImportedClass");
            fail("Should have thrown ClassNotFoundException");
        } catch (ClassNotFoundException expected) {
        }
    }

    @Test
    public void testDoubleExportCLassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_DOUBLE_EXPORT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        Class<?> testClass = classLoader.loadExportedClass("org.jboss.modules.test.ImportedClass");
        assertNotNull(testClass);

        testClass = classLoader.loadExportedClass("org.jboss.modules.test.TestClass");
        assertNotNull(testClass);
    }

    @Test
    public void testInvertedDoubleExportCLassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_INVERTED_DOUBLE_EXPORT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        Class<?> testClass = classLoader.loadExportedClass("org.jboss.modules.test.ImportedClass");
        assertNotNull(testClass);

        testClass = classLoader.loadExportedClass("org.jboss.modules.test.TestClass");
        assertNotNull(testClass);
    }

    @Test
    public void testFilteredDoubleExportCLassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_FILTERED_DOUBLE_EXPORT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        Class<?> testClass = classLoader.loadExportedClass("org.jboss.modules.test.ImportedClass");
        assertNotNull(testClass);
    }

    @Test
    public void testLocalResourceRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final URL resUrl = classLoader.getResource("test.txt");
        assertNotNull(resUrl);
    }

    @Test
    public void testLocalResourceRetrievalNotFound() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final URL resUrl = classLoader.getResource("bogus.txt");
        assertNull(resUrl);
    }

    @Test
    public void testImportResourceRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final URL resUrl = classLoader.getResource("testTwo.txt");
        assertNotNull(resUrl);
    }

    @Test
    public void testFilteredImportResourceRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_FILTERED_IMPORT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        URL resUrl = classLoader.getResource("nested/nested.txt");
        assertNull(resUrl);
    }

    @Test
    public void testLocalResourcesRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final Enumeration<URL> resUrls = classLoader.getResources("test.txt");
        assertNotNull(resUrls);
        final List<URL> resUrlList = toList(resUrls);
        assertEquals(1, resUrlList.size());
        assertTrue(resUrlList.get(0).getPath().contains("rootOne"));
    }

    @Test
    public void testLocalResourcesRetrievalNotFound() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final Enumeration<URL> resUrls = classLoader.getResources("bogus.txt");
        assertNotNull(resUrls);
        final List<URL> resUrlList = toList(resUrls);
        assertTrue(resUrlList.isEmpty());
    }

    @Test
    public void testImportResourcesRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final Enumeration<URL> resUrls = classLoader.getResources("testTwo.txt");
        assertNotNull(resUrls);
        final List<URL> resUrlList = toList(resUrls);
        assertEquals(1, resUrlList.size());
        assertTrue(resUrlList.get(0).getPath().contains("rootTwo"));
    }

    @Test
    public void testLocalAndImportResourcesRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final Enumeration<URL> resUrls = classLoader.getResources("nested/nested.txt");
        assertNotNull(resUrls);
        final List<URL> resUrlList = toList(resUrls);
        assertEquals(2, resUrlList.size());
        boolean rootOne = false;
        boolean rootTwo = false;
        for(URL resUrl : resUrlList) {
            if(!rootOne)
                rootOne = resUrl.getPath().contains("rootOne");
            if(!rootTwo)
                rootTwo = resUrl.getPath().contains("rootTwo");
        }
        assertTrue(rootOne);
        assertTrue(rootTwo);
    }

    @Test
    public void testFilteredImportResourcesRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_FILTERED_IMPORT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        Enumeration<URL> resUrls = classLoader.getResources("nested/nested.txt");
        List<URL> resUrlList = toList(resUrls);
        assertTrue(resUrlList.isEmpty());
    }

    @Test
    public void testManifest() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        final Class<?> testClass = classLoader.loadClass("org.jboss.modules.test.TestClass");
        System.out.println(testClass.getClassLoader());
        final Package pkg = testClass.getPackage();
        assertEquals("JBoss Modules Test Classes", pkg.getSpecificationTitle());
    }
}
