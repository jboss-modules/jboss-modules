/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.modules.util;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleContentLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleSpec;

/**
 * Builder API to build test module specs that can be installed at runtime without requiring the module to exist
 * on disk.
 * 
 * @author John E. Bailey
 */
public class ModuleSpecBuilder {

    private final ModuleSpec moduleSpec;
    private final ModuleContentLoader.Builder moduleContentLoaderBuilder;
    private final TestModuleLoader testModuleLoader;

    ModuleSpecBuilder(final ModuleIdentifier moduleIdentifier, final TestModuleLoader testModuleLoader) {
        moduleSpec = new ModuleSpec(moduleIdentifier);
        moduleContentLoaderBuilder = ModuleContentLoader.build();
        this.testModuleLoader = testModuleLoader;
    }

    public ModuleSpecBuilder setMainClass(final String mainClass) {
        moduleSpec.setMainClass(mainClass);
        return this;
    }

    public DependencySpecBuilder addDependency(final ModuleIdentifier moduleIdentifier) {
        final DependencySpecBuilder dependencySpecBuilder = new DependencySpecBuilder(moduleIdentifier);
        moduleSpec.getDependencies().add(dependencySpecBuilder.dependencySpec);
        return dependencySpecBuilder;
    }

    public TestResourceLoader.TestResourceLoaderBuilder addRoot(final String rootName) {
        final TestResourceLoader.TestResourceLoaderBuilder resourceRootBuilder = TestResourceLoader.build();
        moduleContentLoaderBuilder.add(rootName, resourceRootBuilder.create());
        return resourceRootBuilder;
    }


    public void install() {
        moduleSpec.setContentLoader(moduleContentLoaderBuilder.create());
        testModuleLoader.addModuleSpec(moduleSpec);
    }

    public static class DependencySpecBuilder {
        private final DependencySpec dependencySpec;

        public DependencySpecBuilder(final ModuleIdentifier moduleIdentifier) {
            this.dependencySpec = new DependencySpec();
            dependencySpec.setModuleIdentifier(moduleIdentifier);
        }

        public DependencySpecBuilder setExport(final boolean export) {
            dependencySpec.setExport(export);
            return this;
        }

        public DependencySpecBuilder setOptional(final boolean optional) {
            dependencySpec.setOptional(optional);
            return this;
        }

        public DependencySpecBuilder addExportInclude(final String path) {
            dependencySpec.addExportInclude(path);
            return this;
        }

        public DependencySpecBuilder addExportExclude(final String path) {
            dependencySpec.addExportExclude(path);
            return this;
        }
    }
}
