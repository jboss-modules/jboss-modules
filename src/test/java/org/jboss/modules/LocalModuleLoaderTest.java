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

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import static org.junit.Assert.assertNotEquals;
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
        Module module = moduleLoader.loadModule("test.test");
        assertNotNull(module);
    }

    @Test
    public void testCurrent() throws Exception {
        ModuleLoader loader = Module.getCallerModuleLoader();
        System.out.println(loader);
    }


    @Test
    public void testLoadWithDeps() throws Exception {
        Module module = moduleLoader.loadModule(ModuleIdentifier.fromString("test.with-deps"));
        assertNotNull(module);
    }

    @Test
    public void testLoadWithBadDeps() throws Exception {
        try {
            moduleLoader.loadModule(ModuleIdentifier.fromString("test.bad-deps.1_0"));
            fail("Should have thrown a ModuleNotFoundException");
        } catch(ModuleNotFoundException expected) {}
    }

    @Test
    public void testLoadWithCircularDeps() throws Exception {
        assertNotNull(moduleLoader.loadModule(ModuleIdentifier.fromString("test.circular-deps-A")));
        assertNotNull(moduleLoader.loadModule(ModuleIdentifier.fromString("test.circular-deps-B")));
        assertNotNull(moduleLoader.loadModule(ModuleIdentifier.fromString("test.circular-deps-C")));
        assertNotNull(moduleLoader.loadModule(ModuleIdentifier.fromString("test.circular-deps-D")));
    }

    @Test
    public void testAbsentModule() throws Exception {
        try {
            moduleLoader.loadModule("test.absent");
            fail("Should have thrown a ModuleNotFoundException");
        } catch(ModuleNotFoundException expected) {}
    }

    @Test
    public void testIterateModules() throws Exception {
        Iterator<String> names = moduleLoader.iterateModules((String) null, true);
        while (names.hasNext()) {
            String name = names.next();
            assertNotEquals("test.absent", name);
            if (!name.equals("test.bad-deps")) {
                Module module = moduleLoader.loadModule(name);
                assertNotNull(module);
            }
        }
    }
}
