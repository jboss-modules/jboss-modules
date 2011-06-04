/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;
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
    }

    @Test
    public void testLoader() throws Exception {
        final File repoRoot = getResource("test/repo");
        final String classPath = "./target/test-classes/test/repo";
        final String deps = "test.test,test.with-deps";
        final String mainClass = "org.jboss.modules.test.TestClass";
        final ModuleLoader moduleLoader = new ClassPathModuleLoader(new LocalModuleLoader(new File[] { repoRoot }), mainClass, classPath, deps);
        Module module = moduleLoader.loadModule(ModuleIdentifier.CLASSPATH);
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
        final String classPath = "./target/test-classes/test/repo";
        final String deps = "test.service";
        final String mainClass = null;
        final ModuleLoader moduleLoader = new ClassPathModuleLoader(new LocalModuleLoader(new File[] { repoRoot }), mainClass, classPath, deps);
        final Module module = moduleLoader.loadModule(ModuleIdentifier.CLASSPATH);
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
        final String classPath = "./target/test-classes/test/repo";
        final String deps = "test.jaxrs";
        final String mainClass = null;
        final ModuleLoader moduleLoader = new ClassPathModuleLoader(new LocalModuleLoader(new File[] { repoRoot }), mainClass, classPath, deps);
        final Module module = moduleLoader.loadModule(ModuleIdentifier.CLASSPATH);
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
