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

import java.net.URL;
import java.util.Enumeration;

import org.jboss.modules.util.TestModuleLoader;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test System pkgs resources
 *
 * @author Jason T. Greene
 */
public class SystemResourcesTest  {
    private static final ModuleIdentifier MODULE_A = ModuleIdentifier.fromString("A");

    static {
        System.setProperty("jboss.modules.system.pkgs", "javax.activation");
    }


    @Test
    public void testResources() throws Exception {
        final TestModuleLoader moduleLoader = new TestModuleLoader();

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_A);
        //builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.match("org/jboss/modules/**"), PathFilters.rejectAll(), null, ModuleIdentifier.SYSTEM, false));
        moduleLoader.addModuleSpec(builder.create());

        Module module = moduleLoader.loadModule(MODULE_A);
        ClassLoader cl = module.getClassLoader();

        Enumeration<URL> resources = cl.getResources("javax/activation/DataHandler.class");
        Assert.assertTrue(resources.hasMoreElements());

        resources = cl.getResources("javax/sql/RowSet.class");
        Assert.assertFalse(resources.hasMoreElements());

        resources = cl.getResources("org/jboss/");
        Assert.assertFalse(resources.hasMoreElements());
    }
}