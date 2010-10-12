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

import org.jboss.modules.test.ImportedClass;
import org.jboss.modules.util.TestModuleLoader;
import org.jboss.modules.util.TestResourceLoader;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
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

    private static final ModuleIdentifier MODULE_A = ModuleIdentifier.fromString("a");
    private static final ModuleIdentifier MODULE_B = ModuleIdentifier.fromString("b");
    private static final ModuleIdentifier MODULE_C = ModuleIdentifier.fromString("c");
    private static final ModuleIdentifier MODULE_D = ModuleIdentifier.fromString("d");

    @Test
    public void testExportDependencies() throws Exception {
        final TestModuleLoader moduleLoader = new TestModuleLoader();
        Module.setModuleLoaderSelector(new SimpleModuleLoaderSelector(moduleLoader));

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_A);
        builder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_B, true, false));
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_B);
        builder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_C, true, false));
        builder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_D));
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_C);
        builder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_A, true, false));
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_D);
        moduleLoader.addModuleSpec(builder.create());

        Module module = moduleLoader.loadModule(MODULE_A);
        final Set<ModuleIdentifier> dependencyExports = new HashSet<ModuleIdentifier>();
        getExportedModuleDeps(module, dependencyExports);
        assertEquals(2, dependencyExports.size());
        assertTrue(dependencyExports.contains(MODULE_B));
        assertTrue(dependencyExports.contains(MODULE_C));

        dependencyExports.clear();
        module = moduleLoader.loadModule(MODULE_B);
        getExportedModuleDeps(module, dependencyExports);
        assertEquals(2, dependencyExports.size());
        assertTrue(dependencyExports.contains(MODULE_A));
        assertTrue(dependencyExports.contains(MODULE_C));

        dependencyExports.clear();
        module = moduleLoader.loadModule(MODULE_C);
        getExportedModuleDeps(module, dependencyExports);
        assertEquals(2, dependencyExports.size());
        assertTrue(dependencyExports.contains(MODULE_A));
        assertTrue(dependencyExports.contains(MODULE_B));
    }

    private static void getExportedModuleDeps(final Module module, final Set<ModuleIdentifier> dependencyExports) throws ModuleLoadException {
        getExportedModuleDeps(module, new HashSet<Module>(Collections.singleton(module)), dependencyExports);
    }

    private static void getExportedModuleDeps(final Module module, final Set<Module> visited, final Set<ModuleIdentifier> dependencyExports) throws ModuleLoadException {
        for (Dependency dependency : module.getDependencies()) {
            if (dependency instanceof ModuleDependency && dependency.getExportFilter() != PathFilters.rejectAll()) {
                final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                final Module md = moduleDependency.getModuleLoader().loadModule(moduleDependency.getIdentifier());
                if (md != null && moduleDependency.getExportFilter() != PathFilters.rejectAll()) {
                    if (visited.add(md)) {
                        dependencyExports.add(md.getIdentifier());
                        getExportedModuleDeps(md, visited, dependencyExports);
                    }
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testImportPaths() throws Exception {
        final TestModuleLoader moduleLoader = new TestModuleLoader();
        Module.setModuleLoaderSelector(new SimpleModuleLoaderSelector(moduleLoader));

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_A);
        builder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_B, true, false));
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_B);
        builder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_C, true));
        builder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_D));
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_C);
        builder.addResourceRoot(TestResourceLoader.build()
            .addClass(ImportedClass.class)
            .create()
        );
        builder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_A, true));
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_D);
        moduleLoader.addModuleSpec(builder.create());

        Module module = moduleLoader.loadModule(MODULE_A);
        module.getClassLoader().loadClass(ImportedClass.class.getName());

        final Field pathsField = Module.class.getDeclaredField("paths");
        pathsField.setAccessible(true);
        final Object paths = pathsField.get(module);
        final Field allPathsField = paths.getClass().getDeclaredField("allPaths");
        allPathsField.setAccessible(true);
        final Map<String, List<LocalLoader>> allPaths = (Map<String, List<LocalLoader>>) allPathsField.get(paths);

        Module moduleC = moduleLoader.loadModule(MODULE_C);

        assertEquals(4, allPaths.size());
        for(Map.Entry<String, List<LocalLoader>> entry : allPaths.entrySet()) {
            assertEquals(1, entry.getValue().size());
            assertEquals(moduleC.getClassLoaderPrivate().getLocalLoader(), entry.getValue().get(0));
        }
    }
}
