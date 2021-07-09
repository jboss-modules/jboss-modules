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

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.log.ModuleLogger;
import org.jboss.modules.security.ModularProtectionDomain;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A module classloader.  Instances of this class implement the complete view of classes and resources available in a
 * module.  Contrast with {@link Module}, which has API methods to access the exported view of classes and resources.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author thomas.diesler@jboss.com
 *
 * @apiviz.landmark
 */
public class ModuleClassLoader extends ConcurrentClassLoader {

    static {
        boolean parallelOk = true;
        try {
            parallelOk = ClassLoader.registerAsParallelCapable();
        } catch (Throwable ignored) {
        }
        if (! parallelOk) {
            throw new Error("Failed to register " + ModuleClassLoader.class.getName() + " as parallel-capable");
        }
    }

    private final Module module;
    private final ClassTransformer transformer;

    private final AtomicReference<Paths<ResourceLoader, ResourceLoaderSpec>> paths = new AtomicReference<>(Paths.<ResourceLoader, ResourceLoaderSpec>none());

    private final LocalLoader localLoader = new IterableLocalLoader() {
        public Class<?> loadClassLocal(final String name, final boolean resolve) {
            try {
                return ModuleClassLoader.this.loadClassLocal(name, resolve);
            } catch (ClassNotFoundException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof Error) {
                    throw (Error) cause;
                } else if (cause instanceof RuntimeException) {
                    //unlikely
                    throw (RuntimeException) cause;
                }
                return null;
            }
        }

        public Package loadPackageLocal(final String name) {
            return findLoadedPackage(name);
        }

        public List<Resource> loadResourceLocal(final String name) {
            return ModuleClassLoader.this.loadResourceLocal(name);
        }

        public Iterator<Resource> iterateResources(final String startPath, final boolean recursive) {
            return ModuleClassLoader.this.iterateResources(startPath, recursive);
        }

