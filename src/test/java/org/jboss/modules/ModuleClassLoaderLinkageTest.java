/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

import org.jboss.modules.test.TestClass;
import org.jboss.modules.util.TestModuleLoader;
import org.jboss.modules.util.TestResourceLoader;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import static org.junit.Assert.*;

/**
 * Test to verify proper module linkage functionality.
 * There was an issue with alias modules that were relinked
 * even though the module they alias have been removed.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ModuleClassLoaderLinkageTest extends AbstractModuleTestCase {

    private static final ModuleIdentifier MODULE_ONE_ID = ModuleIdentifier.fromString("test-module-1");
    private static final ModuleIdentifier MODULE_TWO_ID = ModuleIdentifier.fromString("test-module-2");
    private static final ModuleIdentifier MODULE_TWO_AL = ModuleIdentifier.fromString("test-module-2-alias");
    private TestModuleLoader moduleLoader = new TestModuleLoader();

    private static final class CloseAwareResourceLoader implements ResourceLoader {

        private final ResourceLoader delegate;
        private volatile boolean closed;

        private CloseAwareResourceLoader(final ResourceLoader delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getRootName() {
            if (closed) throw new IllegalStateException();
            return delegate.getRootName();
        }

        @Override
        public ClassSpec getClassSpec(String fileName) throws IOException {
            if (closed) throw new IllegalStateException();
            return delegate.getClassSpec(fileName);
        }

        @Override
        public PackageSpec getPackageSpec(String name) throws IOException {
            if (closed) throw new IllegalStateException();
            return delegate.getPackageSpec(name);
        }

        @Override
        public Resource getResource(String name) {
            if (closed) throw new IllegalStateException();
            return delegate.getResource(name);
        }

        @Override
        public String getLibrary(String name) {
            if (closed) throw new IllegalStateException();
            return delegate.getLibrary(name);
        }

        @Override
        public Collection<String> getPaths() {
            if (closed) throw new IllegalStateException();
            return delegate.getPaths();
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    public void configureModules() throws Exception {
        // first module
        final ModuleSpec.Builder moduleOneBuilder = ModuleSpec.build(MODULE_ONE_ID);
        moduleOneBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(
                new CloseAwareResourceLoader(
                        TestResourceLoader.build()
                                .addClass(TestClass.class)
                                .addResources(getResource("test/linkagetest/rootOne"))
                                .create())
        ));
        moduleOneBuilder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_TWO_AL));
        moduleOneBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleOneBuilder.create());
        // second module
        final ModuleSpec.Builder moduleTwoBuilder = ModuleSpec.build(MODULE_TWO_ID);
        moduleTwoBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(
                new CloseAwareResourceLoader(
                        TestResourceLoader.build()
                                .addClass(TestClass.class)
                                .addResources(getResource("test/linkagetest/rootTwo"))
                                .create()
                )
        ));
        moduleTwoBuilder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_ONE_ID));
        moduleTwoBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleTwoBuilder.create());
        // second alias module
        final ModuleSpec.AliasBuilder moduleTwoAliasBuilder = ModuleSpec.buildAlias(MODULE_TWO_AL, MODULE_TWO_ID);
        moduleLoader.addModuleSpec(moduleTwoAliasBuilder.create());
    }

    @Test
    public void testRelinkIssue() throws Exception {
        // FIRST PHASE
        configureModules();
        Module testModule1 = moduleLoader.loadModule(MODULE_ONE_ID);
        Module testModule2 = moduleLoader.loadModule(MODULE_TWO_ID);
        ModuleClassLoader classLoader1 = testModule1.getClassLoader();
        ModuleClassLoader classLoader2 = testModule2.getClassLoader();

        try {
            Class<?> testClass1 = classLoader1.loadClass("org.jboss.modules.test.TestClass");
            assertNotNull(testClass1);
            Class<?> testClass2 = classLoader2.loadClass("org.jboss.modules.test.TestClass");
            assertNotNull(testClass2);
            assertNotNull(testClass1.getResource("/test.txt"));
            assertNotNull(testClass2.getResource("/test.txt"));
        } catch (ClassNotFoundException e) {
            fail();
        }
        // cleanup
        for (final ResourceLoader rl : classLoader1.getResourceLoaders()) rl.close();
        for (final ResourceLoader rl : classLoader2.getResourceLoaders()) rl.close();
        moduleLoader.unloadModuleLocal(testModule1);
        moduleLoader.unloadModuleLocal(testModule2);

        // SECOND PHASE
        configureModules();
        testModule1 = moduleLoader.loadModule(MODULE_ONE_ID);
        moduleLoader.relink(testModule1);
        testModule2 = moduleLoader.loadModule(MODULE_TWO_ID);
        moduleLoader.relink(testModule2);
        classLoader1 = testModule1.getClassLoader();
        classLoader2 = testModule2.getClassLoader();

        try {
            Class<?> testClass1 = classLoader1.loadClass("org.jboss.modules.test.TestClass");
            assertNotNull(testClass1);
            Class<?> testClass2 = classLoader2.loadClass("org.jboss.modules.test.TestClass");
            assertNotNull(testClass2);
            assertNotNull(testClass1.getResource("/test.txt"));
            assertNotNull(testClass2.getResource("/test.txt"));
        } catch (ClassNotFoundException e) {
            fail();
        }
        // cleanup
        for (final ResourceLoader rl : classLoader1.getResourceLoaders()) rl.close();
        for (final ResourceLoader rl : classLoader2.getResourceLoaders()) rl.close();
        moduleLoader.unloadModuleLocal(testModule1);
        moduleLoader.unloadModuleLocal(testModule2);
    }
}
