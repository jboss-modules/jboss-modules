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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.jar.JarFile;

import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.util.TestModuleLoader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author Thomas.Diesler@jboss.com
 */
public class ModuleIteratorTest extends AbstractModuleTestCase {

    private static final ModuleIdentifier MODULE_A = ModuleIdentifier.fromString("a");
    private static final ModuleIdentifier MODULE_B = ModuleIdentifier.fromString("b");

    @Test
    public void testMetaInfServicesIterator() throws Exception {
        TestModuleLoader moduleLoader = new TestModuleLoader();

        ModuleSpec.Builder builder = ModuleSpec.build(MODULE_A);
        builder.addDependency(DependencySpec.createModuleDependencySpec(MODULE_B, false, false));
        PathFilter importFilter = PathFilters.getMetaInfServicesFilter();
        PathFilter exportFilter = PathFilters.acceptAll();
        builder.addDependency(DependencySpec.createModuleDependencySpec(importFilter, exportFilter, moduleLoader, MODULE_B, false));
        moduleLoader.addModuleSpec(builder.create());

        builder = ModuleSpec.build(MODULE_B);
        ResourceLoader resB = new JarFileResourceLoader("jarB", toJarFile(getModuleB()));
        builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(resB));
        builder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(builder.create());

        Module moduleA = moduleLoader.loadModule(MODULE_A);
        Iterator<Resource> itres = moduleA.iterateResources(PathFilters.getMetaInfServicesFilter());
        Assert.assertTrue("Found a resource", itres.hasNext());
        Assert.assertEquals("META-INF/services/org/apache/camel/component/jms", itres.next().getName());
        Assert.assertFalse("No other resource", itres.hasNext());
    }

    @Test
    public void testIterateModules() throws Exception {
        IterableModuleFinder fakeFinder = new IterableModuleFinder() {
            private ModuleIdentifier[] modules = { ModuleIdentifier.create("a"), ModuleIdentifier.create("b")};

            @Override
            public Iterator<ModuleIdentifier> iterateModules(ModuleIdentifier baseIdentifier, boolean recursive) {
                return new Iterator<ModuleIdentifier>() {
                    private int pos = 0;

                    @Override
                    public boolean hasNext() {
                        return pos < modules.length;
                    }

                    @Override
                    public ModuleIdentifier next() {
                        return modules[pos++];
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public ModuleSpec findModule(ModuleIdentifier identifier, ModuleLoader delegateLoader)
                throws ModuleLoadException {
                for (ModuleIdentifier m : modules) {
                    if (m.equals(identifier)) {
                        return ModuleSpec.build(m).create();
                    }
                }
                return null;
            }
        };

        ModuleLoader loader = new ModuleLoader(new ModuleFinder[]{fakeFinder});

        Iterator<ModuleIdentifier> it = loader.iterateModules(null, true);
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }

        Assert.assertEquals(2, count);
    }

    private JarFile toJarFile(JavaArchive archive) throws IOException {
        ZipExporter exporter = archive.as(ZipExporter.class);
        File targetFile = new File("target/shrinkwrap/" + archive.getName());
        targetFile.getParentFile().mkdirs();
        exporter.exportTo(targetFile, true);
        return new JarFile(targetFile);
    }

    private JavaArchive getModuleB() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "moduleB");
        archive.addAsManifestResource(new StringAsset("someContent"), "services/org/apache/camel/component/jms");
        return archive;
    }
}
