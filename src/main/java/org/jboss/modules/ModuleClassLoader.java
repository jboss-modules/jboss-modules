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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A module classloader.  Instances of this class implement the complete view of classes and resources available in a
 * module.  Contrast with {@link Module}, which has API methods to access the exported view of classes and resources.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author thomas.diesler@jboss.com
 */
public final class ModuleClassLoader extends ConcurrentClassLoader {

    static {
        try {
            final Method method = ClassLoader.class.getMethod("registerAsParallelCapable");
            method.invoke(null);
        } catch (Exception e) {
            // ignore
        }
    }

    private final Module module;
    private volatile Paths<ResourceLoader> paths = Paths.none();
    private final Collection<ResourceLoader> resourceLoaders;
    private final LocalLoader localLoader = new LocalLoader() {
        public Class<?> loadClassLocal(final String name, final boolean resolve) {
            try {
                return ModuleClassLoader.this.loadClassLocal(name, false, resolve);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        public List<Resource> loadResourceLocal(final String name) {
            return ModuleClassLoader.this.loadResourceLocal(name, false);
        }

        public Resource loadResourceLocal(final String root, final String name) {
            return ModuleClassLoader.this.loadResourceLocal(root, name, false);
        }
    };
    private final PathFilter exportPathFilter = new PathFilter() {
        public boolean accept(final String path) {
            return paths.getExportedPaths().containsKey(path);
        }
    };

    /**
     * Construct a new instance.  The collection objects passed in then belong to this class loader instance.
     *
     * @param module the module
     * @param setting the assertion setting
     * @param resourceLoaders the collection of resource loaders
     */
    ModuleClassLoader(final Module module, final AssertionSetting setting, final Collection<ResourceLoader> resourceLoaders) {
        this.module = module;
        this.resourceLoaders = resourceLoaders;
        if (setting != AssertionSetting.INHERIT) {
            setDefaultAssertionStatus(setting == AssertionSetting.ENABLED);
        }
    }

    /**
     * Recalculate the path maps for this module class loader.
     */
    void recalculate() {
        final Map<String, List<ResourceLoader>> exportedPaths = new HashMap<String, List<ResourceLoader>>();
        final Map<String, List<ResourceLoader>> allPaths = new HashMap<String, List<ResourceLoader>>();
        for (ResourceLoader loader : resourceLoaders) {
            final PathFilter exportFilter = loader.getExportFilter();
            for (String path : loader.getPaths()) {
                final List<ResourceLoader> allLoaders = allPaths.get(path);
                if (allLoaders == null) {
                    allPaths.put(path, new ArrayList<ResourceLoader>(Collections.singleton(loader)));
                } else {
                    allLoaders.add(loader);
                }
                if (exportFilter.accept(path)) {
                    final List<ResourceLoader> exportedLoaders = exportedPaths.get(path);
                    if (exportedLoaders == null) {
                        exportedPaths.put(path, new ArrayList<ResourceLoader>(Collections.singleton(loader)));
                    } else {
                        exportedLoaders.add(loader);
                    }
                }
            }
        }
        paths = new Paths<ResourceLoader>(allPaths, exportedPaths);
    }

    LocalLoader getLocalLoader() {
        return localLoader;
    }

    PathFilter getExportPathFilter() {
        return exportPathFilter;
    }

    /** {@inheritDoc} */
    protected Class<?> findClass(String className, boolean exportsOnly, final boolean resolve) throws ClassNotFoundException {
        // Check if we have already loaded it..
        Class<?> loadedClass = findLoadedClass(className);
        if (loadedClass != null) {
            return loadedClass;
        }
        final ModuleLogger log = Module.log;
        final Module module = this.module;
        log.trace("Finding class %s from %s", className, module);

        final Class<?> clazz = module.loadModuleClass(className, exportsOnly, resolve);

        if (clazz != null) {
            return clazz;
        }

        log.trace("Class %s not found from %s", className, module);

        throw new ClassNotFoundException(className + " from [" + module + "]");
    }

    /**
     * Load a local class from this class loader.
     *
     * @param className the class name
     * @param exportOnly {@code true} to consider only exports
     * @param resolve {@code true} to resolve the loaded class
     * @return the loaded class or {@code null} if it was not found
     * @throws ClassNotFoundException if an error occurs while loading the class
     */
    Class<?> loadClassLocal(final String className, final boolean exportOnly, final boolean resolve) throws ClassNotFoundException {
        final ModuleLogger log = Module.log;
        final Module module = this.module;
        log.trace("Finding local class %s from %s", className, module);

        // Check if we have already loaded it..
        Class<?> loadedClass = findLoadedClass(className);
        if (loadedClass != null) {
            log.trace("Found previously loaded %s from %s", loadedClass, module);
            return loadedClass;
        }

        final Map<String, List<ResourceLoader>> paths = this.paths.getPaths(exportOnly);

        log.trace("Loading class %s locally from %s", className, module);

        final String path = Module.pathOfClass(className);

        final List<ResourceLoader> loaders = paths.get(path);
        if (loaders == null) {
            // no loaders for this path
            return null;
        }

        // Check to see if we can define it locally it
        ClassSpec classSpec = null;
        try {
            for (ResourceLoader loader : loaders) {
                classSpec = loader.getClassSpec(className);
                if (classSpec != null) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(className, e);
        } catch (RuntimeException e) {
            log.trace(e, "Unexpected runtime exception in module loader");
            throw new ClassNotFoundException(className, e);
        } catch (Error e) {
            log.trace(e, "Unexpected error in module loader");
            throw new ClassNotFoundException(className, e);
        }

        if (classSpec == null) {
            log.trace("No local specification found for class %s in %s", className, module);
            return null;
        }

        final Class<?> clazz = defineClass(className, classSpec);
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    /**
     * Load a local resource from a specific root from this module class loader.
     *
     * @param root the root name
     * @param name the resource name
     * @param exportsOnly {@code true} to only include exported paths
     * @return the resource, or {@code null} if it was not found
     */
    Resource loadResourceLocal(final String root, final String name, final boolean exportsOnly) {

        final Map<String, List<ResourceLoader>> paths = this.paths.getPaths(exportsOnly);

        final String path = Module.pathOf(name);

        final List<ResourceLoader> loaders = paths.get(path);
        if (loaders == null) {
            // no loaders for this path
            return null;
        }

        for (ResourceLoader loader : loaders) {
            if (root.equals(loader.getRootName())) {
                return loader.getResource(name);
            }
        }

        return null;
    }

    /**
     * Load a local resource from this class loader.
     *
     * @param name the resource name
     * @param exportsOnly {@code true} to only consider exported resource paths
     * @return the list of resources
     */
    List<Resource> loadResourceLocal(final String name, final boolean exportsOnly) {
        final Map<String, List<ResourceLoader>> paths = this.paths.getPaths(exportsOnly);

        final String path = Module.pathOf(name);

        final List<ResourceLoader> loaders = paths.get(path);
        if (loaders == null) {
            // no loaders for this path
            return Collections.emptyList();
        }

        final List<Resource> list = new ArrayList<Resource>(loaders.size());
        for (ResourceLoader loader : loaders) {
            final Resource resource = loader.getResource(name);
            if (resource != null) {
                list.add(resource);
            }
        }
        return list.isEmpty() ? Collections.<Resource>emptyList() : list;
    }

    /**
     * Define a class from a class name and class spec.  Also defines any enclosing {@link Package} instances,
     * and performs any sealed-package checks.
     *
     * @param name the class name
     * @param classSpec the class spec
     * @return the new class
     */
    private Class<?> defineClass(final String name, final ClassSpec classSpec) {
        final ModuleLogger log = Module.log;
        final Module module = this.module;
        log.trace("Attempting to define class %s in %s", name, module);

        // Ensure that the package is loaded
        final int lastIdx = name.lastIndexOf('.');
        if (lastIdx != -1) {
            // there's a package name; get the Package for it
            final String packageName = name.substring(0, lastIdx);
            final Package pkg = getPackage(packageName);
            if (pkg != null) {
                // Package is defined already
                if (pkg.isSealed() && ! pkg.isSealed(classSpec.getCodeSource().getLocation())) {
                    log.trace("Detected a sealing violation (attempt to define class %s in sealed package %s in %s)", name, packageName, module);
                    // use the same message as the JDK
                    throw new SecurityException("sealing violation: package " + packageName + " is sealed");
                }
            } else {
                final Map<String, List<ResourceLoader>> paths = this.paths.getAllPaths();
                final String path = Module.pathOf(name);
                final List<ResourceLoader> loaders = paths.get(path);
                if (loaders != null) {
                    PackageSpec spec = null;
                    for (ResourceLoader loader : loaders) {
                        try {
                            spec = loader.getPackageSpec(name);
                            if (spec != null) {
                                break;
                            }
                        } catch (IOException e) {
                            // skip
                        }
                    }
                    if (spec != null) {
                        definePackage(packageName, spec);
                    } else {
                        definePackage(packageName, null);
                    }
                } else {
                    definePackage(packageName, null);
                }
            }
        }
        final Class<?> newClass;
        try {
            final byte[] bytes = classSpec.getBytes();
            newClass = defineClass(name, bytes, 0, bytes.length, classSpec.getCodeSource());
        } catch (Error e) {
            log.trace(e, "Failed to define class %s in %s", name, module);
            throw e;
        } catch (RuntimeException e) {
            log.trace(e, "Failed to define class %s in %s", name, module);
            throw e;
        }
        final AssertionSetting setting = classSpec.getAssertionSetting();
        if (setting != AssertionSetting.INHERIT) {
            setClassAssertionStatus(name, setting == AssertionSetting.ENABLED);
        }
        return newClass;
    }

    /**
     * Define a package from a package spec.
     *
     * @param name the package name
     * @param spec the package specification
     * @return the new package
     */
    private Package definePackage(final String name, final PackageSpec spec) {
        final Module module = this.module;
        final ModuleLogger log = Module.log;
        log.trace("Attempting to define package %s in %s", name, module);

        final Package pkg;
        if (spec == null) {
            pkg = definePackage(name, null, null, null, null, null, null, null);
        } else {
            pkg = definePackage(name, spec.getSpecTitle(), spec.getSpecVersion(), spec.getSpecVendor(), spec.getImplTitle(), spec.getImplVersion(), spec.getImplVendor(), spec.getSealBase());
            final AssertionSetting setting = spec.getAssertionSetting();
            if (setting != AssertionSetting.INHERIT) {
                setPackageAssertionStatus(name, setting == AssertionSetting.ENABLED);
            }
        }
        log.trace("Defined package %s in %s", name, module);
        return pkg;
    }

    /**
     * Find a library from one of the resource loaders.
     *
     * @param libname the library name
     * @return the full absolute path to the library
     */
    @Override
    protected String findLibrary(final String libname) {
        final ModuleLogger log = Module.log;
        log.trace("Attempting to load native library %s from %s", libname, module);

        for (ResourceLoader loader : resourceLoaders) {
            final String library = loader.getLibrary(libname);
            if (library != null) {
                return library;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public URL findResource(final String name, final boolean exportsOnly) {
        return module.getResource(name, exportsOnly);
    }

    /** {@inheritDoc} */
    @Override
    public Enumeration<URL> findResources(final String name, final boolean exportsOnly) throws IOException {
        return module.getResources(name, exportsOnly);
    }

    /** {@inheritDoc} */
    @Override
    public InputStream findResourceAsStream(final String name, boolean exportsOnly) {
        try {
            final URL resource = findResource(name, exportsOnly);
            return resource == null ? null : resource.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Get the module for this class loader.
     *
     * @return the module
     */
    public Module getModule() {
        return module;
    }

    /**
     * Get a string representation of this class loader.
     *
     * @return the string
     */
    public String toString() {
        return "ClassLoader for " + module;
    }

    /**
     * Get the class loader for a given module.
     *
     * @param identifier the module identifier
     * @return the class loader
     * @throws ModuleLoadException if the module could not be loaded
     */
    public static ModuleClassLoader forModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        return Module.getModule(identifier).getClassLoader();
    }

    /**
     * Get the class loader for a given module.
     *
     * @param identifier the module identifier
     * @return the class loader
     * @throws ModuleLoadException if the module could not be loaded
     * @throws IllegalArgumentException if the given identifier is invalid
     */
    public static ModuleClassLoader forModuleName(final String identifier) throws ModuleLoadException {
        return forModule(ModuleIdentifier.fromString(identifier));
    }

    Set<String> getPaths() {
        return paths.getAllPaths().keySet();
    }
}
