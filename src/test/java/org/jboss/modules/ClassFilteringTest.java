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

import org.jboss.modules.filter.ClassFilter;
import org.jboss.modules.filter.ClassFilters;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.test.BarImpl;
import org.jboss.modules.test.QuxBar;
import org.jboss.modules.test.QuxFoo;
import org.jboss.modules.test.QuxImpl;
import org.jboss.modules.util.ModulesTestBase;
import org.jboss.modules.util.TestResourceLoader;
import org.junit.Test;

import static org.jboss.modules.DependencySpec.createLocalDependencySpec;
import static org.jboss.modules.DependencySpec.createModuleDependencySpec;
import static org.jboss.modules.ResourceLoaderSpec.createResourceLoaderSpec;
import static org.jboss.modules.util.TestResourceLoader.TestResourceLoaderBuilder;

/**
 * [MODULES-69] Allow for OSGi style Class Filtering
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 28-Apr-2011
 */
public class ClassFilteringTest extends ModulesTestBase {

    @Test
    public void testClassFilter() throws Exception {
        final ModuleIdentifier identifierA = ModuleIdentifier.create(getClass().getSimpleName());

        ModuleSpec.Builder specBuilderA = ModuleSpec.build(identifierA);

        // Export-Package: com.acme.foo; include:="Qux*,BarImpl";exclude:=QuxImpl

        String packagePath = QuxBar.class.getPackage().getName().replace('.', '/');
        PathFilter inA = PathFilters.match(packagePath + "/Qux*.class");
        PathFilter inB = PathFilters.match(packagePath + "/BarImpl.class");
        PathFilter exA = PathFilters.match(packagePath + "/QuxImpl.class");

        //A class is only visible if it is:
        //    Matched with an entry in the included list, and
        //    Not matched with an entry in the excluded list.

        PathFilter in = PathFilters.any(inA, inB);
        PathFilter ex = PathFilters.not(PathFilters.any(exA));
        final PathFilter filter = PathFilters.all(in, ex);

        ClassFilter classImportFilter = ClassFilters.acceptAll();
        ClassFilter classExportFilter = ClassFilters.fromResourcePathFilter(filter);

        specBuilderA.addResourceRoot(createResourceLoaderSpec(getTestResourceLoader()));
        PathFilter importFilter = PathFilters.acceptAll();
        PathFilter exportFilter = PathFilters.acceptAll();
        PathFilter resourceImportFilter = PathFilters.acceptAll();
        PathFilter resourceExportFilter = PathFilters.acceptAll();
        specBuilderA.addDependency(createLocalDependencySpec(importFilter, exportFilter, resourceImportFilter, resourceExportFilter, classImportFilter, classExportFilter));
        addModuleSpec(specBuilderA.create());

        ModuleIdentifier identifierB = ModuleIdentifier.create("moduleB");
        ModuleSpec.Builder specBuilderB = ModuleSpec.build(identifierB);
        specBuilderB.addDependency(createModuleDependencySpec(identifierA));
        addModuleSpec(specBuilderB.create());

        assertLoadClass(identifierA, QuxFoo.class.getName());
        assertLoadClass(identifierA, QuxBar.class.getName());
        assertLoadClass(identifierA, QuxImpl.class.getName());
        assertLoadClass(identifierA, BarImpl.class.getName());

        assertLoadClass(identifierB, QuxFoo.class.getName());
        assertLoadClass(identifierB, QuxBar.class.getName());
        assertLoadClassFail(identifierB, QuxImpl.class.getName());
        assertLoadClass(identifierB, BarImpl.class.getName());
    }

    private TestResourceLoader getTestResourceLoader() throws Exception {
        TestResourceLoaderBuilder builder = new TestResourceLoaderBuilder();
        builder.addClasses(QuxBar.class, QuxFoo.class, QuxImpl.class, BarImpl.class);
        return builder.create();
    }
}
