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

import java.util.HashSet;
import org.jboss.modules.test.ImportedClass;
import org.jboss.modules.util.TestModuleLoader;
import org.jboss.modules.util.TestResourceLoader;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test to verify the module export dependencies and imports are created correctly.  Each module should have an entry
 * directly to the module that has an exported path. 
 *
 * @author John E. Bailey
 */
public class ModuleExportTest extends AbstractModuleTestCase {

    private static final ModuleIdentifier MODULE_A = new ModuleIdentifier("test", "a", "1.0");
    private static final ModuleIdentifier MODULE_B = new ModuleIdentifier("test", "b", "1.0");
    private static final ModuleIdentifier MODULE_C = new ModuleIdentifier("test", "c", "1.0");
    private static final ModuleIdentifier MODULE_D = new ModuleIdentifier("test", "d", "1.0");

    @Test
    public void testExportDependencies() throws Exception {
        final TestModuleLoader moduleLoader = new TestModuleLoader();

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_A);
        builder.addModuleDependency(ModuleDependencySpec.build(MODULE_B).setExportFilter(PathFilter.ACCEPT_ALL).create());
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_B);
        builder.addModuleDependency(ModuleDependencySpec.build(MODULE_C).setExportFilter(PathFilter.ACCEPT_ALL).create());
        builder.addModuleDependency(ModuleDependencySpec.build(MODULE_D).create());
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_C);
        builder.addModuleDependency(ModuleDependencySpec.build(MODULE_A).setExportFilter(PathFilter.ACCEPT_ALL).create());
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_D);
        moduleLoader.addModuleSpec(builder.create());

        Module module = moduleLoader.loadModule(MODULE_A);
        final Set<ModuleIdentifier> dependencyExports = new HashSet<ModuleIdentifier>();
        getExportedModuleDeps(module, dependencyExports);
        assertEquals(2, dependencyExports.size());
        assertTrue(dependencyExports.contains(MODULE_B));
        assertTrue(dependencyExports.contains(MODULE_C));

        module = moduleLoader.loadModule(MODULE_B);
        dependencyExports.clear();
        getExportedModuleDeps(module, dependencyExports);
        assertEquals(2, dependencyExports.size());
        assertTrue(dependencyExports.contains(MODULE_A));
        assertTrue(dependencyExports.contains(MODULE_C));

        module = moduleLoader.loadModule(MODULE_C);
        getExportedModuleDeps(module, dependencyExports);
        assertEquals(2, dependencyExports.size());
        assertTrue(dependencyExports.contains(MODULE_A));
        assertTrue(dependencyExports.contains(MODULE_B));
    }

    private static void getExportedModuleDeps(final Module module, final Set<ModuleIdentifier> dependencyExports) throws ModuleNotFoundException {
        for (Dependency dependency : module.getDependencies()) {
            if (dependency instanceof ModuleDependency) {
                final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                final Module md = moduleDependency.getModuleRequired();
                if (md != null && moduleDependency.getExportFilter() != PathFilter.REJECT_ALL) dependencyExports.add(md.getIdentifier());
            }
        }
    }

    @Test
    public void testImportPaths() throws Exception {
        final TestModuleLoader moduleLoader = new TestModuleLoader();

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_A);
        builder.addModuleDependency(ModuleDependencySpec.build(MODULE_B).setExportFilter(PathFilter.ACCEPT_ALL).create());
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_B);
        builder.addModuleDependency(ModuleDependencySpec.build(MODULE_C).setExportFilter(PathFilter.ACCEPT_ALL).create());
        builder.addModuleDependency(ModuleDependencySpec.build(MODULE_D).create());
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_C);
        builder.addResourceRoot(TestResourceLoader.build()
            .addClass(ImportedClass.class)
            .create()
        );
        builder.addModuleDependency(ModuleDependencySpec.build(MODULE_A).setExportFilter(PathFilter.ACCEPT_ALL).create());
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_D);
        moduleLoader.addModuleSpec(builder.create());

        Module module = moduleLoader.loadModule(MODULE_A);
        module.getClassLoader().loadClass(ImportedClass.class.getName());

        Field importsField = Module.class.getDeclaredField("pathsToImports");
        importsField.setAccessible(true);

        Map<String, List<Module.DependencyImport>> pathsToImport = (Map<String, List<Module.DependencyImport>>)importsField.get(module);

        assertEquals(4, pathsToImport.size());
        for(Map.Entry<String, List<Module.DependencyImport>> entry : pathsToImport.entrySet()) {
            assertEquals(1, entry.getValue().size());
            assertEquals(MODULE_C, entry.getValue().get(0).getModule().getIdentifier());
        }
    }
}
