/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.modules.test.ClassA;
import org.jboss.modules.test.ClassB;
import org.jboss.modules.util.ModulesTestBase;
import org.jboss.modules.util.TestResourceLoader;
import org.junit.Test;

import static org.jboss.modules.DependencySpec.createModuleDependencySpec;
import static org.jboss.modules.ResourceLoaderSpec.createResourceLoaderSpec;
import static org.jboss.modules.util.TestResourceLoader.TestResourceLoaderBuilder;

/**
 * [MODULES-140] Cannot define class with split packages
 *
 * @author Thomas.Diesler@jboss.com
 * @since 12-Jul-2012
 */
public class SplitPackagesTest extends ModulesTestBase {

    @Test
    public void testSplitPackage() throws Exception {

        // moduleA and moduleB contain ClassA and ClassB respectivly
        // Both classes come from the same package
        // ClassA extends ClassB

        ModuleIdentifier identifierA = ModuleIdentifier.create("moduleA");
        ModuleSpec.Builder specBuilderA = ModuleSpec.build(identifierA);
        specBuilderA.addResourceRoot(createResourceLoaderSpec(getResourceLoaderA()));
        addModuleSpec(specBuilderA.create());

        ModuleIdentifier identifierB = ModuleIdentifier.create("moduleB");
        ModuleSpec.Builder specBuilderB = ModuleSpec.build(identifierB);
        specBuilderB.addDependency(createModuleDependencySpec(identifierA));
        specBuilderB.addResourceRoot(createResourceLoaderSpec(getResourceLoaderB()));
        addModuleSpec(specBuilderB.create());

        assertLoadClass(identifierA, ClassA.class.getName());
    }

    private TestResourceLoader getResourceLoaderA() throws Exception {
        TestResourceLoaderBuilder builder = new TestResourceLoaderBuilder();
        builder.addClasses(ClassA.class);
        return builder.create();
    }

    private TestResourceLoader getResourceLoaderB() throws Exception {
        TestResourceLoaderBuilder builder = new TestResourceLoaderBuilder();
        builder.addClasses(ClassB.class);
        return builder.create();
    }
}
