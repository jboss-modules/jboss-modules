/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.jboss.modules.DependencySpec.createModuleDependencySpec;
import static org.jboss.modules.ModuleIdentifier.create;
import static org.jboss.modules.ModuleSpec.build;

import org.jboss.modules.util.TestModuleLoader;
import org.junit.Test;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class BrokenDependencyTest {
    @Test
    public void testBrokenDependency() throws ModuleLoadException {
        final TestModuleLoader loader = new TestModuleLoader();
        final ModuleSpec moduleB = build(create("moduleB", "1.0")).create();
        loader.addModuleSpec(moduleB);
        // create a module definition with a typo
        final ModuleSpec moduleA = build(create("moduleA"))
                .addDependency(createModuleDependencySpec(create("moduleB", "typo")))
                .create();
        loader.addModuleSpec(moduleA);
        try {
            // put Modules in a broken state
            loader.loadModule(create("moduleA"));
        } catch (ModuleLoadException e) {
            assertEquals("No module spec found for module moduleB:typo", e.getMessage());
        }
        // fix the module definition
        final ModuleSpec fixedModuleA = build(create("moduleA"))
                .addDependency(createModuleDependencySpec(create("moduleB", "1.0")))
                .create();
        loader.addModuleSpec(fixedModuleA);
        try {
            // now it should load okay
            loader.loadModule(create("moduleA"));
        } catch (ModuleLoadException e) {
            fail(e.getMessage());
        }
    }
}
