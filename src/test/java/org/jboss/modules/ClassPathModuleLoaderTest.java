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

import java.lang.reflect.Method;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test to verify the functionality of the ClassPathModuleLoader.
 *
 * @author @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Scott Stark (sstark@redhat.com)
 */
public class ClassPathModuleLoaderTest extends AbstractModuleTestCase {

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Method method = ModuleLoader.class.getDeclaredMethod("installMBeanServer");
        method.setAccessible(true);
        method.invoke(null);
    }

    @Test
    public void testLoader() throws Exception {
        final File repoRoot = getResource("test/repo");
        final String item = Paths.get("").toAbsolutePath().resolve("./target/test-classes/test/repo").normalize().toString();
        final String[] classPath = { item };
        final String deps = "test.test,test.with-deps";
        final String mainClass = "org.jboss.modules.test.TestClass";
        final ModuleLoader moduleLoader = new DelegatingModuleLoader(Module.getSystemModuleLoader(), new ClassPathModuleFinder(new LocalModuleLoader(new File[] { repoRoot }), classPath, deps, mainClass));
        final Module module = moduleLoader.loadModule(item);
        module.getClassLoader();
        assertNotNull(module);
    }

    /**
     * I need to be able to load EJBContainerProvider from a dependency.
     *
     * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
     */
    @Test
    public void testService() throws Exception {
        final File repoRoot = getResource("test/repo");
        final String item = Paths.get("").toAbsolutePath().resolve("./target/test-classes/test/repo").normalize().toString();
        final String[] classPath = { item };
        final String deps = "test.service";
        final String mainClass = null;
        final ModuleLoader moduleLoader = new DelegatingModuleLoader(Module.getSystemModuleLoader(), new ClassPathModuleFinder(new LocalModuleLoader(new File[] { repoRoot }), classPath, deps, mainClass));
        final Module module = moduleLoader.loadModule(item);
        final ClassLoader classLoader = module.getClassLoader();
        final URL url = classLoader.getResource("META-INF/services/dummy");
        assertNotNull(url);
    }

    /**
     * Validate that dependent module META-INF/services/* content is seen
     * @throws Exception
     */
    @Test
    public void testMultipleServices() throws Exception {
        final File repoRoot = getResource("test/repo");
        final String item = Paths.get("").toAbsolutePath().resolve("./target/test-classes/test/repo").normalize().toString();
        final String[] classPath = { item };
        final String deps = "test.jaxrs";
        final String mainClass = null;
        final ModuleLoader moduleLoader = new DelegatingModuleLoader(Module.getSystemModuleLoader(), new ClassPathModuleFinder(new LocalModuleLoader(new File[] { repoRoot }), classPath, deps, mainClass));
        final Module module = moduleLoader.loadModule(item);
        final ClassLoader classLoader = module.getClassLoader();
        final Enumeration<URL> services = classLoader.getResources("META-INF/services/javax.ws.rs.ext.Providers");
        assertNotNull(services);
        ArrayList<URL> found = new ArrayList<URL>();
        while(services.hasMoreElements()) {
            found.add(services.nextElement());
        }
        assertEquals("Found 2 services of type javax.ws.rs.ext.Providers", 2, found.size());
    }
}
