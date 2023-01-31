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

import org.jboss.modules.test.ClassA;
import org.jboss.modules.util.TestModuleLoader;
import org.jboss.modules.util.TestResourceLoader;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class ErrorHandlingTest extends AbstractModuleTestCase {
    private static final String MODULE_A = "test-module-a";

    private TestModuleLoader moduleLoader;

    @Before
    public void before() throws Exception {
        moduleLoader = new TestModuleLoader();

        final ModuleSpec.Builder moduleABuilder = ModuleSpec.build(MODULE_A);
        moduleABuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(
                TestResourceLoader.build()
                        .addClass(ClassA.class)
                        .create())
        );
        moduleABuilder.addDependency(DependencySpec.OWN_DEPENDENCY);
        moduleLoader.addModuleSpec(moduleABuilder.create());
    }

    @Test
    public void testNonLinkingClass() throws ModuleLoadException, ClassNotFoundException {
        final Module module = moduleLoader.loadModule(MODULE_A);
        final ClassLoader classLoader = module.getClassLoader();
        try {
            classLoader.loadClass(ClassA.class.getName());
            fail("Should have thrown a LinkageError");
        } catch(LinkageError e) {
            // good
        }
    }
}
