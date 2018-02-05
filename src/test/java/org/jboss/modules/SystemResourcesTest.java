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
        System.setProperty("jboss.modules.system.pkgs", "javax.naming");
    }


    @Test
    public void testResources() throws Exception {
        final TestModuleLoader moduleLoader = new TestModuleLoader();

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_A);
        //builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.match("org/jboss/modules/**"), PathFilters.rejectAll(), null, ModuleIdentifier.SYSTEM, false));
        moduleLoader.addModuleSpec(builder.create());

        Module module = moduleLoader.loadModule(MODULE_A);
        ClassLoader cl = module.getClassLoader();

        Enumeration<URL> resources = cl.getResources("javax/naming/Context.class");
        Assert.assertTrue(resources.hasMoreElements());

        resources = cl.getResources("javax/sql/RowSet.class");
        Assert.assertFalse(resources.hasMoreElements());

        resources = cl.getResources("org/jboss/");
        Assert.assertFalse(resources.hasMoreElements());
    }
}