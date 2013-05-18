/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
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
    private static final ModuleIdentifier MODULE_A = ModuleIdentifier.fromString("test-module-a");

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
        moduleABuilder.addDependency(DependencySpec.createLocalDependencySpec());
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
