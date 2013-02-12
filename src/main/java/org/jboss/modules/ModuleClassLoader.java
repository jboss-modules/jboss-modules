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

import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.log.ModuleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A module classloader.  Instances of this class implement the complete view of classes and resources available in a
 * module.  Contrast with {@link Module}, which has API methods to access the exported view of classes and resources.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author thomas.diesler@jboss.com
 *
 * @apiviz.landmark
 */
public class ModuleClassLoader extends ConcurrentClassLoader {

    static {
        try {
            ClassLoader.registerAsParallelCapable();
        } catch (Throwable ignored) {
        }
    }

    static final ResourceLoaderSpec[] NO_RESOURCE_LOADERS = new ResourceLoaderSpec[0];

    private final Module module;
    private final ClassFileTransformer transformer;

    private volatile Paths<ResourceLoader, ResourceLoaderSpec> paths;

    private final LocalLoader localLoader = new LocalLoader() {
        public Class<?> loadClassLocal(final String name, final boolean resolve) {
            try {
                return ModuleClassLoader.this.loadClassLocal(name, resolve);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        public Package loadPackageLocal(final String name) {
            return findLoadedPackage(name);
        }

        public List<Resource> loadResourceLocal(final String name) {
            return ModuleClassLoader.this.loadResourceLocal(name);
        }

        public String toString() {
            return "local loader for " + ModuleClassLoader.this.toString();
        }
    };

    private static final AtomicReferenceFieldUpdater<ModuleClassLoader, Paths<ResourceLoader, ResourceLoaderSpec>> pathsUpdater
            = unsafeCast(AtomicReferenceFieldUpdater.newUpdater(ModuleClassLoader.class, Paths.class, "paths"));

    @SuppressWarnings({ "unchecked" })
    private static <A, B> AtomicReferenceFieldUpdater<A, B> unsafeCast(AtomicReferenceFieldUpdater<?, ?> updater) {
        return (AtomicReferenceFieldUpdater<A, B>) updater;
    }

    /**
     * Construct a new instance.
     *
     * @param configuration the module class loader configuration to use
     */
    protected ModuleClassLoader(final Configuration configuration) {
        module = configuration.getModule();
        paths = new Paths<ResourceLoader, ResourceLoaderSpec>(configuration.getResourceLoaders(), Collections.<String, List<ResourceLoader>>emptyMap(), Collections.<String, List<ResourceLoader>>emptyMap());
        final AssertionSetting setting = configuration.getAssertionSetting();
        if (setting != AssertionSetting.INHERIT) {
            setDefaultAssertionStatus(setting == AssertionSetting.ENABLED);
        }
        transformer = configuration.getTransformer();
    }

    /**
     * Recalculate the path maps for this module class loader.
     *
     * @return {@code true} if the paths were recalculated, or {@code false} if another thread finished recalculating
     *  before the calling thread
     */
    boolean recalculate() {
        final Paths<ResourceLoader, ResourceLoaderSpec> paths = this.paths;
        return setResourceLoaders(paths, paths.getSourceList(NO_RESOURCE_LOADERS));
    }

    /**
     * Change the set of resource loaders for this module class loader, and recalculate the path maps.
     *
     * @param resourceLoaders the new resource loaders
     * @return {@code true} if the paths were recalculated, or {@code false} if another thread finished recalculating
     *  before the calling thread
     */
    boolean setResourceLoaders(final ResourceLoaderSpec[] resourceLoaders) {
        return setResourceLoaders(paths, resourceLoaders);
    }

    private boolean setResourceLoaders(final Paths<ResourceLoader, ResourceLoaderSpec> paths, final ResourceLoaderSpec[] resourceLoaders) {
        final Map<String, List<ResourceLoader>> allPaths = new HashMap<String, List<ResourceLoader>>();
        for (ResourceLoaderSpec loaderSpec : resourceLoaders) {
            final ResourceLoader loader = loaderSpec.getResourceLoader();
            final PathFilter filter = loaderSpec.getPathFilter();
            for (String path : loader.getPaths()) {
                if (filter.accept(path)) {
                    final List<ResourceLoader> allLoaders = allPaths.get(path);
                    if (allLoaders == null) {
                        ArrayList<ResourceLoader> newList = new ArrayList<ResourceLoader>(16);
                        newList.add(loader);
                        allPaths.put(path, newList);
                    } else {
                        allLoaders.add(loader);
                    }
                }
            }
        }
        return pathsUpdater.compareAndSet(this, paths, new Paths<ResourceLoader, ResourceLoaderSpec>(resourceLoaders, allPaths, null));
    }

    /**
     * Get the local loader which refers to this module class loader.
     *
     * @return the local loader
     */
    LocalLoader getLocalLoader() {
        return localLoader;
    }

    /** {@inheritDoc} */
    @Override
    protected final Class<?> findClass(String className, boolean exportsOnly, final boolean resolve) throws ClassNotFoundException {
        // Check if we have already loaded it..
        Class<?> loadedClass = findLoadedClass(className);
        if (loadedClass != null) {
            if (resolve) {
                resolveClass(loadedClass);
            }
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
     * @param resolve {@code true} to resolve the loaded class
     * @return the loaded class or {@code null} if it was not found
     * @throws ClassNotFoundException if an error occurs while loading the class
     */
    Class<?> loadClassLocal(final String className, final boolean resolve) throws ClassNotFoundException {
        final ModuleLogger log = Module.log;
        final Module module = this.module;
        log.trace("Finding local class %s from %s", className, module);

        // Check if we have already loaded it..
        Class<?> loadedClass = findLoadedClass(className);
        if (loadedClass != null) {
            log.trace("Found previously loaded %s from %s", loadedClass, module);
            if (resolve) {
                resolveClass(loadedClass);
            }
            return loadedClass;
        }

        final Map<String, List<ResourceLoader>> paths = this.paths.getAllPaths();

        log.trace("Loading class %s locally from %s", className, module);

        String pathOfClass = Module.pathOfClass(className);
        final List<ResourceLoader> loaders = paths.get(pathOfClass);
        if (loaders == null) {
            // no loaders for this path
            return null;
        }

        // Check to see if we can define it locally it
        ClassSpec classSpec;
        ResourceLoader resourceLoader;
        try {
            if (loaders.size() > 0) {
                String fileName = Module.fileNameOfClass(className);
                for (ResourceLoader loader : loaders) {
                    classSpec = loader.getClassSpec(fileName);
                    if (classSpec != null) {
                        resourceLoader = loader;
                        try {
                            preDefine(classSpec, className);
                        }
                        catch (Throwable th) {
                            throw new ClassNotFoundException("Failed to preDefine class: " + className, th);
                        }
                        final Class<?> clazz = defineClass(className, classSpec, resourceLoader);
                        try {
                            postDefine(classSpec, clazz);
                        }
                        catch (Throwable th) {
                            throw new ClassNotFoundException("Failed to postDefine class: " + className, th);
                        }
                        if (resolve) {
                            resolveClass(clazz);
                        }
                        return clazz;
                    }
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
        log.trace("No local specification found for class %s in %s", className, module);
        return null;
    }

    /**
     * Load a local resource from a specific root from this module class loader.
     *
     * @param root the root name
     * @param name the resource name
     * @return the resource, or {@code null} if it was not found
     */
    Resource loadResourceLocal(final String root, final String name) {

        final Map<String, List<ResourceLoader>> paths = this.paths.getAllPaths();

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
     * @return the list of resources
     */
    List<Resource> loadResourceLocal(final String name) {
        final Map<String, List<ResourceLoader>> paths = this.paths.getAllPaths();

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

    private Class<?> doDefineOrLoadClass(final String className, final byte[] bytes, int off, int len, CodeSource codeSource) {
        try {
            final Class<?> definedClass = defineClass(className, bytes, off, len, codeSource);
            module.getModuleLoader().incClassCount();
            return definedClass;
        } catch (LinkageError e) {
            final Class<?> loadedClass = findLoadedClass(className);
            if (loadedClass != null) {
                module.getModuleLoader().incRaceCount();
                return loadedClass;
            }
            throw e;
        }
    }

    /**
     * Define a class from a class name and class spec.  Also defines any enclosing {@link Package} instances,
     * and performs any sealed-package checks.
     *
     * @param name the class name
     * @param classSpec the class spec
     * @param resourceLoader the resource loader of the class spec
     * @return the new class
     */
    private Class<?> defineClass(final String name, final ClassSpec classSpec, final ResourceLoader resourceLoader) {
        final ModuleLogger log = Module.log;
        final Module module = this.module;
        log.trace("Attempting to define class %s in %s", name, module);

        // Ensure that the package is loaded
        final int lastIdx = name.lastIndexOf('.');
        if (lastIdx != -1) {
            // there's a package name; get the Package for it
            final String packageName = name.substring(0, lastIdx);
            synchronized (this) {
                Package pkg = findLoadedPackage(packageName);
                if (pkg == null) {
                    try {
                        pkg = definePackage(packageName, resourceLoader.getPackageSpec(packageName));
                    } catch (IOException e) {
                        pkg = definePackage(packageName, null);
                    }
                }
                // Check sealing
                if (pkg.isSealed() && ! pkg.isSealed(classSpec.getCodeSource().getLocation())) {
                    log.trace("Detected a sealing violation (attempt to define class %s in sealed package %s in %s)", name, packageName, module);
                    // use the same message as the JDK
                    throw new SecurityException("sealing violation: package " + packageName + " is sealed");
                }
            }
        }
        final Class<?> newClass;
        try {
            byte[] bytes = classSpec.getBytes();
            try {
                if (transformer != null) {
                    try {
                        // todo: support protection domain
                        bytes = transformer.transform(this, name.replace('.', '/'), null, null, bytes);
                    } catch (Exception e) {
                        ClassFormatError error = new ClassFormatError(e.getMessage());
                        error.initCause(e);
                        throw error;
                    }
                }
                final long start = Metrics.getCurrentCPUTime();
                newClass = doDefineOrLoadClass(name, bytes, 0, bytes.length, classSpec.getCodeSource());
                module.getModuleLoader().addClassLoadTime(Metrics.getCurrentCPUTime() - start);
                log.classDefined(name, module);
            } catch (NoClassDefFoundError e) {
                // Prepend the current class name, so that transitive class definition issues are clearly expressed
                final LinkageError ne = new LinkageError("Failed to link " + name.replace('.', '/') + " (" + module + ")");
                ne.initCause(e);
                throw ne;
            }
        } catch (Error e) {
            log.classDefineFailed(e, name, module);
            throw e;
        } catch (RuntimeException e) {
            log.classDefineFailed(e, name, module);
            throw e;
        }
        final AssertionSetting setting = classSpec.getAssertionSetting();
        if (setting != AssertionSetting.INHERIT) {
            setClassAssertionStatus(name, setting == AssertionSetting.ENABLED);
        }
        return newClass;
    }

    /**
     * A hook which is invoked before a class is defined.
     *
     * @param classSpec the class spec of the defined class
     * @param className the class to be defined
     */
    @SuppressWarnings("unused")
    protected void preDefine(ClassSpec classSpec, String className) {
    }

    /**
     * A hook which is invoked after a class is defined.
     *
     * @param classSpec the class spec of the defined class
     * @param definedClass the class that was defined
     */
    @SuppressWarnings("unused")
    protected void postDefine(ClassSpec classSpec, Class<?> definedClass) {
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
    protected final String findLibrary(final String libname) {
        final ModuleLogger log = Module.log;
        log.trace("Attempting to load native library %s from %s", libname, module);

        for (ResourceLoaderSpec loader : paths.getSourceList(NO_RESOURCE_LOADERS)) {
            final String library = loader.getResourceLoader().getLibrary(libname);
            if (library != null) {
                return library;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public final URL findResource(final String name, final boolean exportsOnly) {
        return module.getResource(name, exportsOnly);
    }

    /** {@inheritDoc} */
    @Override
    public final Enumeration<URL> findResources(final String name, final boolean exportsOnly) throws IOException {
        return module.getResources(name, exportsOnly);
    }

    /** {@inheritDoc} */
    @Override
    public final InputStream findResourceAsStream(final String name, boolean exportsOnly) {
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
    public final Module getModule() {
        return module;
    }

    /**
     * Get a string representation of this class loader.
     *
     * @return the string
     */
    @Override
    public final String toString() {
        return getClass().getSimpleName() + " for " + module;
    }

    Set<String> getPaths() {
        return paths.getAllPaths().keySet();
    }

    /** {@inheritDoc} */
    @Override
    protected final PermissionCollection getPermissions(final CodeSource codesource) {
        return super.getPermissions(codesource);
    }

    /** {@inheritDoc} */
    @Override
    protected final Package definePackage(final String name, final String specTitle, final String specVersion, final String specVendor, final String implTitle, final String implVersion, final String implVendor, final URL sealBase) throws IllegalArgumentException {
        return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }

    /** {@inheritDoc} */
    @Override
    protected final Package getPackageByName(final String name) {
        Package loaded = findLoadedPackage(name);
        if (loaded != null) {
            return loaded;
        }
        return module.getPackage(name);
    }

    /** {@inheritDoc} */
    @Override
    protected final Package[] getPackages() {
        return module.getPackages();
    }

    /** {@inheritDoc} */
    @Override
    public final void setDefaultAssertionStatus(final boolean enabled) {
        super.setDefaultAssertionStatus(enabled);
    }

    /** {@inheritDoc} */
    @Override
    public final void setPackageAssertionStatus(final String packageName, final boolean enabled) {
        super.setPackageAssertionStatus(packageName, enabled);
    }

    /** {@inheritDoc} */
    @Override
    public final void setClassAssertionStatus(final String className, final boolean enabled) {
        super.setClassAssertionStatus(className, enabled);
    }

    /** {@inheritDoc} */
    @Override
    public final void clearAssertionStatus() {
        super.clearAssertionStatus();
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object obj) {
        return super.equals(obj);
    }

    /** {@inheritDoc} */
    @Override
    protected final Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /** {@inheritDoc} */
    @Override
    protected final void finalize() throws Throwable {
        super.finalize();
    }

    ResourceLoader[] getResourceLoaders() {
        final ResourceLoaderSpec[] specs = paths.getSourceList(NO_RESOURCE_LOADERS);
        final int length = specs.length;
        final ResourceLoader[] loaders = new ResourceLoader[length];
        for (int i = 0; i < length; i++) {
            loaders[i] = specs[i].getResourceLoader();
        }
        return loaders;
    }

    /**
     * An opaque configuration used internally to create a module class loader.
     *
     * @apiviz.exclude
     */
    protected static final class Configuration {
        private final Module module;
        private final AssertionSetting assertionSetting;
        private final ResourceLoaderSpec[] resourceLoaders;
        private final ClassFileTransformer transformer;

        Configuration(final Module module, final AssertionSetting assertionSetting, final ResourceLoaderSpec[] resourceLoaders, final ClassFileTransformer transformer) {
            this.module = module;
            this.assertionSetting = assertionSetting;
            this.resourceLoaders = resourceLoaders;
            this.transformer = transformer;
        }

        Module getModule() {
            return module;
        }

        AssertionSetting getAssertionSetting() {
            return assertionSetting;
        }

        ResourceLoaderSpec[] getResourceLoaders() {
            return resourceLoaders;
        }

        ClassFileTransformer getTransformer() {
            return transformer;
        }
    }
}
