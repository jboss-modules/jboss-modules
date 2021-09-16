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

import java.io.File;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test to verify the functionality of module properties.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ModulePropertyTest extends AbstractModuleTestCase {
    private ModuleLoader moduleLoader;

    @Before
    public void setupModuleLoader() throws Exception {
        final File repoRoot = getResource("test/repo");
        moduleLoader = new LocalModuleLoader(new File[] {repoRoot});
    }

    @Test
    public void testBasic() throws Exception {
        Module module = moduleLoader.loadModule("test.test");
        assertNull(module.getProperty("non-existent"));
        assertEquals("blah", module.getProperty("non-existent", "blah"));
        assertEquals("true", module.getProperty("test.prop.1"));
        assertEquals("propertyValue", module.getProperty("test.prop.2"));
    }

    /**
     * This test parses properties attached to module dependencies.
     * The properties are currently not exposed via the dependency spec API,
     * so the test simply makes sure they can be parsed w/o errors.
     * @throws Exception
     */
    @Test
    public void testModuleDependencyProperties() throws Exception {
        Module module = moduleLoader.loadModule("test.dep-props");
        assertNull(module.getProperty("non-existent"));
        assertEquals("blah", module.getProperty("non-existent", "blah"));

        final DependencySpec[] deps = module.getDependencies();
        int testedDepsTotal = 0;
        for(DependencySpec dep : deps) {
            if(!(dep instanceof ModuleDependencySpec)) {
                continue;
            }
            final ModuleDependencySpec moduleDep = (ModuleDependencySpec) dep;
            String depName = moduleDep.getName();
            if(!depName.startsWith("test.")) {
                continue;
            }
            ++testedDepsTotal;
            depName = depName.substring("test.".length());
            if(depName.equals("test")) {
            } else {
                fail("Unexpected module dependency " + moduleDep);
            }
        }
        assertEquals(1, testedDepsTotal);
    }
}
