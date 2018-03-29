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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test to verify conditional expressions
 *
 * @author Stuart Douglas
 */
public class ConditionalResourceTest extends AbstractModuleTestCase {

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Method method = ModuleLoader.class.getDeclaredMethod("installMBeanServer");
        method.setAccessible(true);
        method.invoke(null);
        System.setProperty("test-prop", "testval");
    }

    /**
     * Validate that system property conditional expressions work correctly
     * @throws Exception
     */
    @Test
    public void testMultipleServices() throws Exception {
        final File repoRoot = getResource("test/repo");
        final String item = Paths.get("").toAbsolutePath().resolve("./target/test-classes/test/repo").normalize().toString();
        final String[] classPath = { item };
        final String deps = "test.conditional-active,test.conditional-not-active";
        final String mainClass = null;
        final ModuleLoader moduleLoader = new DelegatingModuleLoader(Module.getSystemModuleLoader(), new ClassPathModuleFinder(new LocalModuleLoader(new File[] { repoRoot }), classPath, deps, mainClass));
        final Module module = moduleLoader.loadModule(item);
        final ClassLoader classLoader = module.getClassLoader();
        final URL active = classLoader.getResource("active.txt");
        final URL notActive = classLoader.getResource("not-active.txt");
        assertNotNull(active);
        assertNull(notActive);
    }
}
