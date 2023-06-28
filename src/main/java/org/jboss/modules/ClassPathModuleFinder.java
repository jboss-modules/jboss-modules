/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.jar.Attributes;

import org.jboss.modules.filter.PathFilters;

/**
 * The special module finder to handle launching from the actual class path.
 */
final class ClassPathModuleFinder extends FileSystemClassPathModuleFinder {
    private final String[] classPath;
    private final String[] dependencies;
    private final String mainClass;

    ClassPathModuleFinder(final Supplier<ModuleLoader> baseModuleLoaderSupplier, final String[] classPath, final String[] dependencies, final String mainClass) {
        super(baseModuleLoaderSupplier, EMPTY_MODULE_LOADER_SUPPLIER);
        this.classPath = classPath;
        this.dependencies = dependencies;
        this.mainClass = mainClass;
    }

    ClassPathModuleFinder(final ModuleLoader baseModuleLoader, final String[] classpath, final String dependencies, final String mainClass) {
        this(new SimpleSupplier<>(baseModuleLoader), classpath, dependencies == null ? Utils.NO_STRINGS : dependencies.split(","), mainClass);
    }

    public ModuleSpec findModule(final String name, final ModuleLoader delegateLoader) throws ModuleLoadException {
        if (name.equals("<classpath>")) {
            // special initial module
            ModuleSpec.Builder builder = ModuleSpec.build(name);
            for (String dependency : dependencies) {
                builder.addDependency(new ModuleDependencySpecBuilder()
                    .setImportFilter(PathFilters.acceptAll())
                    .setExportFilter(PathFilters.acceptAll())
                    .setName(dependency)
                    .build());
            }
            for (String item : classPath) {
                builder.addDependency(new ModuleDependencySpecBuilder()
                    .setImportFilter(PathFilters.acceptAll())
                    .setExportFilter(PathFilters.acceptAll())
                    .setName(item)
                    .setOptional(true)
                    .build());
            }
            if (mainClass != null) {
                builder.setMainClass(mainClass);
            }
            addSystemDependencies(builder);
            return builder.create();
        } else {
            return super.findModule(name, delegateLoader);
        }
    }

    void addExtensionDependencies(final ModuleSpec.Builder builder, final Attributes mainAttributes, final ModuleLoader extensionModuleLoader) {
        // not supported
    }

    void addModuleDependencies(final ModuleSpec.Builder builder, final ModuleLoader fatModuleLoader, final Attributes mainAttributes) {
        for (String dependency : dependencies) {
            builder.addDependency(new ModuleDependencySpecBuilder()
                .setImportFilter(PathFilters.acceptAll())
                .setModuleLoader(fatModuleLoader)
                .setName(dependency)
                .build());
        }
        super.addModuleDependencies(builder, fatModuleLoader, mainAttributes);
    }

    void addClassPathDependencies(final ModuleSpec.Builder builder, final ModuleLoader moduleLoader, final Path path, final Attributes mainAttributes) {
        // add the class path items in order, just like a real class path would
        for (String item : classPath) {
            if (item.equals(builder.getName())) {
                builder.addDependency(DependencySpec.OWN_DEPENDENCY);
            } else {
                builder.addDependency(new ModuleDependencySpecBuilder()
                    .setName(item)
                    .setOptional(true)
                    .build());
            }
        }
    }

    void addSelfDependency(final ModuleSpec.Builder builder) {
        // not supported
    }

    void setMainClass(final ModuleSpec.Builder builder, final Attributes mainAttributes) {
        // every module gets the same main class!
        if (mainClass != null) {
            builder.setMainClass(mainClass);
        }
    }
}
