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
import java.util.ArrayList;
import java.util.Arrays;
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
        builder.addDependency(MODULE_B).setExport(true);
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_B);
        builder.addDependency(MODULE_C).setExport(true);
        builder.addDependency(MODULE_D);
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_C);
        builder.addDependency(MODULE_A).setExport(true);
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_D);
        moduleLoader.addModuleSpec(builder.create());

        Module module = moduleLoader.loadModule(MODULE_A);
        Set<Module.DependencyExport> dependencyExports = module.getExportedDependencies();
        assertEquals(2, dependencyExports.size());
        assertContains(dependencyExports, MODULE_B, MODULE_C);

        module = moduleLoader.loadModule(MODULE_B);
        dependencyExports = module.getExportedDependencies();
        assertEquals(2, dependencyExports.size());
        assertContains(dependencyExports, MODULE_A, MODULE_C);

        module = moduleLoader.loadModule(MODULE_C);
        dependencyExports = module.getExportedDependencies();
        assertEquals(2, dependencyExports.size());
        assertContains(dependencyExports, MODULE_A, MODULE_B);
    }

    @Test
    public void testImportPaths() throws Exception {
        final TestModuleLoader moduleLoader = new TestModuleLoader();

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_A);
        builder.addDependency(MODULE_B).setExport(true);
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_B);
        builder.addDependency(MODULE_C).setExport(true);
        builder.addDependency(MODULE_D);
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_C);
        builder.addRoot("root", TestResourceLoader.build()
            .addClass(ImportedClass.class)
            .create()
        );
        builder.addDependency(MODULE_A).setExport(true);
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

    private void assertContains(Set<Module.DependencyExport> dependencyExports, ModuleIdentifier... modules) {
        final List<ModuleIdentifier> moduleIdentifierList = new ArrayList<ModuleIdentifier>(Arrays.asList(modules));
        for(Module.DependencyExport dependencyExport: dependencyExports) {
            moduleIdentifierList.remove(dependencyExport.getModule().getIdentifier());
        }
        assertTrue("Not all dependencies found: " + moduleIdentifierList, moduleIdentifierList.isEmpty());
    }

}
