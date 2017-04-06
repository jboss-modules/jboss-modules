package org.jboss.modules;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import org.jboss.modules.test.TestClass;
import org.jboss.modules.util.TestModuleLoader;
import org.jboss.modules.util.TestResourceLoader;
import org.junit.Test;

/**
 * Verifies the functionality of alias modules in unload/reload scenarios.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ModuleClassLoaderAliasReloadTest extends AbstractModuleTestCase {

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
                                .addResources(getResource("test/aliasmodule/rootOne"))
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
                                .addResources(getResource("test/aliasmodule/rootTwo"))
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

    /**
     * Ensure the Alias modules do not leak during unload/reload, see https://issues.jboss.org/browse/MODULES-241
     *
     * @throws Exception
     */
    @Test
    public void testAliasModuleReload() throws Exception {
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
        for (final ResourceLoader rl : classLoader1.getResourceLoaders()) { ((CloseAwareResourceLoader)rl).close(); }
        for (final ResourceLoader rl : classLoader2.getResourceLoaders()) { ((CloseAwareResourceLoader)rl).close(); }
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
        for (final ResourceLoader rl : classLoader1.getResourceLoaders()) { ((CloseAwareResourceLoader)rl).close(); }
        for (final ResourceLoader rl : classLoader2.getResourceLoaders()) { ((CloseAwareResourceLoader)rl).close(); }
        moduleLoader.unloadModuleLocal(testModule1);
        moduleLoader.unloadModuleLocal(testModule2);
    }


    /**
     * Ensure the Alias modules do not leak during unload/reload, see https://issues.jboss.org/browse/MODULES-241
     *
     * @throws Exception
     */
    @Test
    public void testAliasModuleReload2() throws Exception {
        // FIRST PHASE
        configureModules();
        Module testModule1 = moduleLoader.loadModule(MODULE_ONE_ID);
        Module testModule2 = moduleLoader.loadModule(MODULE_TWO_AL);
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
        for (final ResourceLoader rl : classLoader1.getResourceLoaders()) { ((CloseAwareResourceLoader)rl).close(); }
        for (final ResourceLoader rl : classLoader2.getResourceLoaders()) { ((CloseAwareResourceLoader)rl).close(); }
        moduleLoader.unloadModuleLocal(testModule1);
        moduleLoader.unloadModuleLocal(testModule2);

        // SECOND PHASE
        configureModules();
        testModule1 = moduleLoader.loadModule(MODULE_ONE_ID);
        moduleLoader.relink(testModule1);
        testModule2 = moduleLoader.loadModule(MODULE_TWO_AL);
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
        for (final ResourceLoader rl : classLoader1.getResourceLoaders()) { ((CloseAwareResourceLoader)rl).close(); }
        for (final ResourceLoader rl : classLoader2.getResourceLoaders()) { ((CloseAwareResourceLoader)rl).close(); }
        moduleLoader.unloadModuleLocal(testModule1);
        moduleLoader.unloadModuleLocal(testModule2);
    }


}
