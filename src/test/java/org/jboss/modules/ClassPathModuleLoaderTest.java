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

import static org.junit.Assert.assertNotNull;

/**
 * Test to verify the functionality of the ClassPathModuleLoader.
 *
 * @author @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
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
        Module module = moduleLoader.loadModule(ClassPathModuleLoader.IDENTIFIER);
        module.getClassLoader();
        assertNotNull(module);
    }
}
