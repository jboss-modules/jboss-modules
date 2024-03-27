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

import java.util.Set;
import org.jboss.modules.filter.ClassFilter;
import org.jboss.modules.filter.ClassFilters;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

/**
 * A dependency specification that represents a single dependency for a module.  The dependency can be on a local loader
 * or another module, or on the target module's local loader.
 *
 * @apiviz.exclude
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author Jason T. Greene
 */
public abstract class DependencySpec {

    static final DependencySpec[] NO_DEPENDENCIES = new DependencySpec[0];
    final PathFilter importFilter;
    final PathFilter exportFilter;
    final PathFilter resourceImportFilter;
    final PathFilter resourceExportFilter;
    final ClassFilter classImportFilter;
    final ClassFilter classExportFilter;

    /**
     * Get the dependency import filter.
     *
     * @return the import filter
     */
    public PathFilter getImportFilter() {
        return importFilter;
    }

    /**
     * Get the dependency export filter.
     *
     * @return the export filter
     */
    public PathFilter getExportFilter() {
        return exportFilter;
    }

    /**
     * Get the dependency resource import filter.
     *
     * @return the import filter
     */
    public PathFilter getResourceImportFilter() {
        return resourceImportFilter;
    }

    /**
     * Get the dependency resource export filter.
     *
     * @return the export filter
     */
    public PathFilter getResourceExportFilter() {
        return resourceExportFilter;
    }

    /**
     * Get the dependency class import filter.
     *
     * @return the class import filter
     */
    public ClassFilter getClassImportFilter() {
        return classImportFilter;
    }

    /**
     * Get the dependency class export filter.
     *
     * @return the class export filter
     */
    public ClassFilter getClassExportFilter() {
        return classExportFilter;
    }

    DependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final PathFilter resourceImportFilter, final PathFilter resourceExportFilter, final ClassFilter classImportFilter, final ClassFilter classExportFilter) {
        this.importFilter = importFilter;
        this.exportFilter = exportFilter;
        this.resourceImportFilter = resourceImportFilter;
        this.resourceExportFilter = resourceExportFilter;
        this.classImportFilter = classImportFilter;
        this.classExportFilter = classExportFilter;
    }

    abstract Dependency getDependency(final Module module);

    /**
     * Create a dependency on the current module's local resources.  You should have at least one such dependency
     * on any module which has its own resources.  Always returns {@link #OWN_DEPENDENCY}.
     *
     * @return the dependency spec
     */
    public static DependencySpec createLocalDependencySpec() {
        return OWN_DEPENDENCY;
    }

    /**
     * Create a dependency on the current module's local resources.  You should have at least one such dependency
     * on any module which has its own resources.
     *
     * @param importFilter the import filter to apply
     * @param exportFilter the export filter to apply
     * @return the dependency spec
     *
     * @deprecated Use {@link LocalDependencySpecBuilder} instead.
     */
    @Deprecated
    public static DependencySpec createLocalDependencySpec(final PathFilter importFilter, final PathFilter exportFilter) {
        return new LocalDependencySpecBuilder()
            .setImportFilter(importFilter)
            .setExportFilter(exportFilter)
            .build();
    }

    /**
     * Create a dependency on the current module's local resources.  You should have at least one such dependency
     * on any module which has its own resources.
     *
     * @param importFilter the import filter to apply
     * @param exportFilter the export filter to apply
     * @param resourceImportFilter the resource import filter to apply
     * @param resourceExportFilter the resource export filter to apply
     * @param classImportFilter the class import filter to apply
     * @param classExportFilter the class export filter to apply
     * @return the dependency spec
     *
     * @deprecated Use {@link LocalDependencySpecBuilder} instead.
     */
    @Deprecated
    public static DependencySpec createLocalDependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final PathFilter resourceImportFilter, final PathFilter resourceExportFilter, final ClassFilter classImportFilter, final ClassFilter classExportFilter) {
        return new LocalDependencySpecBuilder()
            .setImportFilter(importFilter)
            .setExportFilter(exportFilter)
            .setResourceImportFilter(resourceImportFilter)
            .setResourceExportFilter(resourceExportFilter)
            .setClassImportFilter(classImportFilter)
            .setClassExportFilter(classExportFilter)
            .build();
    }

    /**
     * Create a system dependency.
     *
     * @param loaderPaths the set of paths to use from the system class loader
     * @return the dependency spec
     */
    public static DependencySpec createSystemDependencySpec(final Set<String> loaderPaths) {
        return new LocalDependencySpecBuilder()
            .setImportFilter(PathFilters.acceptAll())
            .setLocalLoader(ClassLoaderLocalLoader.SYSTEM)
            .setLoaderPaths(loaderPaths)
            .build();
    }

    /**
     * Create a system dependency.
     *
     * @param loaderPaths the set of paths to use from the system class loader
     * @param export {@code true} if this is a fully re-exported dependency, {@code false} if it should not be exported
     * @return the dependency spec
     */
    public static DependencySpec createSystemDependencySpec(final Set<String> loaderPaths, boolean export) {
        return new LocalDependencySpecBuilder()
            .setLocalLoader(ClassLoaderLocalLoader.SYSTEM)
            .setImportFilter(PathFilters.acceptAll())
            .setLoaderPaths(loaderPaths)
            .setExport(export)
            .build();
    }

    /**
     * Create a system dependency.
     *
     * @param importFilter the import filter to apply
     * @param exportFilter the export filter to apply
     * @param loaderPaths the set of paths to use from the system class loader
     * @return the dependency spec
     */
    public static DependencySpec createSystemDependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final Set<String> loaderPaths) {
        return new LocalDependencySpecBuilder()
            .setImportFilter(importFilter)
            .setExportFilter(exportFilter)
            .setLocalLoader(ClassLoaderLocalLoader.SYSTEM)
            .setLoaderPaths(loaderPaths)
            .build();
    }

    /**
     * Create a dependency on the given class loader.
     *
     * @param classLoader the class loader
     * @param loaderPaths the set of paths to use from this class loader
     * @return the dependency spec
     *
     * @deprecated Use {@link LocalDependencySpecBuilder} instead.
     */
    @Deprecated
    public static DependencySpec createClassLoaderDependencySpec(final ClassLoader classLoader, final Set<String> loaderPaths) {
        return createLocalDependencySpec(new ClassLoaderLocalLoader(classLoader), loaderPaths);
    }

    /**
     * Create a dependency on the given class loader.
     *
     * @param classLoader the class loader
     * @param loaderPaths the set of paths to use from this class loader
     * @param export {@code true} if this is a fully re-exported dependency, {@code false} if it should not be exported
     * @return the dependency spec
     *
     * @deprecated Use {@link LocalDependencySpecBuilder} instead.
     */
    @Deprecated
    public static DependencySpec createClassLoaderDependencySpec(final ClassLoader classLoader, final Set<String> loaderPaths, boolean export) {
        return createLocalDependencySpec(new ClassLoaderLocalLoader(classLoader), loaderPaths, export);
    }

    /**
     * Create a dependency on the given class loader.
     *
     * @param importFilter the import filter to apply
     * @param exportFilter the export filter to apply
     * @param classLoader the class loader
     * @param loaderPaths the set of paths to use from this class loader
     * @return the dependency spec
     *
     * @deprecated Use {@link LocalDependencySpecBuilder} instead.
     */
    @Deprecated
    public static DependencySpec createClassLoaderDependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final ClassLoader classLoader, final Set<String> loaderPaths) {
        return createLocalDependencySpec(importFilter, exportFilter, new ClassLoaderLocalLoader(classLoader), loaderPaths);
    }

    /**
     * Create a dependency on the given local loader.
     *
     * @param localLoader the local loader
     * @param loaderPaths the set of paths that is exposed by the local loader
     * @return the dependency spec
     *
     * @deprecated Use {@link LocalDependencySpecBuilder} instead.
     */
    @Deprecated
    public static DependencySpec createLocalDependencySpec(final LocalLoader localLoader, final Set<String> loaderPaths) {
        return new LocalDependencySpecBuilder()
            .setLocalLoader(localLoader)
            .setLoaderPaths(loaderPaths)
            .build();
    }

    /**
     * Create a dependency on the given local loader.
     *
     * @param localLoader the local loader
     * @param loaderPaths the set of paths that is exposed by the local loader
     * @param export {@code true} if this is a fully re-exported dependency, {@code false} if it should not be exported
     * @return the dependency spec
     *
     * @deprecated Use {@link LocalDependencySpecBuilder} instead.
     */
    @Deprecated
    public static DependencySpec createLocalDependencySpec(final LocalLoader localLoader, final Set<String> loaderPaths, boolean export) {
        return new LocalDependencySpecBuilder()
            .setLocalLoader(localLoader)
            .setLoaderPaths(loaderPaths)
            .setExport(export)
            .build();
    }

    /**
     * Create a dependency on the given local loader.
     *
     * @param importFilter the import filter to apply
     * @param exportFilter the export filter to apply
     * @param localLoader the local loader
     * @param loaderPaths the set of paths that is exposed by the local loader
     * @return the dependency spec
     *
     * @deprecated Use {@link LocalDependencySpecBuilder} instead.
     */
    @Deprecated
    public static DependencySpec createLocalDependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final LocalLoader localLoader, final Set<String> loaderPaths) {
        return new LocalDependencySpecBuilder()
            .setImportFilter(importFilter)
            .setExportFilter(exportFilter)
            .setLocalLoader(localLoader)
            .setLoaderPaths(loaderPaths)
            .build();
    }

    /**
     * Create a dependency on the given local loader.
     *
     * @param importFilter the import filter to apply
     * @param exportFilter the export filter to apply
     * @param resourceImportFilter the resource import filter to apply
     * @param resourceExportFilter the resource export filter to apply
     * @param classImportFilter the class import filter to apply
     * @param classExportFilter the class export filter to apply
     * @param localLoader the local loader
     * @param loaderPaths the set of paths that is exposed by the local loader
     * @return the dependency spec
     *
     * @deprecated Use {@link LocalDependencySpecBuilder} instead.
     */
    @Deprecated
    public static DependencySpec createLocalDependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final PathFilter resourceImportFilter, final PathFilter resourceExportFilter, final ClassFilter classImportFilter, final ClassFilter classExportFilter, final LocalLoader localLoader, final Set<String> loaderPaths) {
        return new LocalDependencySpecBuilder()
            .setImportFilter(importFilter)
            .setExportFilter(exportFilter)
            .setResourceImportFilter(resourceImportFilter)
            .setResourceExportFilter(resourceExportFilter)
            .setClassImportFilter(classImportFilter)
            .setClassExportFilter(classExportFilter)
            .setLocalLoader(localLoader)
            .setLoaderPaths(loaderPaths)
            .build();
    }

    /**
     * Create a dependency on the given module.
     *
     * @param name the module name
     * @return the dependency spec
     *
     * @deprecated Use {@link ModuleDependencySpecBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public static DependencySpec createModuleDependencySpec(final String name) {
        return new ModuleDependencySpecBuilder()
            .setName(name)
            .build();
    }

    /**
     * Create a dependency on the given module.
     *
     * @param name the module name
     * @param export {@code true} if the dependency should be exported by default
     * @return the dependency spec
     *
     * @deprecated Use {@link ModuleDependencySpecBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public static DependencySpec createModuleDependencySpec(final String name, final boolean export) {
        return new ModuleDependencySpecBuilder()
            .setName(name)
            .setExport(export)
            .build();
    }

    /**
     * Create a dependency on the given module.
     *
     * @param name the module name
     * @param export {@code true} if this is a fully re-exported dependency, {@code false} if it should not be exported
     * @param optional {@code true} if the dependency is optional, {@code false} if it is mandatory
     * @return the dependency spec
     *
     * @deprecated Use {@link ModuleDependencySpecBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public static DependencySpec createModuleDependencySpec(final String name, final boolean export, final boolean optional) {
        return new ModuleDependencySpecBuilder()
            .setName(name)
            .setExport(export)
            .setOptional(optional)
            .build();
    }

    /**
     * Create a dependency on the given module.
     *
     * @param moduleLoader the specific module loader from which the module should be acquired
     * @param name the module name
     * @param export {@code true} if this is a fully re-exported dependency, {@code false} if it should not be exported
     * @return the dependency spec
     *
     * @deprecated Use {@link ModuleDependencySpecBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public static DependencySpec createModuleDependencySpec(final ModuleLoader moduleLoader, final String name, final boolean export) {
        return new ModuleDependencySpecBuilder()
            .setModuleLoader(moduleLoader)
            .setName(name)
            .setExport(export)
            .build();
    }

    /**
     * Create a dependency on the given module.
     *
     * @param moduleLoader the specific module loader from which the module should be acquired
     * @param name the module name
     * @param export {@code true} if this is a fully re-exported dependency, {@code false} if it should not be exported
     * @param optional {@code true} if the dependency is optional, {@code false} if it is mandatory
     * @return the dependency spec
     *
     * @deprecated Use {@link ModuleDependencySpecBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public static DependencySpec createModuleDependencySpec(final ModuleLoader moduleLoader, final String name, final boolean export, final boolean optional) {
        return new ModuleDependencySpecBuilder()
            .setModuleLoader(moduleLoader)
            .setName(name)
            .setExport(export)
            .setOptional(optional)
            .build();
    }

    /**
     * Create a dependency on the given module.
     *
     * @param exportFilter the export filter to apply
     * @param name the module name
     * @param optional {@code true} if the dependency is optional, {@code false} if it is mandatory
     * @return the dependency spec
     *
     * @deprecated Use {@link ModuleDependencySpecBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public static DependencySpec createModuleDependencySpec(final PathFilter exportFilter, final String name, final boolean optional) {
        return new ModuleDependencySpecBuilder()
            .setExportFilter(exportFilter)
            .setName(name)
            .setOptional(optional)
            .build();
    }

    /**
     * Create a dependency on the given module.
     *
     * @param exportFilter the export filter to apply
     * @param moduleLoader the specific module loader from which the module should be acquired
     * @param name the module name
     * @param optional {@code true} if the dependency is optional, {@code false} if it is mandatory
     * @return the dependency spec
     *
     * @deprecated Use {@link ModuleDependencySpecBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public static DependencySpec createModuleDependencySpec(final PathFilter exportFilter, final ModuleLoader moduleLoader, final String name, final boolean optional) {
        return new ModuleDependencySpecBuilder()
            .setExportFilter(exportFilter)
            .setModuleLoader(moduleLoader)
            .setName(name)
            .setOptional(optional)
            .build();
    }

    /**
     * Create a dependency on the given module.
     *
     * @param importFilter the import filter to apply
     * @param exportFilter the export filter to apply
     * @param moduleLoader the specific module loader from which the module should be acquired
     * @param name the module name
     * @param optional {@code true} if the dependency is optional, {@code false} if it is mandatory
     * @return the dependency spec
     *
     * @deprecated Use {@link ModuleDependencySpecBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public static DependencySpec createModuleDependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final ModuleLoader moduleLoader, final String name, final boolean optional) {
        return new ModuleDependencySpecBuilder()
            .setImportFilter(importFilter)
            .setExportFilter(exportFilter)
            .setModuleLoader(moduleLoader)
            .setName(name)
            .setOptional(optional)
            .build();
    }

    /**
     * Create a dependency on the given module.
     *
     * @param importFilter the import filter to apply
     * @param exportFilter the export filter to apply
     * @param resourceImportFilter the resource import filter to apply
     * @param resourceExportFilter the resource export filter to apply
     * @param classImportFilter the class import filter to apply
     * @param classExportFilter the class export filter to apply
     * @param moduleLoader the specific module loader from which the module should be acquired
     * @param name the module name
     * @param optional {@code true} if the dependency is optional, {@code false} if it is mandatory
     * @return the dependency spec
     *
     * @deprecated Use {@link ModuleDependencySpecBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public static DependencySpec createModuleDependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final PathFilter resourceImportFilter, final PathFilter resourceExportFilter, final ClassFilter classImportFilter, final ClassFilter classExportFilter, final ModuleLoader moduleLoader, final String name, final boolean optional) {
        return new ModuleDependencySpecBuilder()
            .setImportFilter(importFilter)
            .setExportFilter(exportFilter)
            .setResourceImportFilter(resourceImportFilter)
            .setResourceExportFilter(resourceExportFilter)
            .setClassImportFilter(classImportFilter)
            .setClassExportFilter(classExportFilter)
            .setModuleLoader(moduleLoader)
            .setName(name)
            .setOptional(optional)
            .build();
    }

    /**
     * A constant dependency which always represents a module's own content.
     */
    public static final DependencySpec OWN_DEPENDENCY = new LocalDependencySpecBuilder().setExportFilter(PathFilters.acceptAll()).build();
}
