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

import junit.framework.AssertionFailedError;

import org.jboss.modules.DependencySpec.DependencyBuilder;
import org.jboss.modules.ModuleDependencySpec.Builder;
import org.jboss.modules.util.TestModuleLoader;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test Relinking
 */
public class RelinkTest extends AbstractModuleTestCase {

    private static final ModuleIdentifier MODULE_A = ModuleIdentifier.fromString("A");
    private static final ModuleIdentifier MODULE_B = ModuleIdentifier.fromString("B");

    @Test
    public void testSmoke() throws Exception {
        final TestModuleLoader moduleLoader = new TestModuleLoader();
        Module.setModuleLoaderSelector(new SimpleModuleLoaderSelector(moduleLoader));

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_A);
        moduleLoader.addModuleSpec(builder.create());

        Module module = moduleLoader.loadModule(MODULE_A);
        ClassLoader cl = module.getClassLoader();
        try {
            cl.loadClass("org.jboss.modules.util.Util");
            throw new AssertionFailedError("Should not have loaded class");
        } catch (Exception e) {
        }

        ModuleDependencySpec.Builder sysDep = ModuleDependencySpec.build(ModuleIdentifier.SYSTEM);
        sysDep.setImportFilter(PathFilters.match("org/jboss/modules/**"));
        ModuleDependencySpec spec = sysDep.create();

        DependencyBuilder dep = DependencySpec.build().addModuleDependency(spec);
        moduleLoader.setAndRelinkDependencies(module, dep.create());

        Assert.assertNotNull(cl.loadClass("org.jboss.modules.util.Util"));
    }

    @Test
    public void testTransitive() throws Exception {
        TestModuleLoader moduleLoader = new TestModuleLoader();
        Module.setModuleLoaderSelector(new SimpleModuleLoaderSelector(moduleLoader));

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_B);
        ModuleDependencySpec.Builder depA = ModuleDependencySpec.build(MODULE_A);
        depA.setExportFilter(PathFilters.acceptAll());
        builder.addModuleDependency(depA.create());

        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_A);
        moduleLoader.addModuleSpec(builder.create());

        Module.setModuleLoaderSelector(new SimpleModuleLoaderSelector(moduleLoader));
        try
        {
            Module moduleB = moduleLoader.loadModule(MODULE_B);
            ClassLoader cl = moduleB.getClassLoader();
            try {
                cl.loadClass("org.jboss.modules.util.Util");
                throw new AssertionFailedError();
            } catch (ClassNotFoundException e) {
            }

            Builder sysDep = ModuleDependencySpec.build(ModuleIdentifier.SYSTEM);
            sysDep.setImportFilter(PathFilters.match("org/jboss/modules/**"));
            sysDep.setExportFilter(PathFilters.match("org/jboss/modules/**"));

            Module moduleA = moduleLoader.loadModule(MODULE_A);
            DependencyBuilder dep = DependencySpec.build().addModuleDependency(sysDep.create());
            moduleLoader.setAndRelinkDependencies(moduleA, dep.create());
            moduleLoader.relink(moduleB);

            Assert.assertNotNull(cl.loadClass("org.jboss.modules.util.Util"));

        }
        finally
        {
            Module.setModuleLoaderSelector(ModuleLoaderSelector.DEFAULT);
        }
    }


}