        public String toString() {
            return "local loader for " + ModuleClassLoader.this.toString();
        }
    };

    /**
     * Construct a new instance.
     *
     * @param configuration the module class loader configuration to use
     */
    protected ModuleClassLoader(final Configuration configuration) {
        super(configuration.getModule().getModuleLoader().getModuleDescription(configuration.getModule()));
        module = configuration.getModule();
        paths.lazySet(new Paths<>(configuration.getResourceLoaders(), Collections.<String, List<ResourceLoader>>emptyMap()));
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
        final Paths<ResourceLoader, ResourceLoaderSpec> paths = this.paths.get();
        return setResourceLoaders(paths, paths.getSourceList(ResourceLoaderSpec.NO_RESOURCE_LOADERS));
    }

    /**
     * Change the set of resource loaders for this module class loader, and recalculate the path maps.
     *
     * @param resourceLoaders the new resource loaders
     * @return {@code true} if the paths were recalculated, or {@code false} if another thread finished recalculating
     *  before the calling thread
     */
    boolean setResourceLoaders(final ResourceLoaderSpec[] resourceLoaders) {
        return setResourceLoaders(paths.get(), resourceLoaders);
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
        return this.paths.compareAndSet(paths, new Paths<>(resourceLoaders, allPaths));
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

        final Class<?> clazz = module.loadModuleClass(className, resolve);

        if (clazz != null) {
            return clazz;
        }

        log.trace("Class %s not found from %s", className, module);

        throw new ClassNotFoundException(getClassNotFoundExceptionMessage(className, module));
    }

    /**
     * Returns an exception message used when producing instances of ClassNotFoundException. This can be overridden
     * by subclasses to customise the error message.
     *
     * @param className the name of the class which is missing
     * @param fromModule the module from which the class could not be found
     * @return an exception message used when producing instances of ClassNotFoundException
     */
    protected String getClassNotFoundExceptionMessage(String className, Module fromModule){
        return className + " from [" + fromModule + "]";
    }

    /**
     * Load a class from this class loader.
     *
     * @param className the class name to load
     * @return the loaded class or {@code null} if it was not found
     * @throws ClassNotFoundException if an exception occurs while loading the class or its dependencies
     */
    public Class<?> loadClassLocal(String className) throws ClassNotFoundException {
        return loadClassLocal(className, false);
    }

    /**
     * Load a local class from this class loader.
     *
     * @param className the class name
     * @param resolve {@code true} to resolve the loaded class
     * @return the loaded class or {@code null} if it was not found
     * @throws ClassNotFoundException if an error occurs while loading the class
     */
    public Class<?> loadClassLocal(final String className, final boolean resolve) throws ClassNotFoundException {
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

        final Map<String, List<ResourceLoader>> paths = this.paths.get().getAllPaths();

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
            throw e;
        }
        log.trace("No local specification found for class %s in %s", className, module);
        return null;
    }

    /**
     * Load a local exported resource from a specific root from this module class loader.
     *
     * @param root the root name
     * @param name the resource name
     * @return the resource, or {@code null} if it was not found
     */
    @Deprecated
    Resource loadResourceLocal(final String root, final String name) {
        final Map<String, List<ResourceLoader>> paths = this.paths.get().getAllPaths();
        final String path = Module.pathOf(name);
        final List<ResourceLoader> loaders = paths.get(path);
        if (loaders != null) {
            for (ResourceLoader loader : loaders) {
                if (root.equals(loader.getRootName())) {
                    return loader.getResource(name);
                }
            }
        }
        return null;
    }

    /**
     * Load a local exported resource from this class loader.
     *
     * @param name the resource name
     * @return the list of resources
     */
    public List<Resource> loadResourceLocal(final String name) {
        final Map<String, List<ResourceLoader>> paths = this.paths.get().getAllPaths();
        final String path = Module.pathOf(name);
        final List<ResourceLoader> loaders = paths.get(path);
        final List<Resource> list = new ArrayList<Resource>(loaders == null ? 1 : loaders.size());
        if (loaders != null) {
            for (ResourceLoader loader : loaders) {
                final Resource resource = loader.getResource(name);
                if (resource != null) {
                    list.add(resource);
                }
            }
        }
        return list.isEmpty() ? Collections.emptyList() : list;
    }

    private Class<?> doDefineOrLoadClass(final String className, final byte[] bytes, final ByteBuffer byteBuffer, ProtectionDomain protectionDomain) {
        try {
            final Class<?> definedClass = bytes != null ?
                defineClass(className, bytes, 0, bytes.length, protectionDomain) :
                defineClass(className, byteBuffer, protectionDomain);
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

    private final IdentityHashMap<CodeSource, ProtectionDomain> protectionDomains = new IdentityHashMap<CodeSource, ProtectionDomain>();

    private ProtectionDomain getProtectionDomain(CodeSource codeSource) {
        final IdentityHashMap<CodeSource, ProtectionDomain> map = protectionDomains;
        synchronized (map) {
            ProtectionDomain protectionDomain = map.get(codeSource);
            if (protectionDomain == null) {
                final PermissionCollection permissions = module.getPermissionCollection();
                protectionDomain = new ModularProtectionDomain(codeSource, permissions, this);
                map.put(codeSource, protectionDomain);
            }
            return protectionDomain;
        }
    }

    Class<?> defineClassInternal(final String className, ByteBuffer byteBuffer, final ProtectionDomain protectionDomain) {
        if (transformer != null) {
            int pos = byteBuffer.position();
            int lim = byteBuffer.limit();
            ByteBuffer transformed;
            try {
                transformed = transformer.transform(this, className.replace('.', '/'), protectionDomain, byteBuffer);
            } catch (Exception e) {
                ClassFormatError error = new ClassFormatError(e.getMessage());
                error.initCause(e);
                throw error;
            }
            if (transformed != null) {
                byteBuffer = transformed;
            } else {
                byteBuffer.position(0);
                byteBuffer.limit(lim);
                byteBuffer.position(pos);
            }
        }
        final long start = Metrics.getCurrentCPUTime();
        final Class<?> defined = defineClass(className, byteBuffer, protectionDomain);
        module.getModuleLoader().addClassLoadTime(Metrics.getCurrentCPUTime() - start);
        return defined;
    }

    Class<?> defineClassInternal(final String className, byte[] bytes, int off, int len, final ProtectionDomain protectionDomain) {
        if (transformer != null) {
            return defineClassInternal(className, ByteBuffer.wrap(bytes, off, len), protectionDomain);
        }
        final long start = Metrics.getCurrentCPUTime();
        final Class<?> defined = defineClass(className, bytes, off, len, protectionDomain);
        module.getModuleLoader().addClassLoadTime(Metrics.getCurrentCPUTime() - start);
        return defined;
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
            ByteBuffer byteBuffer = classSpec.getByteBuffer();
            try {
                final ProtectionDomain protectionDomain = getProtectionDomain(classSpec.getCodeSource());
                if (transformer != null) {
                    ByteBuffer buffer = byteBuffer;
                    if (buffer == null) {
                        buffer = ByteBuffer.wrap(bytes);
                    }
                    int pos = buffer.position();
                    int lim = buffer.limit();
                    ByteBuffer transformed;
                    try {
                        transformed = transformer.transform(this, name.replace('.', '/'), protectionDomain, buffer);
                    } catch (Exception e) {
                        ClassFormatError error = new ClassFormatError(e.getMessage());
                        error.initCause(e);
                        throw error;
                    }
                    if (transformed != null) {
                        byteBuffer = transformed;
                        bytes = null;
                    } else if (byteBuffer != null) {
                        byteBuffer.position(0);
                        byteBuffer.limit(lim);
                        byteBuffer.position(pos);
                    }
                }
                final long start = Metrics.getCurrentCPUTime();
                newClass = doDefineOrLoadClass(name, bytes, byteBuffer, protectionDomain);
                module.getModuleLoader().addClassLoadTime(Metrics.getCurrentCPUTime() - start);
                log.classDefined(name, module);
            } catch (LinkageError e) {
                // Prepend the current class name, so that transitive class definition issues are clearly expressed
                Error ne;
                try {
                    final String oldMsg = e.getMessage();
                    final String newMsg = "Failed to link " + name.replace('.', '/') + " (" + module + ")";
                    ne = e.getClass().getConstructor(String.class).newInstance(oldMsg == null || oldMsg.isEmpty() ? newMsg : newMsg + ": " + oldMsg);
                    ne.setStackTrace(e.getStackTrace());
                } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
                    // just throw the original
                    throw e;
                }
                throw ne;
            }
        } catch (Error | RuntimeException e) {
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

        for (ResourceLoaderSpec loader : paths.get().getSourceList(ResourceLoaderSpec.NO_RESOURCE_LOADERS)) {
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
        return module.getResource(name);
    }

    /** {@inheritDoc} */
    @Override
    public final Enumeration<URL> findResources(final String name, final boolean exportsOnly) {
        return module.getResources(name);
    }

    /** {@inheritDoc} */
    @Override
    public final InputStream findResourceAsStream(final String name, boolean exportsOnly) {
        try {
            return module.getResourceAsStream(name);
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
     * Get the name of this module.  This method is used by Java 9 in debug output.
     *
     * @return the name of this module
     */
    public final String getName() {
        return super.getName();
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
        return paths.get().getAllPaths().keySet();
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
        final ResourceLoaderSpec[] specs = paths.get().getSourceList(ResourceLoaderSpec.NO_RESOURCE_LOADERS);
        final int length = specs.length;
        final ResourceLoader[] loaders = new ResourceLoader[length];
        for (int i = 0; i < length; i++) {
            loaders[i] = specs[i].getResourceLoader();
        }
        return loaders;
    }

    /**
     * Iterate the resources within this module class loader.  Only resource roots which are inherently iterable will
     * be checked, thus the result of this method may only be a subset of the actual loadable resources.  The returned
     * resources are not sorted or grouped in any particular way.
     *
     * @param startName the directory name to search
     * @param recurse {@code true} to recurse into subdirectories, {@code false} otherwise
     * @return the resource iterator
     */
    public final Iterator<Resource> iterateResources(final String startName, final boolean recurse) {
        final String realStartName = PathUtils.canonicalize(PathUtils.relativize(startName));
        final PathFilter filter;
        if (recurse) {
            if (realStartName.isEmpty()) {
                filter = PathFilters.acceptAll();
            } else {
                filter = PathFilters.any(PathFilters.is(realStartName), PathFilters.isChildOf(realStartName));
            }
        } else {
            filter = PathFilters.is(realStartName);
        }
        final Map<String, List<ResourceLoader>> paths = this.paths.get().getAllPaths();
        final Iterator<Map.Entry<String, List<ResourceLoader>>> iterator = paths.entrySet().iterator();
        return new Iterator<Resource>() {

            private String path;
            private Iterator<Resource> resourceIterator;
            private Iterator<ResourceLoader> loaderIterator;
            private Resource next;

            public boolean hasNext() {
                while (next == null) {
                    if (resourceIterator != null) {
                        assert path != null;
                        if (resourceIterator.hasNext()) {
                            next = resourceIterator.next();
                            return true;
                        }
                        resourceIterator = null;
                    }
                    if (loaderIterator != null) {
                        assert path != null;
                        if (loaderIterator.hasNext()) {
                            final ResourceLoader loader = loaderIterator.next();
                            if (loader instanceof IterableResourceLoader) {
                                resourceIterator = ((IterableResourceLoader)loader).iterateResources(path, false);
                                continue;
                            }
                        }
                        loaderIterator = null;
                    }
                    if (! iterator.hasNext()) {
                        return false;
                    }
                    final Map.Entry<String, List<ResourceLoader>> entry = iterator.next();
                    path = entry.getKey();
                    if (filter.accept(path)) {
                        loaderIterator = entry.getValue().iterator();
                    }
                }
                return true;
            }

            public Resource next() {
                if (! hasNext()) throw new NoSuchElementException();
                try {
                    return next;
                } finally {
                    next = null;
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Get the (unmodifiable) set of paths which are locally available in this module class loader.  The set will
     * include all paths defined by the module's resource loaders, minus any paths excluded by filters.  The set will
     * generally always contain an empty entry ("").  The set is unordered and unsorted, and is iterable in O(n) time
     * and accessible in O(1) time.
     *
     * @return the set of local paths
     */
    public final Set<String> getLocalPaths() {
        return Collections.unmodifiableSet(paths.get().getAllPaths().keySet());
    }

    /**
     * An opaque configuration used internally to create a module class loader.
     *
     * @apiviz.exclude
     */
    public static final class Configuration {
        private final Module module;
        private final AssertionSetting assertionSetting;
        private final ResourceLoaderSpec[] resourceLoaders;
        private final ClassTransformer transformer;

        Configuration(final Module module, final AssertionSetting assertionSetting, final ResourceLoaderSpec[] resourceLoaders, final ClassTransformer transformer) {
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

        ClassTransformer getTransformer() {
            return transformer;
        }
    }
}
