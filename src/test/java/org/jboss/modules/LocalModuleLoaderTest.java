/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test to verify the functionality of the LocalModuleLoader.
 *
 * @author John Bailey
 */
public class LocalModuleLoaderTest extends AbstractModuleTestCase {
    private ModuleLoader moduleLoader;

    @Before
    public void setupModuleLoader() throws Exception {
        final File repoRoot = getResource("test/repo");
        moduleLoader = new LocalModuleLoader(new File[] {repoRoot});
    }

    @Test
    public void testBasicLoad() throws Exception {
        Module module = moduleLoader.loadModule(MODULE_ID);
        assertNotNull(module);
    }

    @Test
    public void testLoadWithDeps() throws Exception {
        Module module = moduleLoader.loadModule(new ModuleIdentifier("test", "test-with-deps", "1.0"));
        assertNotNull(module);
    }

    @Test
    public void testLoadWithBadDeps() throws Exception {
        try {
            moduleLoader.loadModule(new ModuleIdentifier("test", "test-bad-deps", "1.0"));
            fail("Should have thrown a ModuleNotFoundException");
        } catch(ModuleNotFoundException expected) {}
    }

    @Test
    public void testLoadWithCircularDeps() throws Exception {
        assertNotNull(moduleLoader.loadModule(new ModuleIdentifier("test", "test-circular-deps-A", "1.0")));
        assertNotNull(moduleLoader.loadModule(new ModuleIdentifier("test", "test-circular-deps-B", "1.0")));
        assertNotNull(moduleLoader.loadModule(new ModuleIdentifier("test", "test-circular-deps-C", "1.0")));
        assertNotNull(moduleLoader.loadModule(new ModuleIdentifier("test", "test-circular-deps-D", "1.0")));
    }
}
