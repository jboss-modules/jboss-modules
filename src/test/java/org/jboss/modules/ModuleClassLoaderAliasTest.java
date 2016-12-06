package org.jboss.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

        Module testModule1 = moduleLoader.loadModule(ModuleIdentifier.fromString("test.alias-cyclic.module-one"));
        Module testModule2 = moduleLoader.loadModule(ModuleIdentifier.fromString("test.alias-cyclic.module-two"));
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
        moduleLoader.unloadModuleLocal(testModule1);
        moduleLoader.unloadModuleLocal(testModule2);

    }

    @Test
    public void testAliasSimple() throws Exception {
        final File repoRoot = getResource("test/repo");
        final ModuleLoader moduleLoader = new LocalModuleLoader(new File[] { repoRoot });

        Module testModule1 = moduleLoader.loadModule(ModuleIdentifier.fromString("test.alias-simple.module-one"));
        Module testModule2 = moduleLoader.loadModule(ModuleIdentifier.fromString("test.alias-simple.module-two"));
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

        moduleLoader.unloadModuleLocal(testModule1);
        moduleLoader.unloadModuleLocal(testModule2);

    }

    private static void assertResourceString(URL resource, String expected) throws IOException {
        assertNotNull(resource);
        byte[] bytes = Util.readBytes(resource.openStream());
        assertEquals(expected, new String(bytes, Charset.forName("utf-8")));
    }
}
