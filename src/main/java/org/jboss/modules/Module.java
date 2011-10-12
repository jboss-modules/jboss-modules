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
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.log.ModuleLogger;
import org.jboss.modules.log.NoopModuleLogger;

/**
 * A module is a unit of classes and other resources, along with the specification of what is imported and exported
 * by this module from and to other modules.  Modules are created by {@link ModuleLoader}s which build modules from
 * various configuration information and resource roots.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author Jason T. Greene
 * @author thomas.diesler@jboss.com
 *
 * @apiviz.landmark
*/
public final class Module {

    private static final AtomicReference<ModuleLoader> BOOT_MODULE_LOADER = new AtomicReference<ModuleLoader>();

    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    URL.setURLStreamHandlerFactory(new ModularURLStreamHandlerFactory());
                } catch (Throwable t) {
                    // todo log a warning or something
                }
                try {
                    URLConnection.setContentHandlerFactory(new ModularContentHandlerFactory());
                } catch (Throwable t) {
                    // todo log a warning or something
                }
                return null;
            }
        });

        final String pkgsString = AccessController.doPrivileged(new PropertyReadAction("jboss.modules.system.pkgs"));
        final List<String> list = new ArrayList<String>();
        list.add("java.");
        list.add("sun.reflect.");
        if (pkgsString != null) {
            int i;
            int nc = -1;
            do {
                i = nc + 1;
                nc = pkgsString.indexOf(',', i);
                String part;
                if (nc == -1) {
                    part = pkgsString.substring(i).trim();
                } else {
                    part = pkgsString.substring(i, nc).trim();
                }
                if (part.length() > 0) {
                    list.add((part + ".").intern());
                }
            } while (nc != -1);
        }
        systemPackages = list.toArray(list.toArray(new String[list.size()]));
        final ListIterator<String> iterator = list.listIterator();
        // http://youtrack.jetbrains.net/issue/IDEA-72097
        //noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) {
            iterator.set(iterator.next().replace('.', '/'));
        }
        systemPaths = list.toArray(list.toArray(new String[list.size()]));
    }

    // static properties

    static final String[] systemPackages;
    static final String[] systemPaths;

    /**
     * The system-wide module logger, which may be changed via {@link #setModuleLogger(org.jboss.modules.log.ModuleLogger)}.
     */
    static volatile ModuleLogger log = NoopModuleLogger.getInstance();

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
    @SuppressWarnings({ "unused" })
    private final Object myKey;
    /**
     * The fallback local loader, if any is defined.
     */
    private final LocalLoader fallbackLoader;

    // mutable properties

    /**
     * The linkage state.
     */
    private volatile Linkage linkage = Linkage.NONE;

    // private constants

    private static final RuntimePermission GET_CLASS_LOADER = new RuntimePermission("getClassLoader");
    private static final RuntimePermission GET_BOOT_MODULE_LOADER = new RuntimePermission("getBootModuleLoader");
    private static final RuntimePermission ACCESS_MODULE_LOGGER = new RuntimePermission("accessModuleLogger");
    private static final RuntimePermission ADD_CONTENT_HANDLER_FACTORY = new RuntimePermission("addContentHandlerFactory");
    private static final RuntimePermission ADD_URL_STREAM_HANDLER_FACTORY = new RuntimePermission("addURLStreamHandlerFactory");

    /**
     * Construct a new instance from a module specification.
     *
     * @param spec the module specification
     * @param moduleLoader the module loader
     * @param myKey the key to keep a strong reference to
     */
    Module(final ConcreteModuleSpec spec, final ModuleLoader moduleLoader, final Object myKey) {
        this.moduleLoader = moduleLoader;
        this.myKey = myKey;

        // Initialize state from the spec.
        identifier = spec.getModuleIdentifier();
        mainClassName = spec.getMainClass();
        fallbackLoader = spec.getFallbackLoader();
        //noinspection ThisEscapedInObjectConstruction
        final ModuleClassLoader.Configuration configuration = new ModuleClassLoader.Configuration(this, spec.getAssertionSetting(), spec.getResourceLoaders(), spec.getClassFileTransformer());
        final ModuleClassLoaderFactory factory = spec.getModuleClassLoaderFactory();
        ModuleClassLoader moduleClassLoader = null;
        if (factory != null) moduleClassLoader = factory.create(configuration);
        if (moduleClassLoader == null) moduleClassLoader = new ModuleClassLoader(configuration);
        this.moduleClassLoader = moduleClassLoader;
    }

    LocalLoader getFallbackLoader() {
        return fallbackLoader;
    }

    Dependency[] getDependencies() {
        return linkage.getSourceList();
    }

    ModuleClassLoader getClassLoaderPrivate() {
        return moduleClassLoader;
    }

    /**
     * Get an exported resource from a specific root in this module.
     *
     * @param rootPath the module root to search
     * @param resourcePath the path of the resource
     * @return the resource
     */
    public Resource getExportedResource(final String rootPath, final String resourcePath) {
        return moduleClassLoader.loadResourceLocal(rootPath, resourcePath);
    }

    /**
     * Run a module's main class, if any.
     *
     * @param args the arguments to pass
     * @throws NoSuchMethodException if there is no main method
     * @throws InvocationTargetException if the main method failed
     * @throws ClassNotFoundException if the main class is not found
     */
    public void run(final String[] args) throws NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
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
            final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(moduleClassLoader);
                // ignore the return value
                mainMethod.invoke(null, new Object[] {args});
            } finally {
                Thread.currentThread().setContextClassLoader(previousClassLoader);
            }
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
     * Load a service loader from this module.
     *
     * @param serviceType the service type class
     * @param <S> the service type
     * @return the service loader
     */
    public <S> ServiceLoader<S> loadService(Class<S> serviceType) {
        return ServiceLoader.load(serviceType, moduleClassLoader);
    }

    /**
     * Load a service loader from a module in the caller's module loader. The caller's
     * module loader refers to the loader of the module of the class that calls this method.
     * Note that {@link #loadService(Class)} is more efficient since it does not need to crawl
     * the stack.
     *
     * @param <S> the the service type
     * @param identifier the module identifier containing the service loader
     * @param serviceType the service type class
     * @return the loaded service from the caller's module
     * @throws ModuleLoadException if the named module failed to load
     */
    public static <S> ServiceLoader<S> loadServiceFromCallerModuleLoader(ModuleIdentifier identifier, Class<S> serviceType) throws ModuleLoadException {
        return getCallerModuleLoader().loadModule(identifier).loadService(serviceType);
    }

    /**
     * Get the class loader for a module.  The class loader can be used to access non-exported classes and
     * resources of the module.
     * <p>
     * If a security manager is present, then this method invokes the security manager's {@code checkPermission} method
     * with a <code>RuntimePermission("getClassLoader")</code> permission to verify access to the class loader. If
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
        return Collections.unmodifiableSet(getPathsUnchecked(true).keySet());
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
        } else if (search) {
            return forClassLoader(cl.getParent(), true);
        } else {
            return null;
        }
    }

    /**
     * Gets the boot module loader. The boot module loader is the
     * initial loader that is established by the module framework. It typically
     * is based off of the environmental module path unless it is overridden by
     * specifying a different class name for the {@code boot.module.loader} system
     * property.
     *
     * @return the boot module loader
     */
    public static ModuleLoader getBootModuleLoader() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(GET_BOOT_MODULE_LOADER);
        }
        ModuleLoader loader;
        while ((loader = BOOT_MODULE_LOADER.get()) == null) {
            loader = DefaultBootModuleLoaderHolder.INSTANCE;
            if (BOOT_MODULE_LOADER.compareAndSet(null, loader)) {
                break;
            }
            // get it again
        }
        return loader;
    }

    static void initBootModuleLoader(ModuleLoader loader) {
        BOOT_MODULE_LOADER.set(loader);
    }

    /**
     * Gets the current module loader. The current module loader is the
     * loader of the module from the calling class. Note that this method
     * must crawl the stack to determine this, so other mechanisms are more
     * efficient.
     *
     * @return the current module loader, or {@code null} if this method is called outside of a module
     */
    public static ModuleLoader getCallerModuleLoader() {
        Module callerModule = getCallerModule();
        return callerModule == null ? null : callerModule.getModuleLoader();
    }

    /**
     * Get the current thread's context module loader.  This loader is the one which defined the module
     * whose class loader is, or is a parent of, the thread's current context class loader.  If there is none,
     * then {@code null} is returned.
     *
     * @return the module loader, or {@code null} if none is set
     */
    public static ModuleLoader getContextModuleLoader() {
        return Module.forClassLoader(Thread.currentThread().getContextClassLoader(), true).getModuleLoader();
    }

    /**
     * Get a module from the current module loader. Note that this must crawl the
     * stack to determine this, so other mechanisms are more efficient.
     * @see #getCallerModuleLoader()
     *
     * @param identifier the module identifier
     * @return the module
     * @throws ModuleLoadException if the module could not be loaded
     */
    public static Module getModuleFromCallerModuleLoader(final ModuleIdentifier identifier) throws ModuleLoadException {
        return getCallerModuleLoader().loadModule(identifier);
    }

    /**
     * Get the caller's module. The caller's module is the module containing the method that calls this
     * method. Note that this method crawls the stack so other ways of obtaining the
     * module are more efficient.
     *
     * @return the current module
     */
    public static Module getCallerModule() {
        return forClass(CallerContext.getCallingClass());
    }

    /**
     * Get the module with the given identifier from the module loader used by this module.
     *
     * @param identifier the module identifier
     * @return the module
     * @throws ModuleLoadException if an error occurs
     */
    public Module getModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        return moduleLoader.loadModule(identifier);
    }

    /**
     * Load a class from a module in the system module loader.
     *
     * @see #getBootModuleLoader()
     *
     * @param moduleIdentifier the identifier of the module from which the class
     *        should be loaded
     * @param className the class name to load
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClassFromBootModuleLoader(final ModuleIdentifier moduleIdentifier, final String className)
            throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, true, getBootModuleLoader().loadModule(moduleIdentifier).getClassLoader());
    }

    /**
     * Load a class from a module in the caller's module loader.
     *
     * @see #getCallerModuleLoader()
     *
     * @param moduleIdentifier the identifier of the module from which the class
     *        should be loaded
     * @param className the class name to load
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClassFromCallerModuleLoader(final ModuleIdentifier moduleIdentifier, final String className)
            throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, true, getModuleFromCallerModuleLoader(moduleIdentifier).getClassLoader());
    }

    /**
     * Load a class from a local loader.
     *
     * @param className the class name
     * @param exportsOnly {@code true} to only load if the class is exported, {@code false} to load any class
     * @param resolve {@code true} to resolve the class after definition
     * @return the class
     */
    Class<?> loadModuleClass(final String className, final boolean exportsOnly, final boolean resolve) {
        for (String s : systemPackages) {
            if (className.startsWith(s)) {
                try {
                    return moduleClassLoader.loadClass(className, resolve);
                } catch (ClassNotFoundException e) {
                    return null;
                }
            }
        }
        final String path = pathOfClass(className);
        final Map<String, List<LocalLoader>> paths = getPathsUnchecked(exportsOnly);
        final List<LocalLoader> loaders = paths.get(path);
        if (loaders != null) {
            Class<?> clazz;
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
        for (String s : Module.systemPaths) {
            if (name.startsWith(s)) {
                return moduleClassLoader.getResource(name);
            }
        }
        log.trace("Attempting to find resource %s in %s", name, this);
        final String path = pathOf(name);
        final Map<String, List<LocalLoader>> paths = getPathsUnchecked(exportsOnly);
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
        for (String s : Module.systemPaths) {
            if (name.startsWith(s)) {
                try {
                    return moduleClassLoader.getResources(name);
                } catch (IOException e) {
                    return ConcurrentClassLoader.EMPTY_ENUMERATION;
                }
            }
        }
        log.trace("Attempting to find all resources %s in %s", name, this);
        final String path = pathOf(name);
        final Map<String, List<LocalLoader>> paths = getPathsUnchecked(exportsOnly);
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
    public URL getExportedResource(final String name) {
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
    @Override
    public String toString() {
        return "Module \"" + identifier + "\"" + " from " + moduleLoader.toString();
    }

    /**
     * Get the logger used by the module system.
     * <p>
     * If a security manager is present, then this method invokes the security manager's {@code checkPermission} method
     * with a <code>RuntimePermission("accessModuleLogger")</code> permission to verify access to the module logger. If
     * access is not granted, a {@code SecurityException} will be thrown.
     *
     * @return the module logger
     */
    public static ModuleLogger getModuleLogger() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ACCESS_MODULE_LOGGER);
        }
        return log;
    }

    /**
     * Change the logger used by the module system.
     * <p>
     * If a security manager is present, then this method invokes the security manager's {@code checkPermission} method
     * with a <code>RuntimePermission("accessModuleLogger")</code> permission to verify access to the module logger. If
     * access is not granted, a {@code SecurityException} will be thrown.
     *
     * @param logger the new logger, must not be {@code null}
     */
    public static void setModuleLogger(final ModuleLogger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("logger is null");
        }
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ACCESS_MODULE_LOGGER);
        }
        logger.greeting();
        log = logger;
    }

    /**
     * Return the start time in millis when Module.class was loaded.
     *
     * @return start time of Module.class load
     */
    public static long getStartTime() {
        return StartTimeHolder.START_TIME;
    }

    /**
     * Register an additional module which contains content handlers.
     * <p>
     * If a security manager is present, then this method invokes the security manager's {@code checkPermission} method
     * with a <code>RuntimePermission("addContentHandlerFactory")</code> permission to verify access. If
     * access is not granted, a {@code SecurityException} will be thrown.
     *
     * @param module the module to add
     */
    public static void registerContentHandlerFactoryModule(Module module) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ADD_CONTENT_HANDLER_FACTORY);
        }
        ModularContentHandlerFactory.addHandlerModule(module);
    }

    /**
     * Register an additional module which contains URL handlers.
     * <p>
     * If a security manager is present, then this method invokes the security manager's {@code checkPermission} method
     * with a <code>RuntimePermission("addURLStreamHandlerFactory")</code> permission to verify access. If
     * access is not granted, a {@code SecurityException} will be thrown.
     *
     * @param module the module to add
     */
    public static void registerURLStreamHandlerFactoryModule(Module module) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ADD_URL_STREAM_HANDLER_FACTORY);
        }
        ModularURLStreamHandlerFactory.addHandlerModule(module);
    }

    // Linking and resolution

    static final class Visited {
        private final Module module;
        private final FastCopyHashSet<PathFilter> filters;
        private final int hashCode;

        Visited(final Module module, final FastCopyHashSet<PathFilter> filters) {
            this.module = module;
            this.filters = filters;
            hashCode = filters.hashCode() * 13 + module.hashCode();
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object other) {
            return other instanceof Visited && equals((Visited)other);
        }

        public boolean equals(Visited other) {
            return this == other || other != null && module == other.module && filters.equals(other.filters);
        }
    }

    void addPaths(Dependency[] dependencies, Map<String, List<LocalLoader>> map) throws ModuleLoadException {
        final Set<Visited> visited = new FastCopyHashSet<Visited>();
        for (Dependency dependency : dependencies) {
            if (dependency instanceof ModuleDependency) {
                final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                final ModuleLoader moduleLoader = moduleDependency.getModuleLoader();
                final ModuleIdentifier id = moduleDependency.getIdentifier();
                final Module module;

                try {
                    module = moduleLoader.preloadModule(id);
                } catch (ModuleLoadException ex) {
                    if (moduleDependency.isOptional()) {
                        continue;
                    } else {
                        throw ex;
                    }
                }
                if (module == null) {
                    if (!moduleDependency.isOptional()) {
                        throw new ModuleNotFoundException(id.toString());
                    }
                    continue;
                }

                final PathFilter importFilter = dependency.getImportFilter();
                final FastCopyHashSet<PathFilter> filterStack = new FastCopyHashSet<PathFilter>();
                filterStack.add(importFilter);
                module.addExportedPaths(module.getDependencies(), map, filterStack, visited);
            } else if (dependency instanceof ModuleClassLoaderDependency) {
                final ModuleClassLoaderDependency classLoaderDependency = (ModuleClassLoaderDependency) dependency;
                final LocalLoader localLoader = classLoaderDependency.getLocalLoader();
                final PathFilter importFilter = classLoaderDependency.getImportFilter();
                final Set<String> paths = classLoaderDependency.getPaths();
                for (String path : paths) {
                    if (importFilter.accept(path)) {
                        List<LocalLoader> list = map.get(path);
                        if (list == null) {
                            map.put(path, list = new ArrayList<LocalLoader>());
                            list.add(localLoader);
                        } else if (! list.contains(localLoader)) {
                            list.add(localLoader);
                        }
                    }
                }
            } else if (dependency instanceof LocalDependency) {
                final LocalDependency localDependency = (LocalDependency) dependency;
                final LocalLoader localLoader = localDependency.getLocalLoader();
                final PathFilter importFilter = localDependency.getImportFilter();
                final Set<String> paths = localDependency.getPaths();
                for (String path : paths) {
                    if (importFilter.accept(path)) {
                        List<LocalLoader> list = map.get(path);
                        if (list == null) {
                            map.put(path, list = new ArrayList<LocalLoader>());
                            list.add(localLoader);
                        } else if (! list.contains(localLoader)) {
                            list.add(localLoader);
                        }
                    }
                }
            }
            // else unknown dep type so just skip
        }
    }

    void addExportedPaths(Dependency[] dependencies, Map<String, List<LocalLoader>> map) throws ModuleLoadException {
        addExportedPaths(dependencies, map, new FastCopyHashSet<PathFilter>(), new FastCopyHashSet<Visited>());
    }

    void addExportedPaths(Dependency[] dependencies, Map<String, List<LocalLoader>> map, FastCopyHashSet<PathFilter> filterStack, Set<Visited> visited) throws ModuleLoadException {
        if (!visited.add(new Visited(this, filterStack))) {
            return;
        }
        for (Dependency dependency : dependencies) {
            if (dependency instanceof ModuleDependency) {
                final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                final ModuleLoader moduleLoader = moduleDependency.getModuleLoader();
                final ModuleIdentifier id = moduleDependency.getIdentifier();
                final Module module;

                try {
                    module = moduleLoader.preloadModule(id);
                } catch (ModuleLoadException ex) {
                    if (moduleDependency.isOptional()) {
                        continue;
                    } else {
                        throw ex;
                    }
                }
                if (module == null) {
                    if (!moduleDependency.isOptional()) {
                        throw new ModuleNotFoundException(id.toString());
                    }
                    continue;
                }

                final PathFilter exportFilter = dependency.getExportFilter();
                if (exportFilter != PathFilters.rejectAll()) {
                    final PathFilter importFilter = dependency.getImportFilter();
                    if (filterStack.contains(importFilter) && filterStack.contains(exportFilter)) {
                        module.addExportedPaths(module.getDependencies(), map, filterStack, visited);
                    } else {
                        final FastCopyHashSet<PathFilter> clone = filterStack.clone();
                        clone.add(importFilter);
                        clone.add(exportFilter);
                        module.addExportedPaths(module.getDependencies(), map, clone, visited);
                    }
                }
            } else if (dependency instanceof ModuleClassLoaderDependency) {
                final ModuleClassLoaderDependency classLoaderDependency = (ModuleClassLoaderDependency) dependency;
                final LocalLoader localLoader = classLoaderDependency.getLocalLoader();
                final Set<String> paths = classLoaderDependency.getPaths();
                for (String path : paths) {
                    boolean accept = true;
                    for (Object filter : filterStack.getRawArray()) {
                        if (filter != null && ! ((PathFilter)filter).accept(path)) {
                            accept = false; break;
                        }
                    }
                    if (accept && classLoaderDependency.getImportFilter().accept(path) && classLoaderDependency.getExportFilter().accept(path)) {
                        List<LocalLoader> list = map.get(path);
                        if (list == null) {
                            map.put(path, list = new ArrayList<LocalLoader>(1));
                            list.add(localLoader);
                        } else if (! list.contains(localLoader)) {
                            list.add(localLoader);
                        }
                    }
                }
            } else if (dependency instanceof LocalDependency) {
                final LocalDependency localDependency = (LocalDependency) dependency;
                final LocalLoader localLoader = localDependency.getLocalLoader();
                final Set<String> paths = localDependency.getPaths();
                for (String path : paths) {
                    boolean accept = true;
                    for (Object filter : filterStack.getRawArray()) {
                        if (filter != null && ! ((PathFilter)filter).accept(path)) {
                            accept = false; break;
                        }
                    }
                    if (accept && localDependency.getImportFilter().accept(path) && localDependency.getExportFilter().accept(path)) {
                        List<LocalLoader> list = map.get(path);
                        if (list == null) {
                            map.put(path, list = new ArrayList<LocalLoader>(1));
                            list.add(localLoader);
                        } else if (! list.contains(localLoader)) {
                            list.add(localLoader);
                        }
                    }
                }
            }
            // else unknown dep type so just skip
        }
    }

    Map<String, List<LocalLoader>> getPaths(final boolean exports) throws ModuleLoadException {
        Linkage linkage = this.linkage;
        Linkage.State state = linkage.getState();
        if (state == Linkage.State.LINKED) {
            return linkage.getPaths(exports);
        }
        // slow path loop
        boolean intr = false;
        try {
            for (;;) {
                synchronized (this) {
                    linkage = this.linkage;
                    state = linkage.getState();
                    while (state == Linkage.State.LINKING || state == Linkage.State.NEW) try {
                        wait();
                        linkage = this.linkage;
                        state = linkage.getState();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                    if (state == Linkage.State.LINKED) {
                        return linkage.getPaths(exports);
                    }
                    this.linkage = linkage = new Linkage(linkage.getSourceList(), Linkage.State.LINKING);
                    // fall out and link
                }
                link(linkage);
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    Map<String, List<LocalLoader>> getPathsUnchecked(final boolean export) {
        try {
            return getPaths(export);
        } catch (ModuleLoadException e) {
            throw e.toError();
        }
    }

    void link(final Linkage linkage) throws ModuleLoadException {
        final HashMap<String, List<LocalLoader>> importsMap = new HashMap<String, List<LocalLoader>>();
        final HashMap<String, List<LocalLoader>> exportsMap = new HashMap<String, List<LocalLoader>>();
        final Dependency[] dependencies = linkage.getSourceList();
        addPaths(dependencies, importsMap);
        addExportedPaths(dependencies, exportsMap);
        synchronized (this) {
            if (this.linkage == linkage) {
                this.linkage = new Linkage(this.linkage.getSourceList(), Linkage.State.LINKED, importsMap, exportsMap);
                notifyAll();
            }
            // else all our efforts were just wasted since someone changed the deps in the meantime
        }
    }

    void relinkIfNecessary() throws ModuleLoadException {
        Linkage linkage = this.linkage;
        if (linkage.getState() != Linkage.State.UNLINKED) {
            return;
        }
        synchronized (this) {
            linkage = this.linkage;
            if (linkage.getState() != Linkage.State.UNLINKED) {
                return;
            }
            this.linkage = linkage = new Linkage(linkage.getSourceList(), Linkage.State.LINKING);
        }
        link(linkage);
    }

    void relink() throws ModuleLoadException {
        link(linkage);
    }

    void setDependencies(final List<DependencySpec> dependencySpecs) throws ModuleLoadException {
        synchronized (this) {
            linkage = new Linkage(calculateDependencies(dependencySpecs), Linkage.State.UNLINKED, null, null);
            notifyAll();
        }
    }

    private Dependency[] calculateDependencies(final List<DependencySpec> dependencySpecs) {
        final Dependency[] dependencies = new Dependency[dependencySpecs.size()];
        int i = 0;
        for (DependencySpec spec : dependencySpecs) {
            final Dependency dependency = spec.getDependency(this);
            dependencies[i++] = dependency;
        }
        return dependencies;
    }

    String getMainClass() {
        return mainClassName;
    }

    Package getPackage(final String name) {
        List<LocalLoader> loaders = getPathsUnchecked(false).get(name.replace('.', '/'));
        if (loaders != null) for (LocalLoader localLoader : loaders) {
            Package pkg = localLoader.loadPackageLocal(name);
            if (pkg != null) return pkg;
        }
        return null;
    }

    Package[] getPackages() {
        final ArrayList<Package> packages = new ArrayList<Package>();
        final Map<String, List<LocalLoader>> allPaths = getPathsUnchecked(false);
        next: for (String path : allPaths.keySet()) {
            String packageName = path.replace('/', '.');
            for (LocalLoader loader : allPaths.get(path)) {
                Package pkg = loader.loadPackageLocal(packageName);
                if (pkg != null) {
                    packages.add(pkg);
                }
                continue next;
            }
        }
        return packages.toArray(new Package[packages.size()]);
    }
}
