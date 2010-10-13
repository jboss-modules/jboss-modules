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

import org.jboss.modules.util.TestModuleLoader;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test System module usage.
 *
 * @author Ales Justin
 */
public class SystemModuleTest extends AbstractModuleTestCase {

    private static final ModuleIdentifier MODULE_A = ModuleIdentifier.fromString("A");
    private static final ModuleIdentifier MODULE_B = ModuleIdentifier.fromString("B");

    @Test
    public void testSmoke() throws Exception {
        final TestModuleLoader moduleLoader = new TestModuleLoader();

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_A);
        builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.match("org/jboss/modules/**"), PathFilters.rejectAll(), null, ModuleIdentifier.SYSTEM, false));
        moduleLoader.addModuleSpec(builder.create());

        Module module = moduleLoader.loadModule(MODULE_A);
        ClassLoader cl = module.getClassLoader();
        Assert.assertNotNull(cl.loadClass("org.jboss.modules.util.Util"));
    }

    @Test
    public void testTransitive() throws Exception {
        TestModuleLoader moduleLoader = new TestModuleLoader();

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_B);

        builder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_A));
        builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.match("org/jboss/modules/**"), PathFilters.rejectAll(), null, ModuleIdentifier.SYSTEM, false));

        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_A);
        builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.match("org/jboss/modules/**"), PathFilters.rejectAll(), null, ModuleIdentifier.SYSTEM, false));

        moduleLoader.addModuleSpec(builder.create());

        Module module = moduleLoader.loadModule(MODULE_B);
        ClassLoader cl = module.getClassLoader();
        Assert.assertNotNull(cl.loadClass("org.jboss.modules.util.Util"));
    }
}