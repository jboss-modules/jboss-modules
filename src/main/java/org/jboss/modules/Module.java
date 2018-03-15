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

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.modules._private.ModulesPrivateAccess;
import org.jboss.modules.filter.ClassFilter;
import org.jboss.modules.filter.ClassFilters;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.log.ModuleLogger;
import org.jboss.modules.log.NoopModuleLogger;

import __redirected.__JAXPRedirected;
import org.jboss.modules.security.ModularPermissionFactory;

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

    private static final AtomicReference<ModuleLoader> BOOT_MODULE_LOADER;
    private static final MethodType MAIN_METHOD_TYPE = MethodType.methodType(void.class, String[].class);

    static {
        log = NoopModuleLogger.getInstance();
        BOOT_MODULE_LOADER = new AtomicReference<ModuleLoader>();
        EMPTY_CLASS_FILTERS = new FastCopyHashSet<ClassFilter>(0);
        EMPTY_PATH_FILTERS = new FastCopyHashSet<PathFilter>(0);
        GET_DEPENDENCIES = new RuntimePermission("getDependencies");
        GET_CLASS_LOADER = new RuntimePermission("getClassLoader");
        GET_BOOT_MODULE_LOADER = new RuntimePermission("getBootModuleLoader");
        ACCESS_MODULE_LOGGER = new RuntimePermission("accessModuleLogger");
        ADD_CONTENT_HANDLER_FACTORY = new RuntimePermission("addContentHandlerFactory");
        ADD_URL_STREAM_HANDLER_FACTORY = new RuntimePermission("addURLStreamHandlerFactory");

        final String pkgsString = AccessController.doPrivileged(new PropertyReadAction("jboss.modules.system.pkgs"));
        final List<String> list = new ArrayList<String>();
        list.add("java.");
        list.add("sun.reflect.");
        list.add("jdk.internal.reflect.");
        list.add("__redirected.");
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
        final String[] noStrings = new String[0];
        systemPackages = list.toArray(noStrings);
        final ListIterator<String> iterator = list.listIterator();
        // http://youtrack.jetbrains.net/issue/IDEA-72097
        //noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) {
            iterator.set(iterator.next().replace('.', '/'));
        }
        systemPaths = list.toArray(noStrings);

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    URL.setURLStreamHandlerFactory(ModularURLStreamHandlerFactory.INSTANCE);
                } catch (Throwable t) {
                    // todo log a warning or something
                }
                try {
                    URLConnection.setContentHandlerFactory(ModularContentHandlerFactory.INSTANCE);
                } catch (Throwable t) {
                    // todo log a warning or something
                }

                __JAXPRedirected.initAll();

                return null;
            }
        });
    }

    // static properties

    static final String[] systemPackages;
    static final String[] systemPaths;

    static final ModulesPrivateAccess PRIVATE_ACCESS = new ModulesPrivateAccess() {
        public ModuleClassLoader getClassLoaderOf(final Module module) {
            return module.getClassLoaderPrivate();
        }
    };

    /**
     * Private access for module internal code.  Throws {@link SecurityException} for user code.
     *
     * @throws SecurityException always
     */
    public static ModulesPrivateAccess getPrivateAccess() {
        if (JDKSpecific.getCallingClass() == ModularPermissionFactory.class) {
            return PRIVATE_ACCESS;
        }
        throw new SecurityException();
    }

    /**
     * The system-wide module logger, which may be changed via {@link #setModuleLogger(org.jboss.modules.log.ModuleLogger)}.
     */
    static volatile ModuleLogger log;

    private static final FastCopyHashSet<ClassFilter> EMPTY_CLASS_FILTERS;
    private static final FastCopyHashSet<PathFilter> EMPTY_PATH_FILTERS;

    // immutable properties

    /**
     * The name of this module.
     */
    private final String name;
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
     * The fallback local loader, if any is defined.
     */
    private final LocalLoader fallbackLoader;
    /**
     * The properties map specified when this module was defined.
     */
    private final Map<String, String> properties;
    /**
     * The assigned permission collection.
     */
    private final PermissionCollection permissionCollection;
    /**
     * The (optional) module version.
     */
    private final Version version;

    // mutable properties
    /**
     * Module aliases
     */
    volatile Set<String> aliases = new HashSet<>();

    /**
     * The linkage state.
     */
    private volatile Linkage linkage = Linkage.NONE;

    // private constants

    private static final RuntimePermission GET_DEPENDENCIES;
    private static final RuntimePermission GET_CLASS_LOADER;
    private static final RuntimePermission GET_BOOT_MODULE_LOADER;
    private static final RuntimePermission ACCESS_MODULE_LOGGER;
    private static final RuntimePermission ADD_CONTENT_HANDLER_FACTORY;
    private static final RuntimePermission ADD_URL_STREAM_HANDLER_FACTORY;

    private static final PermissionCollection NO_PERMISSIONS = noPermissions();

    /**
     * Construct a new instance from a module specification.
     *
     * @param spec the module specification
     * @param moduleLoader the module loader
     */
    Module(final ConcreteModuleSpec spec, final ModuleLoader moduleLoader) {
        this.moduleLoader = moduleLoader;

        // Initialize state from the spec.
        name = spec.getName();
        mainClassName = spec.getMainClass();
        fallbackLoader = spec.getFallbackLoader();
        final PermissionCollection permissionCollection = spec.getPermissionCollection();
        this.permissionCollection = permissionCollection == null ? NO_PERMISSIONS : permissionCollection.isReadOnly() ? permissionCollection : copyPermissions(permissionCollection);
        //noinspection ThisEscapedInObjectConstruction
        final ModuleClassLoader.Configuration configuration = new ModuleClassLoader.Configuration(this, spec.getAssertionSetting(), spec.getResourceLoaders(), spec.getClassFileTransformer());
        final ModuleClassLoaderFactory factory = spec.getModuleClassLoaderFactory();
        ModuleClassLoader moduleClassLoader = null;
        if (factory != null) moduleClassLoader = factory.create(configuration);
        if (moduleClassLoader == null) moduleClassLoader = new ModuleClassLoader(configuration);
        this.moduleClassLoader = moduleClassLoader;
        final Map<String, String> properties = spec.getProperties();
        this.properties = properties.isEmpty() ? Collections.<String, String>emptyMap() : new LinkedHashMap<String, String>(properties);
        this.version = spec.getVersion();
    }

    private static PermissionCollection noPermissions() {
        final Permissions permissions = new Permissions();
        permissions.setReadOnly();
        return permissions;
    }

    private static PermissionCollection copyPermissions(PermissionCollection permissionCollection) {
        final Permissions permissions = new Permissions();
        final Enumeration<Permission> elements = permissionCollection.elements();
        while (elements.hasMoreElements()) {
            permissions.add(elements.nextElement());
        }
        permissions.setReadOnly();
        return permissions;
    }

    LocalLoader getFallbackLoader() {
        return fallbackLoader;
    }

    Dependency[] getDependenciesInternal() {
        return linkage.getDependencies();
    }

    DependencySpec[] getDependencySpecsInternal() {
        return linkage.getDependencySpecs();
    }

    ModuleClassLoader getClassLoaderPrivate() {
        return moduleClassLoader;
    }

    /**
     * Get the current dependencies of this module.
     *
     * @return the current dependencies of this module
     * @throws SecurityException if a security manager is enabled and the caller does not have the {@code getDependencies}
     * {@link RuntimePermission}
     */
    public DependencySpec[] getDependencies() throws SecurityException {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(GET_DEPENDENCIES);
        }
        return getDependencySpecsInternal().clone();
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
        if (mainClassName == null) {
            throw new NoSuchMethodException("No main class defined for " + this);
        }
        final ClassLoader oldClassLoader = SecurityActions.setContextClassLoader(moduleClassLoader);
        try {
            final Class<?> mainClass = Class.forName(mainClassName, false, moduleClassLoader);
            try {
                Class.forName(mainClassName, true, moduleClassLoader);
            } catch (Throwable t) {
                throw new InvocationTargetException(t, "Failed to initialize main class '" + mainClassName + "'");
            }
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final MethodHandle methodHandle;
            try {
                methodHandle = lookup.findStatic(mainClass, "main", MAIN_METHOD_TYPE);
            } catch (IllegalAccessException e) {
                throw new NoSuchMethodException("The main method is not public");
            }
            try {
                methodHandle.invokeExact(args);
            } catch (Throwable throwable) {
                throw new InvocationTargetException(throwable);
            }
        } finally {
            SecurityActions.setContextClassLoader(oldClassLoader);
        }
    }

    /**
     * Get this module's identifier.
     *
     * @return the identifier
     * @deprecated Use {@link #getName()} instead.
     */
    @Deprecated
    public ModuleIdentifier getIdentifier() {
        return ModuleIdentifier.fromString(getName());
    }

    /**
     * Get this module's name.
     *
     * @return this module's name
     */
    public String getName() {
        return name;
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
     * Load a service loader from this module, without looking at dependencies.
     *
     * @param serviceType the service type class
     * @param <S> the service type
     * @return the service loader
     */
    public <S> ServiceLoader<S> loadServiceDirectly(Class<S> serviceType) {
        return ServiceLoader.load(serviceType, new ClassLoader(null) {
            public Enumeration<URL> getResources(final String name) throws IOException {
                final Enumeration<Resource> resourceEnumeration = Collections.enumeration(getClassLoader().getLocalLoader().loadResourceLocal(name));
                return new Enumeration<URL>() {
                    public boolean hasMoreElements() {
                        return resourceEnumeration.hasMoreElements();
                    }

                    public URL nextElement() {
                        return resourceEnumeration.nextElement().getURL();
                    }
                };
            }
        });
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
     * @deprecated Use {@link #loadServiceFromCallerModuleLoader(String, Class)} instead.
     */
    @Deprecated
    public static <S> ServiceLoader<S> loadServiceFromCallerModuleLoader(ModuleIdentifier identifier, Class<S> serviceType) throws ModuleLoadException {
        return loadServiceFromCallerModuleLoader(identifier.toString(), serviceType);
    }

    /**
     * Load a service loader from a module in the caller's module loader. The caller's
     * module loader refers to the loader of the module of the class that calls this method.
     * Note that {@link #loadService(Class)} is more efficient since it does not need to crawl
     * the stack.
     *
     * @param <S> the the service type
     * @param name the module name containing the service loader
     * @param serviceType the service type class
     * @return the loaded service from the caller's module
     * @throws ModuleLoadException if the named module failed to load
     */
    public static <S> ServiceLoader<S> loadServiceFromCallerModuleLoader(String name, Class<S> serviceType) throws ModuleLoadException {
        return getCallerModuleLoader().loadModule(name).loadService(serviceType);
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
        return Collections.unmodifiableSet(getPathsUnchecked().keySet());
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
        } else if (search && cl != null) {
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
     * @deprecated Use {@link #getModuleFromCallerModuleLoader(String)} instead.
     */
    @Deprecated
    public static Module getModuleFromCallerModuleLoader(final ModuleIdentifier identifier) throws ModuleLoadException {
        return getModuleFromCallerModuleLoader(identifier.toString());
    }

    /**
     * Get a module from the current module loader. Note that this must crawl the
     * stack to determine this, so other mechanisms are more efficient.
     * @see #getCallerModuleLoader()
     *
     * @param name the module name
     * @return the module
     * @throws ModuleLoadException if the module could not be loaded
     */
    public static Module getModuleFromCallerModuleLoader(final String name) throws ModuleLoadException {
        return getCallerModuleLoader().loadModule(name);
    }

    /**
     * Get the caller's module. The caller's module is the module containing the method that calls this
     * method. Note that this method crawls the stack so other ways of obtaining the
     * module are more efficient.
     *
     * @return the current module
     */
    public static Module getCallerModule() {
        return forClass(JDKSpecific.getCallingUserClass());
    }

    /**
     * Get the module with the given identifier from the module loader used by this module.
     *
     * @param identifier the module identifier
     * @return the module
     * @throws ModuleLoadException if an error occurs
     * @deprecated Use {@link #getModule(String)} instead.
     */
    @Deprecated
    public Module getModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        return getModule(identifier.toString());
    }

    /**
     * Get the module with the given identifier from the module loader used by this module.
     *
     * @param name the module name
     * @return the module
     * @throws ModuleLoadException if an error occurs
     */
    public Module getModule(final String name) throws ModuleLoadException {
        return moduleLoader.loadModule(name);
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
     * @deprecated Use {@link #loadClassFromBootModuleLoader(String, String)} instead.
     */
    @Deprecated
    public static Class<?> loadClassFromBootModuleLoader(final ModuleIdentifier moduleIdentifier, final String className)
            throws ModuleLoadException, ClassNotFoundException {
        return loadClassFromBootModuleLoader(moduleIdentifier.toString(), className);
    }

    /**
     * Load a class from a module in the system module loader.
     *
     * @see #getBootModuleLoader()
     *
     * @param name the name of the module from which the class
     *        should be loaded
     * @param className the class name to load
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClassFromBootModuleLoader(final String name, final String className)
            throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, true, getBootModuleLoader().loadModule(name).getClassLoader());
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
     * @deprecated Use {@link #loadClassFromCallerModuleLoader(String, String)} instead.
     */
    public static Class<?> loadClassFromCallerModuleLoader(final ModuleIdentifier moduleIdentifier, final String className)
            throws ModuleLoadException, ClassNotFoundException {
        return loadClassFromCallerModuleLoader(moduleIdentifier.toString(), className);
    }

    /**
     * Load a class from a module in the caller's module loader.
     *
     * @see #getCallerModuleLoader()
     *
     * @param name the name of the module from which the class
     *        should be loaded
     * @param className the class name to load
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClassFromCallerModuleLoader(final String name, final String className)
            throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, true, getModuleFromCallerModuleLoader(name).getClassLoader());
    }

    /**
     * Load a class from a local loader.
     *
     * @param className the class name
     * @param resolve {@code true} to resolve the class after definition
     * @return the class
     */
    Class<?> loadModuleClass(final String className, final boolean resolve) throws ClassNotFoundException {
        for (String s : systemPackages) {
            if (className.startsWith(s)) {
                return moduleClassLoader.loadClass(className, resolve);
            }
        }
        final String path = pathOfClass(className);
        final Map<String, List<LocalLoader>> paths = getPathsUnchecked();
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
     * @return the resource URL, or {@code null} if not found
     */
    URL getResource(final String name) {
        final String canonPath = PathUtils.canonicalize(name);
        for (String s : Module.systemPaths) {
            if (canonPath.startsWith(s)) {
                return moduleClassLoader.getResource(canonPath);
            }
        }
        log.trace("Attempting to find resource %s in %s", canonPath, this);
        final String path = pathOf(canonPath);
        final Map<String, List<LocalLoader>> paths = getPathsUnchecked();
        final List<LocalLoader> loaders = paths.get(path);
        if (loaders != null) {
            for (LocalLoader loader : loaders) {
                final List<Resource> resourceList = loader.loadResourceLocal(canonPath);
                for (Resource resource : resourceList) {
                    return resource.getURL();
                }
            }
        }
        final LocalLoader fallbackLoader = this.fallbackLoader;
        if (fallbackLoader != null) {
            final List<Resource> resourceList = fallbackLoader.loadResourceLocal(canonPath);
            for (Resource resource : resourceList) {
                return resource.getURL();
            }
        }
        return null;
    }

    /**
     * Load a resource from a local loader.
     *
     * @param name the resource name
     * @return the resource stream, or {@code null} if not found
     */
    InputStream getResourceAsStream(final String name) throws IOException {
        final String canonPath = PathUtils.canonicalize(name);
        for (String s : Module.systemPaths) {
            if (canonPath.startsWith(s)) {
                return moduleClassLoader.getResourceAsStream(canonPath);
            }
        }
        log.trace("Attempting to find resource %s in %s", canonPath, this);
        final String path = pathOf(canonPath);
        final Map<String, List<LocalLoader>> paths = getPathsUnchecked();
        final List<LocalLoader> loaders = paths.get(path);
        if (loaders != null) {
            for (LocalLoader loader : loaders) {
                final List<Resource> resourceList = loader.loadResourceLocal(canonPath);
                for (Resource resource : resourceList) {
                    return resource.openStream();
                }
            }
        }
        final LocalLoader fallbackLoader = this.fallbackLoader;
        if (fallbackLoader != null) {
            final List<Resource> resourceList = fallbackLoader.loadResourceLocal(canonPath);
            for (Resource resource : resourceList) {
                return resource.openStream();
            }
        }
        return null;
    }

    /**
     * Load all resources of a given name from a local loader.
     *
     * @param name the resource name
     * @return the enumeration of all the matching resource URLs (may be empty)
     */
    Enumeration<URL> getResources(final String name) {
        final String canonPath = PathUtils.canonicalize(PathUtils.relativize(name));
        for (String s : Module.systemPaths) {
            if (canonPath.startsWith(s)) {
                try {
                    return moduleClassLoader.getResources(canonPath);
                } catch (IOException e) {
                    return ConcurrentClassLoader.EMPTY_ENUMERATION;
                }
            }
        }
        log.trace("Attempting to find all resources %s in %s", canonPath, this);
        final String path = pathOf(canonPath);
        final Map<String, List<LocalLoader>> paths = getPathsUnchecked();
        final List<LocalLoader> loaders = paths.get(path);

        final List<URL> list = new ArrayList<URL>();
        if (loaders != null) {
            for (LocalLoader loader : loaders) {
                final List<Resource> resourceList = loader.loadResourceLocal(canonPath);
                for (Resource resource : resourceList) {
                    list.add(resource.getURL());
                }
            }
        }
        final LocalLoader fallbackLoader = this.fallbackLoader;
        if (fallbackLoader != null) {
            final List<Resource> resourceList = fallbackLoader.loadResourceLocal(canonPath);
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
        return getResource(name);
    }

    /**
     * Get all exported resource URLs for a resource name.
     *
     * @param name the resource name
     * @return the resource URLs
     */
    public Enumeration<URL> getExportedResources(final String name) {
        return getResources(name);
    }

    /**
     * Enumerate all the imported resources in this module, subject to a path filter.  The filter applies to
     * the containing path of each resource.
     *
     * @param filter the filter to apply to the search
     * @return the resource iterator (possibly empty)
     * @throws ModuleLoadException if linking a dependency module fails for some reason
     */
    public Iterator<Resource> iterateResources(final PathFilter filter) throws ModuleLoadException {
        final Map<String, List<LocalLoader>> paths = getPaths();
        final Iterator<Map.Entry<String, List<LocalLoader>>> iterator = paths.entrySet().iterator();
        return new Iterator<Resource>() {

            private String path;
            private Iterator<Resource> resourceIterator;
            private Iterator<LocalLoader> loaderIterator;
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
                            final LocalLoader loader = loaderIterator.next();
                            if (loader instanceof IterableLocalLoader) {
                                resourceIterator = ((IterableLocalLoader)loader).iterateResources(path, false);
                                continue;
                            }
                        }
                        loaderIterator = null;
                    }
                    if (! iterator.hasNext()) {
                        return false;
                    }
                    final Map.Entry<String, List<LocalLoader>> entry = iterator.next();
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
     * Enumerate all imported resources in this module which match the given glob expression.  The glob applies to
     * the whole resource name.
     *
     * @param glob the glob to apply
     * @return the iterator
     * @throws ModuleLoadException if linking a dependency module fails for some reason
     */
    public Iterator<Resource> globResources(final String glob) throws ModuleLoadException {
        String safeGlob = PathUtils.canonicalize(PathUtils.relativize(glob));
        final int i = safeGlob.lastIndexOf('/');
        if (i == -1) {
            return PathFilters.filtered(PathFilters.match(glob), iterateResources(PathFilters.acceptAll()));
        } else {
            return PathFilters.filtered(PathFilters.match(glob.substring(i + 1)), iterateResources(PathFilters.match(glob.substring(0, i))));
        }
    }

    /**
     * Get the (unmodifiable) set of paths which are imported into this module class loader, including local paths.  The
     * set will include all paths defined by the module's resource loaders, minus any paths excluded by filters.  The
     * set will generally always contain an empty entry ("").  The set is unordered and unsorted, and is iterable in
     * O(n) time and accessible in O(1) time.
     *
     * @return the set of paths
     * @throws ModuleLoadException if the module was previously unlinked, and there was an exception while linking
     */
    public Set<String> getImportedPaths() throws ModuleLoadException {
        return Collections.unmodifiableSet(getPaths().keySet());
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
     * Get the property with the given name, or {@code null} if none was defined.
     *
     * @param name the property name
     * @return the property value
     */
    public String getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Get the property with the given name, or a default value if none was defined.
     *
     * @param name the property name
     * @param defaultVal the default value
     * @return the property value
     */
    public String getProperty(String name, String defaultVal) {
        return properties.containsKey(name) ? properties.get(name) : defaultVal;
    }

    /**
     * Get a copy of the list of property names.
     *
     * @return the property names list
     */
    public List<String> getPropertyNames() {
        return new ArrayList<String>(properties.keySet());
    }

    /**
     * Get the module version.
     *
     * @return the module version, or {@code null} if none was set
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Get the string representation of this module.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Module \"");
        b.append(getName());
        b.append("\"");
        if (version != null) {
            b.append(" version ").append(version);
        }
        b.append(" from ").append(moduleLoader);
        return b.toString();
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

    /**
     * Get the platform identifier.  This is the string that uniquely identifies the hardware and OS combination for
     * the current running system.
     *
     * @return the platform identifier
     */
    public static String getPlatformIdentifier() {
        return NativeLibraryResourceLoader.getArchName();
    }

    /**
     * Get the module's configured permission collection.
     *
     * @return the module permission collection
     */
    public PermissionCollection getPermissionCollection() {
        return permissionCollection;
    }

    // Linking and resolution

    static final class Visited {
        private final Module module;
        private final FastCopyHashSet<PathFilter> filters;
        private final FastCopyHashSet<ClassFilter> classFilters;
        private final FastCopyHashSet<PathFilter> resourceFilters;
        private final int hashCode;

        Visited(final Module module, final FastCopyHashSet<PathFilter> filters, final FastCopyHashSet<ClassFilter> classFilters, final FastCopyHashSet<PathFilter> resourceFilters) {
            this.module = module;
            this.filters = filters;
            this.classFilters = classFilters;
            this.resourceFilters = resourceFilters;
            hashCode = ((resourceFilters.hashCode() * 13 + classFilters.hashCode()) * 13 + filters.hashCode()) * 13 + module.hashCode();
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object other) {
            return other instanceof Visited && equals((Visited)other);
        }

        public boolean equals(Visited other) {
            return this == other || other != null && module == other.module && filters.equals(other.filters) && classFilters.equals(other.classFilters) && resourceFilters.equals(other.resourceFilters);
        }
    }

    private long addPaths(Dependency[] dependencies, Map<String, List<LocalLoader>> map, FastCopyHashSet<PathFilter> filterStack, FastCopyHashSet<ClassFilter> classFilterStack, final FastCopyHashSet<PathFilter> resourceFilterStack, Set<Visited> visited) throws ModuleLoadException {
        long subtract = 0L;
        moduleLoader.incScanCount();
        for (Dependency dependency : dependencies) {
            if (dependency instanceof ModuleDependency) {
                final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                final ModuleLoader moduleLoader = moduleDependency.getModuleLoader();
                final String name = moduleDependency.getName();
                final Module module;

                try {
                    long pauseStart = Metrics.getCurrentCPUTime();
                    try {
                        module = moduleLoader.preloadModule(name);
                    } finally {
                        subtract += Metrics.getCurrentCPUTime() - pauseStart;
                    }
                } catch (ModuleLoadException ex) {
                    if (moduleDependency.isOptional()) {
                        continue;
                    } else {
                        log.trace("Module %s, dependency %s preload failed: %s", getIdentifier(), moduleDependency.getName(), ex);
                        throw ex;
                    }
                }
                if (module == null) {
                    if (!moduleDependency.isOptional()) {
                        throw new ModuleNotFoundException(name);
                    }
                    continue;
                }

                final PathFilter importFilter = dependency.getImportFilter();
                final FastCopyHashSet<PathFilter> nestedFilters;
                final FastCopyHashSet<ClassFilter> nestedClassFilters;
                final FastCopyHashSet<PathFilter> nestedResourceFilters;
                if (filterStack.contains(importFilter)) {
                    nestedFilters = filterStack;
                } else {
                    nestedFilters = filterStack.clone();
                    nestedFilters.add(importFilter);
                }
                final ClassFilter classImportFilter = dependency.getClassImportFilter();
                if (classImportFilter == ClassFilters.acceptAll() || classFilterStack.contains(classImportFilter)) {
                    nestedClassFilters = classFilterStack;
                } else {
                    nestedClassFilters = classFilterStack.clone();
                    if (classImportFilter != ClassFilters.acceptAll()) nestedClassFilters.add(classImportFilter);
                }
                final PathFilter resourceImportFilter = dependency.getResourceImportFilter();
                if (resourceImportFilter == PathFilters.acceptAll() || resourceFilterStack.contains(resourceImportFilter)) {
                    nestedResourceFilters = resourceFilterStack;
                } else {
                    nestedResourceFilters = resourceFilterStack.clone();
                    if (resourceImportFilter != PathFilters.acceptAll()) nestedResourceFilters.add(resourceImportFilter);
                }
                subtract += module.addExportedPaths(module.getDependenciesInternal(), map, nestedFilters, nestedClassFilters, nestedResourceFilters, visited);
            } else if (dependency instanceof ModuleClassLoaderDependency) {
                final ModuleClassLoaderDependency classLoaderDependency = (ModuleClassLoaderDependency) dependency;
                LocalLoader localLoader = classLoaderDependency.getLocalLoader();
                for (Object filter : classFilterStack.getRawArray()) {
                    if (filter != null && filter != ClassFilters.acceptAll()) {
                        localLoader = createClassFilteredLocalLoader((ClassFilter) filter, localLoader);
                    }
                }
                for (Object filter : resourceFilterStack.getRawArray()) {
                    if (filter != null && filter != PathFilters.acceptAll()) {
                        localLoader = createPathFilteredLocalLoader((PathFilter) filter, localLoader);
                    }
                }
                ClassFilter classFilter = classLoaderDependency.getClassImportFilter();
                if (classFilter != ClassFilters.acceptAll()) {
                    localLoader = createClassFilteredLocalLoader(classFilter, localLoader);
                }
                PathFilter resourceFilter = classLoaderDependency.getResourceImportFilter();
                if (resourceFilter != PathFilters.acceptAll()) {
                    localLoader = createPathFilteredLocalLoader(resourceFilter, localLoader);
                }
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
                LocalLoader localLoader = localDependency.getLocalLoader();
                for (Object filter : classFilterStack.getRawArray()) {
                    if (filter != null && filter != ClassFilters.acceptAll()) {
                        localLoader = createClassFilteredLocalLoader((ClassFilter) filter, localLoader);
                    }
                }
                for (Object filter : resourceFilterStack.getRawArray()) {
                    if (filter != null && filter != PathFilters.acceptAll()) {
                        localLoader = createPathFilteredLocalLoader((PathFilter) filter, localLoader);
                    }
                }
                final ClassFilter classFilter = localDependency.getClassImportFilter();
                if (classFilter != ClassFilters.acceptAll()) {
                    localLoader = createClassFilteredLocalLoader(classFilter, localLoader);
                }
                final PathFilter resourceFilter = localDependency.getResourceImportFilter();
                if (resourceFilter != PathFilters.acceptAll()) {
                    localLoader = createPathFilteredLocalLoader(resourceFilter, localLoader);
                }
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
        return subtract;
    }

    private LocalLoader createPathFilteredLocalLoader(PathFilter filter, LocalLoader localLoader) {
        if (localLoader instanceof IterableLocalLoader)
            return LocalLoaders.createIterablePathFilteredLocalLoader(filter, (IterableLocalLoader) localLoader);
        else
            return LocalLoaders.createPathFilteredLocalLoader(filter, localLoader);
    }

    private LocalLoader createClassFilteredLocalLoader(ClassFilter filter, LocalLoader localLoader) {
        if (localLoader instanceof IterableLocalLoader)
            return LocalLoaders.createIterableClassFilteredLocalLoader(filter, (IterableLocalLoader) localLoader);
        else
            return LocalLoaders.createClassFilteredLocalLoader(filter, localLoader);
    }

    private long addExportedPaths(Dependency[] dependencies, Map<String, List<LocalLoader>> map, FastCopyHashSet<PathFilter> filterStack, FastCopyHashSet<ClassFilter> classFilterStack, final FastCopyHashSet<PathFilter> resourceFilterStack, Set<Visited> visited) throws ModuleLoadException {
        if (!visited.add(new Visited(this, filterStack, classFilterStack, resourceFilterStack))) {
            return 0L;
        }
        long subtract = 0L;
        moduleLoader.incScanCount();
        for (Dependency dependency : dependencies) {
            final PathFilter exportFilter = dependency.getExportFilter();
            // skip non-exported dependencies altogether
            if (exportFilter != PathFilters.rejectAll()) {
                if (dependency instanceof ModuleDependency) {
                    final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                    final ModuleLoader moduleLoader = moduleDependency.getModuleLoader();
                    final String name  = moduleDependency.getName();
                    final Module module;

                    try {
                        long pauseStart = Metrics.getCurrentCPUTime();
                        try {
                            module = moduleLoader.preloadModule(name);
                        } finally {
                            subtract += Metrics.getCurrentCPUTime() - pauseStart;
                        }
                    } catch (ModuleLoadException ex) {
                        if (moduleDependency.isOptional()) {
                            continue;
                        } else {
                            log.trace("Module %s, dependency %s preload failed: %s", getIdentifier(), moduleDependency.getName(), ex);
                            throw ex;
                        }
                    }
                    if (module == null) {
                        if (!moduleDependency.isOptional()) {
                            throw new ModuleNotFoundException(name);
                        }
                        continue;
                    }

                    final PathFilter importFilter = dependency.getImportFilter();
                    final FastCopyHashSet<PathFilter> nestedFilters;
                    final FastCopyHashSet<ClassFilter> nestedClassFilters;
                    final FastCopyHashSet<PathFilter> nestedResourceFilters;
                    if (filterStack.contains(importFilter) && filterStack.contains(exportFilter)) {
                        nestedFilters = filterStack;
                    } else {
                        nestedFilters = filterStack.clone();
                        nestedFilters.add(importFilter);
                        nestedFilters.add(exportFilter);
                    }
                    final ClassFilter classImportFilter = dependency.getClassImportFilter();
                    final ClassFilter classExportFilter = dependency.getClassExportFilter();
                    if ((classImportFilter == ClassFilters.acceptAll() || classFilterStack.contains(classImportFilter)) && (classExportFilter == ClassFilters.acceptAll() || classFilterStack.contains(classExportFilter))) {
                        nestedClassFilters = classFilterStack;
                    } else {
                        nestedClassFilters = classFilterStack.clone();
                        if (classImportFilter != ClassFilters.acceptAll()) nestedClassFilters.add(classImportFilter);
                        if (classExportFilter != ClassFilters.acceptAll()) nestedClassFilters.add(classExportFilter);
                    }
                    final PathFilter resourceImportFilter = dependency.getResourceImportFilter();
                    final PathFilter resourceExportFilter = dependency.getResourceExportFilter();
                    if ((resourceImportFilter == PathFilters.acceptAll() || resourceFilterStack.contains(resourceImportFilter)) && (resourceExportFilter == PathFilters.acceptAll() || resourceFilterStack.contains(resourceExportFilter))) {
                        nestedResourceFilters = resourceFilterStack;
                    } else {
                        nestedResourceFilters = resourceFilterStack.clone();
                        if (resourceImportFilter != PathFilters.acceptAll()) nestedResourceFilters.add(resourceImportFilter);
                        if (resourceExportFilter != PathFilters.acceptAll()) nestedResourceFilters.add(resourceExportFilter);
                    }
                    subtract += module.addExportedPaths(module.getDependenciesInternal(), map, nestedFilters, nestedClassFilters, nestedResourceFilters, visited);
                } else if (dependency instanceof ModuleClassLoaderDependency) {
                    final ModuleClassLoaderDependency classLoaderDependency = (ModuleClassLoaderDependency) dependency;
                    LocalLoader localLoader = classLoaderDependency.getLocalLoader();
                    for (Object filter : classFilterStack.getRawArray()) {
                        if (filter != null && filter != ClassFilters.acceptAll()) {
                            localLoader = createClassFilteredLocalLoader((ClassFilter) filter, localLoader);
                        }
                    }
                    for (Object filter : resourceFilterStack.getRawArray()) {
                        if (filter != null && filter != PathFilters.acceptAll()) {
                            localLoader = createPathFilteredLocalLoader((PathFilter) filter, localLoader);
                        }
                    }
                    ClassFilter classImportFilter = classLoaderDependency.getClassImportFilter();
                    if (classImportFilter != ClassFilters.acceptAll()) {
                        localLoader = createClassFilteredLocalLoader(classImportFilter, localLoader);
                    }
                    ClassFilter classExportFilter = classLoaderDependency.getClassExportFilter();
                    if (classExportFilter != ClassFilters.acceptAll()) {
                        localLoader = createClassFilteredLocalLoader(classExportFilter, localLoader);
                    }
                    PathFilter resourceImportFilter = classLoaderDependency.getResourceImportFilter();
                    if (resourceImportFilter != PathFilters.acceptAll()) {
                        localLoader = createPathFilteredLocalLoader(resourceImportFilter, localLoader);
                    }
                    PathFilter resourceExportFilter = classLoaderDependency.getResourceExportFilter();
                    if (resourceExportFilter != PathFilters.acceptAll()) {
                        localLoader = createPathFilteredLocalLoader(resourceExportFilter, localLoader);
                    }
                    final PathFilter importFilter = classLoaderDependency.getImportFilter();
                    final Set<String> paths = classLoaderDependency.getPaths();
                    for (String path : paths) {
                        boolean accept = ! "_private".equals(path);
                        if (accept) for (Object filter : filterStack.getRawArray()) {
                            if (filter != null && ! ((PathFilter)filter).accept(path)) {
                                accept = false; break;
                            }
                        }
                        if (accept && importFilter.accept(path) && exportFilter.accept(path)) {
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
                    LocalLoader localLoader = localDependency.getLocalLoader();
                    for (Object filter : classFilterStack.getRawArray()) {
                        if (filter != null && filter != ClassFilters.acceptAll()) {
                            localLoader = createClassFilteredLocalLoader((ClassFilter) filter, localLoader);
                        }
                    }
                    for (Object filter : resourceFilterStack.getRawArray()) {
                        if (filter != null && filter != PathFilters.acceptAll()) {
                            localLoader = createPathFilteredLocalLoader((PathFilter) filter, localLoader);
                        }
                    }
                    ClassFilter classFilter = localDependency.getClassExportFilter();
                    if (classFilter != ClassFilters.acceptAll()) {
                        localLoader = createClassFilteredLocalLoader(classFilter, localLoader);
                    }
                    classFilter = localDependency.getClassImportFilter();
                    if (classFilter != ClassFilters.acceptAll()) {
                        localLoader = createClassFilteredLocalLoader(classFilter, localLoader);
                    }
                    PathFilter resourceFilter = localDependency.getResourceExportFilter();
                    if (resourceFilter != PathFilters.acceptAll()) {
                        localLoader = createPathFilteredLocalLoader(resourceFilter, localLoader);
                    }
                    resourceFilter = localDependency.getResourceImportFilter();
                    if (resourceFilter != PathFilters.acceptAll()) {
                        localLoader = createPathFilteredLocalLoader(resourceFilter, localLoader);
                    }
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
        return subtract;
    }

    Map<String, List<LocalLoader>> getPaths() throws ModuleLoadException {
        Linkage oldLinkage = this.linkage;
        Linkage linkage;
        Linkage.State state = oldLinkage.getState();
        if (state == Linkage.State.LINKED) {
            return oldLinkage.getPaths();
        }
        // slow path loop
        boolean intr = false;
        try {
            for (;;) {
                synchronized (this) {
                    oldLinkage = this.linkage;
                    state = oldLinkage.getState();
                    while (state == Linkage.State.LINKING || state == Linkage.State.NEW) try {
                        wait();
                        oldLinkage = this.linkage;
                        state = oldLinkage.getState();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                    if (state == Linkage.State.LINKED) {
                        return oldLinkage.getPaths();
                    }
                    this.linkage = linkage = new Linkage(oldLinkage.getDependencySpecs(), oldLinkage.getDependencies(), Linkage.State.LINKING);
                    // fall out and link
                }
                boolean ok = false;
                try {
                    link(linkage);
                    ok = true;
                } finally {
                    if (! ok) {
                        // restore original (lack of) linkage
                        synchronized (this) {
                            if (this.linkage == linkage) {
                                this.linkage = oldLinkage;
                                notifyAll();
                            }
                        }
                    }
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    Map<String, List<LocalLoader>> getPathsUnchecked() {
        try {
            return getPaths();
        } catch (ModuleLoadException e) {
            throw e.toError();
        }
    }

    void link(final Linkage linkage) throws ModuleLoadException {
        final HashMap<String, List<LocalLoader>> importsMap = new HashMap<String, List<LocalLoader>>();
        final Dependency[] dependencies = linkage.getDependencies();
        final long start = Metrics.getCurrentCPUTime();
        long subtractTime = 0L;
        try {
            final Set<Visited> visited = new FastCopyHashSet<Visited>(16);
            final FastCopyHashSet<PathFilter> filterStack = new FastCopyHashSet<PathFilter>(8);
            final FastCopyHashSet<ClassFilter> classFilterStack = EMPTY_CLASS_FILTERS;
            final FastCopyHashSet<PathFilter> resourceFilterStack = EMPTY_PATH_FILTERS;
            subtractTime += addPaths(dependencies, importsMap, filterStack, classFilterStack, resourceFilterStack, visited);
            synchronized (this) {
                if (this.linkage == linkage) {
                    this.linkage = new Linkage(linkage.getDependencySpecs(), linkage.getDependencies(), Linkage.State.LINKED, importsMap);
                    notifyAll();
                }
                // else all our efforts were just wasted since someone changed the deps in the meantime
            }
        } finally {
            moduleLoader.addLinkTime(Metrics.getCurrentCPUTime() - start - subtractTime);
        }
    }

    void relinkIfNecessary() throws ModuleLoadException {
        Linkage oldLinkage = this.linkage;
        Linkage linkage;
        if (oldLinkage.getState() != Linkage.State.UNLINKED) {
            return;
        }
        synchronized (this) {
            oldLinkage = this.linkage;
            if (oldLinkage.getState() != Linkage.State.UNLINKED) {
                return;
            }
            this.linkage = linkage = new Linkage(oldLinkage.getDependencySpecs(), oldLinkage.getDependencies(), Linkage.State.LINKING);
        }
        boolean ok = false;
        try {
            link(linkage);
            ok = true;
        } finally {
            if (! ok) {
                // restore original (lack of) linkage
                synchronized (this) {
                    if (this.linkage == linkage) {
                        this.linkage = oldLinkage;
                        notifyAll();
                    }
                }
            }
        }
    }

    void relink() throws ModuleLoadException {
        link(linkage);
    }

    void setDependencies(final List<DependencySpec> dependencySpecs) {
        if (dependencySpecs == null) {
            throw new IllegalArgumentException("dependencySpecs is null");
        }
        final DependencySpec[] specs = dependencySpecs.toArray(new DependencySpec[dependencySpecs.size()]);
        for (DependencySpec spec : specs) {
            if (spec == null) {
                throw new IllegalArgumentException("dependencySpecs contains a null dependency specification");
            }
        }
        setDependencies(specs);
    }

    void setDependencies(final DependencySpec[] dependencySpecs) {
        synchronized (this) {
            linkage = new Linkage(dependencySpecs, calculateDependencies(dependencySpecs), Linkage.State.UNLINKED, null);
            notifyAll();
        }
    }

    private Dependency[] calculateDependencies(final DependencySpec[] dependencySpecs) {
        final Dependency[] dependencies = new Dependency[dependencySpecs.length];
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
        List<LocalLoader> loaders = getPathsUnchecked().get(name.replace('.', '/'));
        if (loaders != null) for (LocalLoader localLoader : loaders) {
            Package pkg = localLoader.loadPackageLocal(name);
            if (pkg != null) return pkg;
        }
        return null;
    }

    Package[] getPackages() {
        final ArrayList<Package> packages = new ArrayList<Package>();
        final Map<String, List<LocalLoader>> allPaths = getPathsUnchecked();
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
