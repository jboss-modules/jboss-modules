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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A module is a unit of classes and other resources, along with the specification of what is imported and exported
 * by this module from and to other modules.  Modules are created by {@link ModuleLoader}s which build modules from
 * various configuration information and resource roots.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public final class Module {
    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    URL.setURLStreamHandlerFactory(new ModularURLStreamHandlerFactory());
                } catch (Throwable t) {
                    // todo log a warning or something
                }
                return null;
            }
        });
    }

    /**
     * The system module, which is always available.
     */
    public static final Module SYSTEM = new Module();

    static volatile ModuleLogger log = NoopModuleLogger.getInstance();

    private static ModuleLoaderSelector moduleLoaderSelector = ModuleLoaderSelector.DEFAULT;

    private final ModuleIdentifier identifier;
    private final String mainClassName;
    private final ModuleClassLoader moduleClassLoader;
    private final ModuleLoader moduleLoader;
    private List<Dependency> dependencies;
    private Set<DependencyExport> exportedDependencies;
    private final AtomicBoolean exportsDetermined = new AtomicBoolean();
    private Map<String, List<DependencyImport>> pathsToImports;
    private final AtomicBoolean importsDetermined = new AtomicBoolean();
    private Set<String> localExportedPaths;

    Module(final ModuleSpec spec, final Set<Flag> flags, final ModuleLoader moduleLoader) {
        this.moduleLoader = moduleLoader;
        identifier = spec.getIdentifier();
        mainClassName = spec.getMainClass();
        final ModuleContentLoader moduleContentLoader = spec.getContentLoader();
        // should be safe, so...
        //noinspection ThisEscapedInObjectConstruction
        moduleClassLoader = new ModuleClassLoader(this, flags, spec.getAssertionSetting(), moduleContentLoader);

        localExportedPaths = moduleContentLoader.getFilteredLocalPaths();
    }

    private Module() {
        identifier = ModuleIdentifier.SYSTEM;
        mainClassName = null;
        //noinspection ThisEscapedInObjectConstruction
        final SystemModuleClassLoader smcl = new SystemModuleClassLoader(this, Collections.<Flag>emptySet(), AssertionSetting.INHERIT);
        moduleClassLoader = smcl;
        localExportedPaths = smcl.getExportedPaths();
        pathsToImports = null; // bypassed by the system MCL
        moduleLoader = InitialModuleLoader.INSTANCE;
    }

    void setDependencies(final List<Dependency> dependencies) {
        if(this.dependencies != null) {
            throw new IllegalStateException("Module dependencies can only be set once");
        }
        this.dependencies = dependencies;
    }

    /**
     * Get an exported resource from a specific root in this module.
     *
     * @param rootPath the module root to search
     * @param resourcePath the path of the resource
     * @return the resource
     */
    public final Resource getExportedResource(final String rootPath, final String resourcePath) {
        return moduleClassLoader.getRawResource(rootPath, resourcePath);
    }

    /**
     * Run a module's main class, if any.
     *
     * @param args the arguments to pass
     * @throws NoSuchMethodException if there is no main method
     * @throws InvocationTargetException if the main method failed
     * @throws ClassNotFoundException if the main class is not found
     */
    public final void run(final String[] args) throws NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        try {
            if (mainClassName == null) {
                throw new NoSuchMethodException("No main class defined for " + this);
            }
            final Class<?> mainClass = moduleClassLoader.loadExportedClass(mainClassName);
            if (mainClass == null) {
                throw new NoSuchMethodException("No main class named '" + mainClassName + "' found in " + this);
            }
            final Method mainMethod = mainClass.getMethod("main", String[].class);
            final int modifiers = mainMethod.getModifiers();
            if (! Modifier.isStatic(modifiers)) {
                throw new NoSuchMethodException("Main method is not static for " + this);
            }
            // ignore the return value
            mainMethod.invoke(null, new Object[] {args});
        } catch (IllegalAccessException e) {
            // unexpected; should be public
            throw new IllegalAccessError(e.getMessage());
        }
    }

    /**
     * Get this module's identifier.
     *
     * @return the identifier
     */
    public ModuleIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * Get the module loader which created this module.
     *
     * @return the module loader of this module
     */
    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    /**
     * Load a service from this module.
     *
     * @param serviceType the service type class
     * @param <S> the service type
     * @return the service loader
     */
    public <S> ServiceLoader<S> loadService(Class<S> serviceType) {
        return ServiceLoader.load(serviceType, moduleClassLoader);
    }

    /**
     * Load a service from the named module.
     *
     * @param moduleIdentifier the module identifier
     * @param serviceType the service type class
     * @param <S> the service type
     * @return the service loader
     * @throws ModuleLoadException if the given module could not be loaded
     */
    public static <S> ServiceLoader<S> loadService(ModuleIdentifier moduleIdentifier, Class<S> serviceType) throws ModuleLoadException {
        return Module.getModule(moduleIdentifier).loadService(serviceType);
    }

    /**
     * Load a service from the named module.
     *
     * @param moduleIdentifier the module identifier
     * @param serviceType the service type class
     * @param <S> the service type
     * @return the service loader
     * @throws ModuleLoadException if the given module could not be loaded
     */
    public static <S> ServiceLoader<S> loadService(String moduleIdentifier, Class<S> serviceType) throws ModuleLoadException {
        return loadService(ModuleIdentifier.fromString(moduleIdentifier), serviceType);
    }

    /**
     * Get the class loader for a module.
     *
     * @return the module class loader
     */
    public ModuleClassLoader getClassLoader() {
        return moduleClassLoader;
    }

    /**
     * Get all the paths exported by this module.
     *
     * @return the paths that are exported by this module
     */
    public Set<String> getExportedPaths() {
        return localExportedPaths;
    }

    Map<String, List<DependencyImport>> getPathsToImports() {
        if(importsDetermined.compareAndSet(false, true)) {
            pathsToImports = new HashMap<String, List<DependencyImport>>();
            for(Dependency dependency : dependencies) {
                final Module dependencyModule = dependency.getModule();
                final PathFilter exportFilter = dependency.getExportFilter();
                final PathFilter importFilter = dependency.getImportFilter();

                final Set<DependencyExport> moduleExportedDependencies = dependencyModule.getExportedDependencies();

                for(DependencyExport dependencyExport : moduleExportedDependencies) {
                    final Module dependencyExportModule = dependencyExport.module;
                    if(dependencyExportModule.equals(this));
                    final Set<String> dependenciesLocalExports = dependencyExportModule.getExportedPaths();
                    for(String exportPath : dependenciesLocalExports) {
                        if(importFilter.accept(exportPath)) {
                            final boolean shouldExport = dependency.isExport() && exportFilter.accept(exportPath) && dependencyExport.exportFilter.accept(exportPath);
                            if(!pathsToImports.containsKey(exportPath))
                                pathsToImports.put(exportPath, new ArrayList<DependencyImport>());
                            pathsToImports.get(exportPath).add(new DependencyImport(dependencyExportModule, shouldExport));
                        }
                    }
                }

                final Set<String> dependenciesLocalExports = dependencyModule.getExportedPaths();
                for(String exportPath : dependenciesLocalExports) {
                    if(importFilter.accept(exportPath)) {
                        final boolean shouldExport = dependency.isExport() && exportFilter.accept(exportPath);
                        if(!pathsToImports.containsKey(exportPath))
                            pathsToImports.put(exportPath, new ArrayList<DependencyImport>());
                        pathsToImports.get(exportPath).add(new DependencyImport(dependencyModule, shouldExport));
                    }
                }
            }
        }
        return pathsToImports;
    }

    Set<DependencyExport> getExportedDependencies() {
        if(exportsDetermined.compareAndSet(false, true)) {
            exportedDependencies = new HashSet<DependencyExport>();
            for(Dependency dependency : dependencies) {
                if(dependency.isExport()) {
                    final Module dependencyModule = dependency.getModule();
                    exportedDependencies.add(new DependencyExport(dependencyModule, dependency.getExportFilter()));
                    final Set<DependencyExport> dependencyExports = dependencyModule.getExportedDependencies();
                    for(DependencyExport dependencyExport : dependencyExports) {
                        final Module exportModule = dependencyExport.module;
                        if(exportModule.equals(this))
                            continue;
                        exportedDependencies.add(new DependencyExport(exportModule, new DelegatingPathFilter(dependencyExport.exportFilter, dependency.getExportFilter())));
                    }
                }
            }
        }
        return exportedDependencies;
    }

    /**
     * Get the module for a loaded class, or {@code null} if the class did not come from any module.
     *
     * @param clazz the class
     * @return the module it came from
     */
    public static Module forClass(Class<?> clazz) {
        final ClassLoader cl = clazz.getClassLoader();
        return cl instanceof ModuleClassLoader ? ((ModuleClassLoader) cl).getModule() : null;
    }

    /**
     * Load a class from a module.
     *
     * @param moduleIdentifier the identifier of the module from which the class should be loaded
     * @param className the class name to load
     * @param initialize {@code true} to initialize the class
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClass(final ModuleIdentifier moduleIdentifier, final String className, final boolean initialize) throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, initialize, ModuleClassLoader.forModule(moduleIdentifier));
    }

    /**
     * Load a class from a module.  The class will be initialized.
     *
     * @param moduleIdentifier the identifier of the module from which the class should be loaded
     * @param className the class name to load
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClass(final ModuleIdentifier moduleIdentifier, final String className) throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, true, ModuleClassLoader.forModule(moduleIdentifier));
    }

    /**
     * Load a class from a module.
     *
     * @param moduleIdentifierString the identifier of the module from which the class should be loaded
     * @param className the class name to load
     * @param initialize {@code true} to initialize the class
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClass(final String moduleIdentifierString, final String className, final boolean initialize) throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, initialize, ModuleClassLoader.forModule(ModuleIdentifier.fromString(moduleIdentifierString)));
    }

    /**
     * Load a class from a module.  The class will be initialized.
     *
     * @param moduleIdentifierString the identifier of the module from which the class should be loaded
     * @param className the class name to load
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClass(final String moduleIdentifierString, final String className) throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, true, ModuleClassLoader.forModule(ModuleIdentifier.fromString(moduleIdentifierString)));
    }

    /**
     * Get the module with the given identifier from the current module loader as returned by {@link ModuleLoaderSelector#getCurrentLoader()}
     * on the current module loader selector.
     *
     * @param identifier the module identifier
     * @return the module
     * @throws ModuleLoadException if an error occurs
     */
    public static Module getModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        return moduleLoaderSelector.getCurrentLoader().loadModule(identifier);
    }

    /**
     * The enumeration of flag values which can be used to configure a module instance.
     */
    public enum Flag {
        // flags here
        /**
         * Indicates that the module's local resources should be loaded before consulting external imports.
         */
        CHILD_FIRST
    }

    /**
     * Get the string representation of this module.
     *
     * @return the string representation
     */
    public String toString() {
        return "Module \"" + identifier + "\"";
    }

    /**
     * Set the current module loader selector.
     *
     * @param moduleLoaderSelector the new selector, must not be {@code null}
     */
    public static void setModuleLoaderSelector(final ModuleLoaderSelector moduleLoaderSelector) {
        if (moduleLoaderSelector == null) {
            throw new IllegalArgumentException("moduleLoaderSelector is null");
        }
        // todo: perm check
        Module.moduleLoaderSelector = moduleLoaderSelector;
    }

    /**
     * Change the logger used by the module system.
     *
     * @param logger the new logger, must not be {@code null}
     */
    public static void setModuleLogger(final ModuleLogger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("logger is null");
        }
        // todo: perm check
        log = logger;
    }

    static final class DependencyImport {
        private final Module module;
        private final boolean export;

        DependencyImport(Module module, boolean export) {
            this.module = module;
            this.export = export;
        }

        public Module getModule() {
            return module;
        }

        public boolean isExport() {
            return export;
        }
    }

    static final class DependencyExport {
        private final Module module;
        private final PathFilter exportFilter;

        DependencyExport(Module module, PathFilter exportFilter) {
            this.module = module;
            this.exportFilter = exportFilter;
        }

        Module getModule() {
            return module;
        }
    }
}
