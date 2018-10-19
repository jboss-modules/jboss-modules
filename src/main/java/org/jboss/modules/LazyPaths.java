/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.modules.filter.ClassFilter;
import org.jboss.modules.filter.ClassFilters;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

/**
 */
final class LazyPaths {

    private final DependencySpec[] dependencySpecs;
    private final Dependency[] dependencies;
    private final ModuleLoader moduleLoader;
    private final Map<String, ListCache> map = new ConcurrentHashMap<>();

    LazyPaths(final DependencySpec[] dependencySpecs, final Dependency[] dependencies, final ModuleLoader loader) {
        this.dependencySpecs = dependencySpecs;
        this.dependencies = dependencies;
        moduleLoader = loader;
    }

    Set<String> getCurrentPathSet() {
        return map.keySet();
    }

    Dependency[] getDependencies() {
        return dependencies;
    }

    DependencySpec[] getDependencySpecs() {
        return dependencySpecs;
    }

    ListCache getLoaders(String name) {
        ListCache loaders = map.get(name);
        if (loaders != null) {
            return loaders;
        }
        loaders = moduleLoader.getListCacheRoot();
        for (Dependency dependency : dependencies) {
            loaders = processDependency(name, false, dependency, loaders, new HashSet<>(), ClassFilters.acceptAll(), PathFilters.acceptAll());
        }
        final ListCache appearing = map.putIfAbsent(name, loaders);
        if (appearing != null) {
            return appearing;
        }
        return loaders;
    }

    private static PathFilter combine(PathFilter filter1, PathFilter filter2) {
        if (filter1 == PathFilters.acceptAll()) return filter2;
        if (filter2 == PathFilters.acceptAll()) return filter1;
        return PathFilters.all(filter1, filter2);
    }

    private static ClassFilter combine(ClassFilter filter1, ClassFilter filter2) {
        if (filter1 == ClassFilters.acceptAll()) return filter2;
        if (filter2 == ClassFilters.acceptAll()) return filter1;
        return new ClassFilter() {
            @Override
            public boolean accept(final String className) {
                return filter1.accept(className) && filter2.accept(className);
            }
        };
    }

    private ListCache processDependency(String name, boolean export, Dependency dependency, ListCache loaders, HashSet<Module> visited, ClassFilter classFilter, PathFilter resourceFilter) {
        if (! (dependency.getImportFilter().accept(name) && (! export || dependency.getExportFilter().accept(name)))) {
            return loaders;
        }
        classFilter = combine(classFilter, dependency.getClassImportFilter());
        if (export) classFilter = combine(classFilter, dependency.getClassExportFilter());
        resourceFilter = combine(resourceFilter, dependency.getResourceImportFilter());
        if (export) resourceFilter = combine(resourceFilter, dependency.getResourceExportFilter());
        if (dependency instanceof ModuleDependency) {
            final ModuleDependency moduleDependency = (ModuleDependency) dependency;
            final ModuleLoader moduleLoader = moduleDependency.getModuleLoader();
            final String moduleName = moduleDependency.getName();
            final boolean optional = moduleDependency.isOptional();
            final Module module;
            try {
                module = moduleLoader.loadModule(moduleName);
                if (visited.add(module)) {
                    final Dependency[] dependencies = module.getDependenciesInternal();
                    for (Dependency nestedDependency : dependencies) {
                        loaders = processDependency(name, true, nestedDependency, loaders, visited, classFilter, resourceFilter);
                    }
                }
            } catch (ModuleLoadException e) {
                if (! optional) {
                    throw e.toError();
                }
            }
        } else if (dependency instanceof LocalDependency) {
            final LocalDependency localDependency = (LocalDependency) dependency;
            if (localDependency.getPaths().contains(name)) {
                LocalLoader localLoader = localDependency.getLocalLoader();
                if (classFilter != ClassFilters.acceptAll() || resourceFilter != PathFilters.acceptAll()) {
                    localLoader = LocalLoaders.createFilteredLocalLoader(classFilter, resourceFilter, localLoader);
                }
                loaders = loaders.getChild(localLoader);
            }
        } else if (dependency instanceof ModuleClassLoaderDependency) {
            final ModuleClassLoaderDependency moduleClassLoaderDependency = (ModuleClassLoaderDependency) dependency;
            if (moduleClassLoaderDependency.getPaths().contains(name)) {
                LocalLoader localLoader = moduleClassLoaderDependency.getLocalLoader();
                if (classFilter != ClassFilters.acceptAll() || resourceFilter != PathFilters.acceptAll()) {
                    localLoader = LocalLoaders.createFilteredLocalLoader(classFilter, resourceFilter, localLoader);
                }
                loaders = loaders.getChild(localLoader);
            }
        } else {
            throw new IllegalStateException();
        }
        return loaders;
    }
}
