package org.jboss.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import org.jboss.modules.util.Util;
import org.junit.Test;

/**
 * Tests the basic functionality of alias modules.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ModuleClassLoaderAliasTest extends AbstractModuleTestCase {

    @Test
    public void testAliasCyclic() throws Exception {
        final File repoRoot = getResource("test/repo");
        final ModuleLoader moduleLoader = new LocalModuleLoader(new File[] { repoRoot });

        Module testModule1 = moduleLoader.loadModule("test.alias-cyclic.module-one");
        Module testModule2 = moduleLoader.loadModule("test.alias-cyclic.module-two");
        ModuleClassLoader classLoader1 = testModule1.getClassLoader();
        ModuleClassLoader classLoader2 = testModule2.getClassLoader();

        try {
            Class<?> testClass1 = classLoader1.loadClass("org.jboss.modules.test.ClassA");
            assertNotNull(testClass1);
            Class<?> testClass2 = classLoader2.loadClass("org.jboss.modules.test.ClassB");
            assertNotNull(testClass2);

            Class<?> testClass1ViaLoader2 = classLoader2.loadClass("org.jboss.modules.test.ClassA");
            assertNotNull(testClass1ViaLoader2);
            assertSame(testClass1, testClass1ViaLoader2);

            Class<?> testClass2ViaLoader1 = classLoader1.loadClass("org.jboss.modules.test.ClassB");
            assertNotNull(testClass2ViaLoader1);
            assertSame(testClass2, testClass2ViaLoader1);

            assertResourceString(testClass1.getResource("/test.txt"), "Test file 1");
            assertResourceString(testClass2.getResource("/test.txt"), "Test file 2");

            assertResourceString(classLoader1.getResource("/test.txt"), "Test file 1");
            assertResourceString(classLoader2.getResource("/test.txt"), "Test file 2");

        } catch (ClassNotFoundException e) {
            fail();
        }
        moduleLoader.unloadModuleLocal("test.alias-cyclic.module-one", testModule1);
        assertNull("module-one should not be present in the moduleLoader after it has been removed", moduleLoader.findLoadedModuleLocal("test.alias-cyclic.module-one"));

        /* Assert that unloading an aliased module also removes the aliases */
        moduleLoader.unloadModuleLocal("test.alias-cyclic.module-two", testModule2);
        assertNull("module-two should not be present in the moduleLoader after it has been removed", moduleLoader.findLoadedModuleLocal("test.alias-cyclic.module-two"));
        assertNull("module-two-alias should should not be present in the moduleLoader after module-two has been removed", moduleLoader.findLoadedModuleLocal("test.alias-cyclic.module-two-alias"));

    }

    @Test
    public void testAliasSimple() throws Exception {
        final File repoRoot = getResource("test/repo");
        final ModuleLoader moduleLoader = new LocalModuleLoader(new File[] { repoRoot });

        Module testModule1 = moduleLoader.loadModule("test.alias-simple.module-one");
        Module testModule2 = moduleLoader.loadModule("test.alias-simple.module-two");
        ModuleClassLoader classLoader1 = testModule1.getClassLoader();
        ModuleClassLoader classLoader2 = testModule2.getClassLoader();

        Class<?> testClass1 = classLoader1.loadClass("org.jboss.modules.test.ClassA");
        assertNotNull(testClass1);
        Class<?> testClass2 = classLoader2.loadClass("org.jboss.modules.test.ClassB");
        assertNotNull(testClass2);

        Class<?> testClass2ViaLoader1 = classLoader1.loadClass("org.jboss.modules.test.ClassB");
        assertNotNull(testClass2ViaLoader1);
        assertSame(testClass2, testClass2ViaLoader1);

        try {
            classLoader2.loadClass("org.jboss.modules.test.ClassA");
            fail("ClassNotFoundException expected");
        } catch (ClassNotFoundException expected) {
        }

        assertResourceString(testClass1.getResource("/test.txt"), "Test file 1");
        assertResourceString(testClass2.getResource("/test.txt"), "Test file 2");

        assertResourceString(classLoader1.getResource("/test.txt"), "Test file 1");
        assertResourceString(classLoader2.getResource("/test.txt"), "Test file 2");

        moduleLoader.unloadModuleLocal("test.alias-simple.module-one", testModule1);
        assertNull("module-one should not be present in the moduleLoader after it has been removed", moduleLoader.findLoadedModuleLocal("test.alias-simple.module-one"));

        /* Assert that unloading an aliased module also removes the aliases */
        moduleLoader.unloadModuleLocal("test.alias-simple.module-two", testModule2);
        assertNull("module-two should not be present in the moduleLoader after it has been removed", moduleLoader.findLoadedModuleLocal("test.alias-simple.module-two"));
        assertNull("module-two-alias should should not be present in the moduleLoader after module-two has been removed", moduleLoader.findLoadedModuleLocal("test.alias-simple.module-two-alias"));

    }

    /**
     * A reproducer for MODULES-282
     */
    @Test
    public void testAliasResource() throws Exception {
        final File repoRoot = getResource("test/repo");
        final ModuleLoader moduleLoader = new LocalModuleLoader(new File[] { repoRoot });

        Module dependentAliasModule = moduleLoader.loadModule("test.alias-resources.dependent-module-alias");

        /* Assert that the whole dependency graph is there */
        assertNotNull("dependent-module-alias should be present in the moduleLoader after dependent-module-alias has been loaded", moduleLoader.findLoadedModuleLocal("test.alias-resources.dependent-module-alias"));
        assertNotNull("dependent-module should be present in the moduleLoader after dependent-module-alias has been loaded", moduleLoader.findLoadedModuleLocal("test.alias-resources.dependent-module"));
        assertNotNull("dependency-module should be present in the moduleLoader after dependent-module-alias has been loaded", moduleLoader.findLoadedModuleLocal("test.alias-resources.dependency-module"));


        ModuleClassLoader dependentAliasClassLoader = dependentAliasModule.getClassLoader();
        assertResourceString(dependentAliasClassLoader.getResource("/dependency-resource.txt"), "Dependency resource");

        moduleLoader.unloadModuleLocal("test.alias-resources.dependent-module-alias", dependentAliasModule);

        /* Assert that unloading the alias removes just the alias while leaving the aliased module there */
        assertNull("dependent-module-alias should not be present in the moduleLoader after it has been removed", moduleLoader.findLoadedModuleLocal("test.alias-resources.dependent-module-alias"));
        assertNotNull("dependent-module should be present in the moduleLoader after dependent-module-alias has been removed", moduleLoader.findLoadedModuleLocal("test.alias-resources.dependent-module"));
        assertNotNull("dependency-module should be present in the moduleLoader after dependent-module-alias has been removed", moduleLoader.findLoadedModuleLocal("test.alias-resources.dependency-module"));

        moduleLoader.unloadModuleLocal("test.alias-resources.dependent-module", dependentAliasModule);
        assertNull("dependent-module should not be present in the moduleLoader after it has been removed", moduleLoader.findLoadedModuleLocal("test.alias-resources.dependent-module"));

    }
    private static void assertResourceString(URL resource, String expected) throws IOException {
        assertNotNull(resource);
        byte[] bytes = Util.readBytes(resource.openStream());
        assertEquals(expected, new String(bytes, Charset.forName("utf-8")));
    }
}
