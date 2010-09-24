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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
* A module is a unit of classes and other resources, along with the specification of what is imported and exported
* by this module from and to other modules.  Modules are created by {@link ModuleLoader}s which build modules from
* various configuration information and resource roots.
*
* @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
* @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
* @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
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

    // static properties

    /**
     * The system-wide module logger, which may be changed via {@link #setModuleLogger(ModuleLogger)}.
     */
    static volatile ModuleLogger log = NoopModuleLogger.getInstance();

    /**
     * The selector to choose the module loader to use for locating modules.
     */
    private static volatile ModuleLoaderSelector moduleLoaderSelector = ModuleLoaderSelector.DEFAULT;

    // immutable properties

    /**
     * The identifier of this module.
     */
    private final ModuleIdentifier identifier;
    /**
     * The name of the main class, if any (may be {@code null}).
     */
    private final String mainClassName;
    /**
     * The module class loader for this module.
     */
    private final ModuleClassLoader moduleClassLoader;
    /**
     * The module loader which created this module.
     */
    private final ModuleLoader moduleLoader;
    /**
     * This reference exists solely to prevent the {@code FutureModule} from getting GC'd prematurely.
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private final Object myKey;
    /**
     * The fallback local loader, if any is defined.
     */
    private final LocalLoader fallbackLoader;

    // mutable properties

    private static final Dependency[] NO_DEPENDENCIES = new Dependency[0];

    /**
     * The complete collection of all paths.  Initially, the paths are uninitialized.
     */
    private volatile Paths<LocalLoader, Dependency> paths = Paths.none();

    private static final AtomicReferenceFieldUpdater<Module, Paths<LocalLoader, Dependency>> pathsUpdater
            = unsafeCast(AtomicReferenceFieldUpdater.newUpdater(Module.class, Paths.class, "paths"));

    @SuppressWarnings({ "unchecked" })
    private static <A, B> AtomicReferenceFieldUpdater<A, B> unsafeCast(AtomicReferenceFieldUpdater<?, ?> updater) {
        return (AtomicReferenceFieldUpdater<A, B>) updater;
    }

    /**
     * Construct the system module.
     */
    private Module() {
        moduleLoader = InitialModuleLoader.INSTANCE;
        identifier = ModuleIdentifier.SYSTEM;
        mainClassName = null;
        myKey = null;
        fallbackLoader = null;
        // should be safe, so...
        //noinspection ThisEscapedInObjectConstruction
        moduleClassLoader = new ModuleClassLoader(new ModuleClassLoader.Configuration(this, AssertionSetting.INHERIT, ModuleClassLoader.NO_RESOURCE_LOADERS));
    }

    /**
     * Construct a new instance from a module specification.
     *
     * @param spec the module specification
     * @param moduleLoader the module loader
     * @param myKey the key to keep a strong reference to
     */
    Module(final ModuleSpec spec, final ModuleLoader moduleLoader, final Object myKey) {
        this.moduleLoader = moduleLoader;
        this.myKey = myKey;

        // Initialize state from the spec.
        identifier = spec.getModuleIdentifier();
        mainClassName = spec.getMainClass();
        fallbackLoader = spec.getFallbackLoader();
        //noinspection ThisEscapedInObjectConstruction
        final ModuleClassLoader.Configuration configuration = new ModuleClassLoader.Configuration(this, spec.getAssertionSetting(), spec.getResourceLoaders());
        final ModuleClassLoaderFactory factory = spec.getModuleClassLoaderFactory();
        ModuleClassLoader moduleClassLoader = null;
        if (factory != null) moduleClassLoader = factory.create(configuration);
        if (moduleClassLoader == null) moduleClassLoader = new ModuleClassLoader(configuration);
        this.moduleClassLoader = moduleClassLoader;
    }



    enum LoadState {

        /**
         * This module's content and dependency information have successfully been loaded.
         */
        LOADED,
        /**
         * This module's linkage information is complete, with a populated path->loader mapping.
         */
        RESOLVED,
        /**
         * All of this module's dependencies (and their transitives) are linked and in the READY state.
         */
        LINKED,
    }
    

    private volatile LoadState loadState = LoadState.LOADED;

    enum PathVisitAction {IMPORT_EXPORT, EXPORT_ONLY, SKIP}
    
    private static final AtomicReferenceFieldUpdater<Module, LoadState> loadStateUpdater = AtomicReferenceFieldUpdater.newUpdater(Module.class, LoadState.class, "loadState");

    private void resolveInitial() throws ModuleLoadException {
        if (loadState.compareTo(LoadState.RESOLVED) >= 0) {
            return;
        }
        resolve();
    }

    void resolve() throws ModuleLoadException {
        resolve(new DependencyVisitTracker());
    }

    private void setDependencies(final Dependency[] dependencies) throws ModuleLoadException {
        setDependencies(new DependencyVisitTracker(), paths, dependencies);
    }

    private void resolve(final DependencyVisitTracker dependencyVisitTracker) throws ModuleLoadException {
        final Paths<LocalLoader, Dependency> paths = this.paths;
        setDependencies(dependencyVisitTracker, paths, paths.getSourceList(NO_DEPENDENCIES));
    }

    private void setDependencies(final DependencyVisitTracker visitTracker, final Paths<LocalLoader, Dependency> paths, final Dependency[] dependencies) throws ModuleLoadException {
        final Deque<PathFilter> filterSeries = new ArrayDeque<PathFilter>();
        final Map<String, List<LocalLoader>> allPaths = new HashMap<String, List<LocalLoader>>();
        final Map<String, List<LocalLoader>> exportedPaths = new HashMap<String, List<LocalLoader>>();

        final DependencyVisitor<PathFilter> distantVisitor = new DependencyVisitor<PathFilter>() {

            public void visit(final LocalDependency item, final PathFilter firstExportFilter) throws ModuleLoadException {
                PathVisitAction visitAction = visitTracker.startPathVisit();
                if (visitAction == PathVisitAction.SKIP) {
                    return;
                }
                final Set<String> paths = item.getPaths();
                final PathFilter importFilter = item.getImportFilter();
                final PathFilter exportFilter = item.getExportFilter();
                final LocalLoader loader = item.getLocalLoader();
                OUTER: for (String path : paths) {
                    if (importFilter.accept(path) && exportFilter.accept(path)) {
                        for (PathFilter filter : filterSeries) {
                            if (! filter.accept(path)) {
                                visitTracker.notifyPathNotIncluded();
                                continue OUTER;
                            }
                        }

                        if (visitAction != PathVisitAction.EXPORT_ONLY) {
                            addToMapList(allPaths, path, loader);
                        }
                        if (firstExportFilter.accept(path)) {
                            addToMapList(exportedPaths, path, loader);
                        } else {
                            visitTracker.notifyPathNotExported();
                        }
                    }
                }
                visitTracker.finishPathVisit();
            }

            public void visit(final ModuleDependency item, final PathFilter firstExportFilter) throws ModuleLoadException {
                final Module module = item.getModuleRequired();
                if (module == null) {
                    return;
                }
                final PathFilter exportFilter = item.getExportFilter();
                if (exportFilter == PathFilters.rejectAll()) {
                    return;
                }
                if (!visitTracker.startVisit(item, module)) {
                    return;
                }
                else {
                    filterSeries.addLast(item.getImportFilter());
                    filterSeries.addLast(exportFilter);
                    try {
                        for (Dependency dep : module.getDependencies()) {
                            dep.accept(this, firstExportFilter);
                        }
                    } finally {
                        filterSeries.removeLast();
                        filterSeries.removeLast();
                        visitTracker.finishVisit(module);
                    }
                }
            }
        };
        final DependencyVisitor<Void> nearVisitor = new DependencyVisitor<Void>() {
            public void visit(final LocalDependency item, final Void param) throws ModuleLoadException {
                PathVisitAction visitAction = visitTracker.startPathVisit();
                if (visitAction == PathVisitAction.SKIP) {
                    return;
                }
                final Set<String> paths = item.getPaths();
                final PathFilter importFilter = item.getImportFilter();
                final PathFilter exportFilter = item.getExportFilter();
                final LocalLoader loader = item.getLocalLoader();
                for (String path : paths) {
                    if (importFilter.accept(path)) {
                        addToMapList(allPaths, path, loader);
                        if (exportFilter.accept(path)) {
                            addToMapList(exportedPaths, path, loader);
                        } else {
                            visitTracker.notifyPathNotExported();
                        }
                    } else {
                        visitTracker.notifyPathNotIncluded();
                    }
                }
                visitTracker.finishPathVisit();
            }

            public void visit(final ModuleDependency item, final Void param) throws ModuleLoadException {
                final Module module = item.getModuleRequired();
                if (module == null || !visitTracker.startVisit(item, module)) {
                    return;
                }

                filterSeries.addLast(item.getImportFilter());
                final PathFilter exportFilter = item.getExportFilter();
                try {
                    for (Dependency dep : module.getDependencies()) {
                        dep.accept(distantVisitor, exportFilter);
                    }
                } finally {
                    filterSeries.removeLast();
                    visitTracker.finishVisit(module);
                }
            }
        };

        for (Dependency dependency : dependencies) {
            dependency.accept(nearVisitor, null);
        }

        removeDuplicatesFromLists(allPaths.values());
        removeDuplicatesFromLists(exportedPaths.values());
        pathsUpdater.compareAndSet(this, paths, new Paths<LocalLoader, Dependency>(dependencies, allPaths, exportedPaths));
        loadStateUpdater.compareAndSet(this, LoadState.LOADED, LoadState.RESOLVED);
    }

    Dependency[] getDependencies() {
        return paths.getSourceList(NO_DEPENDENCIES);
    }

    LocalLoader getFallbackLoader() {
        return fallbackLoader;
    }

    void setDependencies(DependencySpec.SpecifiedDependency[] specifiedDependencies, boolean linkChildren) throws ModuleLoadException {
        final List<Dependency> dependencies = new ArrayList<Dependency>();
        for (DependencySpec.SpecifiedDependency specifiedDependency : specifiedDependencies) {
            dependencies.add(specifiedDependency.getDependency(this));
        }

        Dependency[] deps = dependencies.toArray(new Dependency[dependencies.size()]);
        if (linkChildren) {
            linkDependencies(new HashSet<Module>(), deps);
        }
        setDependencies(deps);
    }

    void setInitialDependencies(final DependencySpec.SpecifiedDependency[] specifiedDependencies) {
        final List<Dependency> dependencies = new ArrayList<Dependency>();
        for (DependencySpec.SpecifiedDependency specifiedDependency : specifiedDependencies) {
            dependencies.add(specifiedDependency.getDependency(this));
        }
        paths = new Paths<LocalLoader, Dependency>(dependencies.toArray(new Dependency[dependencies.size()]), Collections.<String, List<LocalLoader>>emptyMap(), Collections.<String, List<LocalLoader>>emptyMap());
    }

    void linkInitial(final HashSet<Module> visited) throws ModuleLoadException {
        if (loadState.compareTo(LoadState.LINKED) >= 0) {
            return;
        }
        link(visited);
    }

    void link(final Set<Module> visited) throws ModuleLoadException {
        if (! visited.add(this)) {
            return;
        }
        resolveInitial();
        final Dependency[] dependencies = getDependencies().clone();
        linkDependencies(visited, dependencies);
    }

    private void linkDependencies(final Set<Module> visited, final Dependency[] dependencies) throws ModuleLoadException {
        Collections.shuffle(Arrays.asList(dependencies));
        for (Dependency dependency : dependencies) {
            dependency.accept(new DependencyVisitor<Void>() {
                public void visit(final LocalDependency item, final Void param) throws ModuleLoadException {
                    // none
                }

                public void visit(final ModuleDependency item, final Void param) throws ModuleLoadException {
                    final Module module = item.getModuleRequired();
                    if (module != null) {
                        module.link(visited);
                    }
                }
            }, null);
        }
        for (Module module : visited) {
            module.loadState = LoadState.LINKED;
        }
    }

    ModuleClassLoader getClassLoaderPrivate() {
        return moduleClassLoader;
    }

    private static <K, V> void addToMapList(Map<K, List<V>> map, K key, V item) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<V>();
            map.put(key, list);
        }
        list.add(item);
    }
    
    private static <E> void removeDuplicatesFromLists(Collection<List<E>> lists) {
        Set<E> set = Collections.newSetFromMap(new IdentityHashMap<E, Boolean>());
        for (List<E> list: lists) {
            if (list.size() <= 1) {
                continue;
            }
            for (Iterator<E> iterator = list.iterator(); iterator.hasNext();) {
                if (!set.add(iterator.next())) {
                    iterator.remove();
                }
            }
            set.clear();
        }
    }

    /**
     * Get the system module.
     *
     * @return the system module
     */
    public static Module getSystemModule() {
        // todo: do we need a perm check here?
        return SystemModuleHolder.SYSTEM;
    }

    /**
     * Get an exported resource from a specific root in this module.
     *
     * @param rootPath the module root to search
     * @param resourcePath the path of the resource
     * @return the resource
     */
    public final Resource getExportedResource(final String rootPath, final String resourcePath) {
        return moduleClassLoader.loadResourceLocal(rootPath, resourcePath, true);
    }

    /**
     * Get a resource from a specific root in this module.
     *
     * @param rootPath the module root to search
     * @param resourcePath the path of the resource
     * @return the resource
     */
    final Resource getResource(final String rootPath, final String resourcePath) {
        return moduleClassLoader.loadResourceLocal(rootPath, resourcePath, false);
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
            final Class<?> mainClass = moduleClassLoader.loadClass(mainClassName);
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

    private static final RuntimePermission GET_CLASS_LOADER = new RuntimePermission("getClassLoader");

    /**
     * Get the class loader for a module.  The class loader can be used to access non-exported classes and
     * resources of the module.
     * <p>
     * If a security manager is present, then this method invokes the security manager's {@code checkPermission} method
     * with a {@code RuntimePermission("getClassLoader")} permission to verify access to the class loader. If
     * access is not granted, a {@code SecurityException} will be thrown.
     *
     * @return the module class loader
     */
    public ModuleClassLoader getClassLoader() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(GET_CLASS_LOADER);
        }
        return moduleClassLoader;
    }

    /**
     * Get all the paths exported by this module.
     *
     * @return the paths that are exported by this module
     */
    public Set<String> getExportedPaths() {
        return Collections.unmodifiableSet(paths.getExportedPaths().keySet());
    }

    /**
     * Get the module for a loaded class, or {@code null} if the class did not come from any module.
     *
     * @param clazz the class
     * @return the module it came from
     */
    public static Module forClass(Class<?> clazz) {
        final ClassLoader cl = clazz.getClassLoader();
        return forClassLoader(cl, false);
    }

    /**
     * Get the module for a class loader, or {@code null} if the class loader is not associated with any module.  If
     * the class loader is unknown, it is possible to check the parent class loader up the chain, and so on until a module is found.
     *
     * @param cl the class loader
     * @param search {@code true} to search up the delegation chain
     * @return the associated module
     */
    public static Module forClassLoader(ClassLoader cl, boolean search) {
        if (cl instanceof ModuleClassLoader) {
            return ((ModuleClassLoader) cl).getModule();
        } else if (cl == null || cl == ClassLoader.getSystemClassLoader()) {
            return getSystemModule();
        } else if (search) {
            return forClassLoader(cl.getParent(), true);
        } else {
            return null;
        }
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
     * Get the current module loader.
     *
     * @return the current module loader
     */
    public static ModuleLoader getCurrentLoader() {
        // todo perm check
        return getCurrentLoaderPrivate();
    }

    static ModuleLoader getCurrentLoaderPrivate() {
        return moduleLoaderSelector.getCurrentLoader();
    }

    /**
     * Load a class from a local loader.
     *
     * @param className the class name
     * @param exportsOnly {@code true} to only load if the class is exported, {@code false} to load any class
     * @param resolve {@code true} to initialize (resolve) the class after definition
     * @return the class
     */
    Class<?> loadModuleClass(final String className, final boolean exportsOnly, final boolean resolve) {
        if (className.startsWith("java.") || className.startsWith("sun.reflect.")) {
            try {
                return moduleClassLoader.loadClass(className, resolve);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        final String path = pathOfClass(className);
        final Map<String, List<LocalLoader>> paths = this.paths.getPaths(exportsOnly);
        final List<LocalLoader> loaders = paths.get(path);
        if (loaders != null) {
            Class<?> clazz = null;
            for (LocalLoader loader : loaders) {
                clazz = loader.loadClassLocal(className, resolve);
                if (clazz != null) {
                    return clazz;
                }
            }
        }
        final LocalLoader fallbackLoader = this.fallbackLoader;
        if (fallbackLoader != null) {
            return fallbackLoader.loadClassLocal(className, resolve);
        }
        return null;
    }

    /**
     * Load a resource from a local loader.
     *
     * @param name the resource name
     * @param exportsOnly {@code true} to only consider exported resources
     * @return the resource URL, or {@code null} if not found
     */
    URL getResource(final String name, final boolean exportsOnly) {
        if (name.startsWith("java/")) {
            return moduleClassLoader.getResource(name);
        }
        log.trace("Attempting to find resource %s in %s", name, this);
        final String path = pathOf(name);
        final Map<String, List<LocalLoader>> paths = this.paths.getPaths(exportsOnly);
        final List<LocalLoader> loaders = paths.get(path);
        if (loaders != null) {
            for (LocalLoader loader : loaders) {
                final List<Resource> resourceList = loader.loadResourceLocal(name);
                for (Resource resource : resourceList) {
                    return resource.getURL();
                }
            }
        }
        final LocalLoader fallbackLoader = this.fallbackLoader;
        if (fallbackLoader != null) {
            final List<Resource> resourceList = fallbackLoader.loadResourceLocal(name);
            for (Resource resource : resourceList) {
                return resource.getURL();
            }
        }
        return null;
    }

    /**
     * Load all resources of a given name from a local loader.
     *
     * @param name the resource name
     * @param exportsOnly {@code true} to only consider exported resources
     * @return the enumeration of all the matching resource URLs (may be empty)
     */
    Enumeration<URL> getResources(final String name, final boolean exportsOnly) {
        if (name.startsWith("java/")) {
            try {
                return moduleClassLoader.getResources(name);
            } catch (IOException e) {
                return ConcurrentClassLoader.EMPTY_ENUMERATION;
            }
        }
        log.trace("Attempting to find all resources %s in %s", name, this);
        final String path = pathOf(name);
        final Map<String, List<LocalLoader>> paths = this.paths.getPaths(exportsOnly);
        final List<LocalLoader> loaders = paths.get(path);

        final List<URL> list = new ArrayList<URL>();
        if (loaders != null) {
            for (LocalLoader loader : loaders) {
                final List<Resource> resourceList = loader.loadResourceLocal(name);
                for (Resource resource : resourceList) {
                    list.add(resource.getURL());
                }
            }
        }
        final LocalLoader fallbackLoader = this.fallbackLoader;
        if (fallbackLoader != null) {
            final List<Resource> resourceList = fallbackLoader.loadResourceLocal(name);
            for (Resource resource : resourceList) {
                list.add(resource.getURL());
            }
        }

        return list.size() == 0 ? ConcurrentClassLoader.EMPTY_ENUMERATION : Collections.enumeration(list);
    }

    /**
     * Get an exported resource URL.
     *
     * @param name the resource name
     * @return the resource, or {@code null} if it was not found
     */
    public final URL getExportedResource(final String name) {
        return getResource(name, true);
    }

    /**
     * Get all exported resource URLs for a resource name.
     *
     * @param name the resource name
     * @return the resource URLs
     */
    public Enumeration<URL> getExportedResources(final String name) {
        return getResources(name, true);
    }

    /**
     * Get the path name of a class.
     *
     * @param className the binary name of the class
     * @return the parent path
     */
    static String pathOfClass(final String className) {
        final String resourceName = className.replace('.', '/');
        final String path;
        final int idx = resourceName.lastIndexOf('/');
        if (idx > -1) {
            path = resourceName.substring(0, idx);
        } else {
            // todo: do we want to disallow the default package?
            path = "";
        }
        return path;
    }

    /**
     * Get the path name of a resource.
     *
     * @param resourceName the resource name
     * @return the parent path
     */
    static String pathOf(final String resourceName) {
        final String path;
        if (resourceName.indexOf('/') == 0) {
            return pathOf(resourceName.substring(1));
        }
        final int idx = resourceName.lastIndexOf('/');
        if (idx > -1) {
            path = resourceName.substring(0, idx);
        } else {
            // todo: do we want to disallow the default package?
            path = "";
        }
        return path;
    }

    /**
     * Get the file name of a class.
     *
     * @param className the class name
     * @return the name of the corresponding class file
     */
    static String fileNameOfClass(final String className) {
        return className.replace('.', '/') + ".class";
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
        logger.greeting();
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

        Module getModule() {
            return module;
        }

        ModuleClassLoader getClassLoader() {
            return module.getClassLoader();
        }

        boolean isExport() {
            return export;
        }
    }

    private static final class SystemModuleHolder {
        private static final Module SYSTEM;

        static {
            final SystemLocalLoader systemLocalLoader = SystemLocalLoader.getInstance();
            final LocalDependency localDependency = new LocalDependency(PathFilters.acceptAll(), PathFilters.acceptAll(), systemLocalLoader, systemLocalLoader.getPathSet());
            final Module system = new Module();
            system.getClassLoaderPrivate().recalculate();
            try {
                system.setDependencies(new DependencySpec.SpecifiedDependency[] { new DependencySpec.ImmediateSpecifiedDependency(localDependency) }, false);
            } catch (ModuleLoadException e) {
                throw new Error("Failed to initialize system module", e);
            }
            SYSTEM = system;
        }

        private SystemModuleHolder() {
        }
    }

    private static final class DependencyVisitTracker {
        private final Set<Dependency> visited;
        private final Map<Module, PathVisitAction> pathActionsPerModule;
        private final Deque<PathVisitAction> pathActionStack;
        private PathVisitAction newPathAction;

        public DependencyVisitTracker()
        {
            visited = new HashSet<Dependency>();
            pathActionsPerModule = new HashMap<Module, PathVisitAction>();
            pathActionStack = new ArrayDeque<PathVisitAction>();
            pathActionStack.addLast(PathVisitAction.IMPORT_EXPORT);
        }

        /**
         * Returns {@code true} if {@code moduleDependency} should be visited.
         */
        public boolean startVisit(ModuleDependency moduleDependency, Module module)
        {
            if (!visited.add(moduleDependency)) {
                return false;
            }
            PathVisitAction currentPathState = pathActionsPerModule.containsKey(module)?
                    pathActionsPerModule.get(module): PathVisitAction.IMPORT_EXPORT;
                    pathActionStack.addLast(currentPathState);
                    return true;
        }

        /**
         * Notifies this tracker that the visit to {@code module} is finished.
         */
        public void finishVisit(Module module) {
            pathActionsPerModule.put(module, pathActionStack.removeLast());
        }

        /**
         * Returns an action indicating how should proceed the path visit.
         */
        public PathVisitAction startPathVisit() {
            if (pathActionStack.getLast() == PathVisitAction.SKIP) {
                return PathVisitAction.SKIP;
            }
            // on a optimistic approach, mark that the current module's path won't require revisiting in the future
            newPathAction = PathVisitAction.SKIP;
            return pathActionStack.removeLast();
        }

        /**
         * Notifies this tracker that the path visit being performed didn't succeed in adding a path to the allPaths
         * collection.
         */
        public void notifyPathNotIncluded() {
            // mark that the current module's paths will require full revisiting in the future
            newPathAction = PathVisitAction.IMPORT_EXPORT;
        }

        /**
         * Notifies this tracker that the path visit being performed didn't succeed in adding a path to the exportPaths
         * collection.
         */
        public void notifyPathNotExported() {
            if (newPathAction == PathVisitAction.SKIP) {
                // mark that the current module's paths will require revisiting only for export in the future
                newPathAction = PathVisitAction.EXPORT_ONLY;
            }
        }

        /**
         * Notifies this tracker that the path visit being performed is finished.
         */
        public void finishPathVisit() {
            pathActionStack.addLast(newPathAction);
        }
    }
}
