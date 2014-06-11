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
