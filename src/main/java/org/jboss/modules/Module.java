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
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
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
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.log.ModuleLogger;
import org.jboss.modules.log.NoopModuleLogger;

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
        GET_DEPENDENCIES = new RuntimePermission("getDependencies");
        GET_CLASS_LOADER = new RuntimePermission("getClassLoader");
        GET_BOOT_MODULE_LOADER = new RuntimePermission("getBootModuleLoader");
        ACCESS_MODULE_LOGGER = new RuntimePermission("accessModuleLogger");
        ADD_CONTENT_HANDLER_FACTORY = new RuntimePermission("addContentHandlerFactory");
        ADD_URL_STREAM_HANDLER_FACTORY = new RuntimePermission("addURLStreamHandlerFactory");

        final String pkgsString = AccessController.doPrivileged(new PropertyReadAction("jboss.modules.system.pkgs"));
        final List<String> list = new ArrayList<String>();
        JDKSpecific.addInternalPackages(list);
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
     * The linkage state.
     */
    private volatile LazyPaths lazyPaths;

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
        final Map<String, String> properties = spec.getProperties();
        this.properties = properties.isEmpty() ? Collections.emptyMap() : new LinkedHashMap<String, String>(properties);
        this.version = spec.getVersion();
        ModuleClassLoader moduleClassLoader = null;
        if (factory != null) moduleClassLoader = factory.create(configuration);
        if (moduleClassLoader == null) moduleClassLoader = new ModuleClassLoader(configuration);
        this.moduleClassLoader = moduleClassLoader;
        lazyPaths = new LazyPaths(DependencySpec.NO_DEPENDENCIES, Dependency.NO_DEPENDENCIES, moduleLoader);
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
        return lazyPaths.getDependencies();
    }

    DependencySpec[] getDependencySpecsInternal() {
        return lazyPaths.getDependencySpecs();
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
     * @deprecated Resource root names are deprecated.
     */
    @Deprecated
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
        run(mainClassName, args);
    }

    /**
     * Run the given main class in this module.
     *
     * @param className the class name to run (must not be {@code null})
     * @param args the arguments to pass
     * @throws NoSuchMethodException if there is no main method
     * @throws InvocationTargetException if the main method failed
     * @throws ClassNotFoundException if the main class is not found
     */
    public void run(final String className, final String[] args) throws NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        if (className == null) {
            throw new NoSuchMethodException("No main class defined for " + this);
        }
        final ClassLoader oldClassLoader = SecurityActions.setContextClassLoader(moduleClassLoader);
        try {
            final Class<?> mainClass = Class.forName(className, false, moduleClassLoader);
            try {
                Class.forName(className, true, moduleClassLoader);
            } catch (Throwable t) {
                throw new InvocationTargetException(t, "Failed to initialize main class '" + className + "'");
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
        // TODO
        return Collections.emptySet();
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

    /**
     * Get the system module loader.  This is the module loader which contains the Java platform modules, plus
     * a module for JBoss Modules itself, called {@code org.jboss.modules}.
     *
     * @return the system module loader
     */
    public static ModuleLoader getSystemModuleLoader() {
        return Utils.JDK_MODULE_LOADER;
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
        final LocalLoader[] loaders = getLoaders(path);
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
        final URLConnectionResource jaxpResource = ModuleClassLoader.jaxpImplResources.get(canonPath);
        final LocalLoader[] loaders = getLoaders(path);
        if (loaders != null) {
            for (LocalLoader loader : loaders) {
                final Iterator<Resource> iterator = loader.loadResourceLocal(canonPath).iterator();
                if (iterator.hasNext()) {
                    final URL url = iterator.next().getURL();
                    if (jaxpResource != null) log.jaxpResourceLoaded(url, this);
                    return url;
                }
            }
        }
        final LocalLoader fallbackLoader = this.fallbackLoader;
        if (fallbackLoader != null) {
            final Iterator<Resource> iterator = fallbackLoader.loadResourceLocal(canonPath).iterator();
            if (iterator.hasNext()) {
                final URL url = iterator.next().getURL();
                if (jaxpResource != null) log.jaxpResourceLoaded(url, this);
                return url;
            }
        }
        if (jaxpResource != null) {
            final URL url = jaxpResource.getURL();
            log.jaxpResourceLoaded(url, this);
            return url;
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
        final URLConnectionResource jaxpResource = ModuleClassLoader.jaxpImplResources.get(canonPath);
        final LocalLoader[] loaders = getLoaders(path);
        if (loaders != null) {
            for (LocalLoader loader : loaders) {
                final List<Resource> resourceList = loader.loadResourceLocal(canonPath);
                final Iterator<Resource> iterator = resourceList.iterator();
                if (iterator.hasNext()) {
                    final Resource resource = iterator.next();
                    if (jaxpResource != null) log.jaxpResourceLoaded(resource.getURL(), this);
                    return resource.openStream();
                }
            }
        }
        final LocalLoader fallbackLoader = this.fallbackLoader;
        if (fallbackLoader != null) {
            final List<Resource> resourceList = fallbackLoader.loadResourceLocal(canonPath);
            final Iterator<Resource> iterator = resourceList.iterator();
            if (iterator.hasNext()) {
                final Resource resource = iterator.next();
                if (jaxpResource != null) log.jaxpResourceLoaded(resource.getURL(), this);
                return resource.openStream();
            }
        }
        if (jaxpResource != null) {
            log.jaxpResourceLoaded(jaxpResource.getURL(), this);
            return jaxpResource.openStream();
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
        final URLConnectionResource jaxpResource = ModuleClassLoader.jaxpImplResources.get(canonPath);
        final LocalLoader[] loaders = getLoaders(path);

        final List<URL> list = new ArrayList<URL>();
        if (loaders != null) {
            for (LocalLoader loader : loaders) {
                final List<Resource> resourceList = loader.loadResourceLocal(canonPath);
                for (Resource resource : resourceList) {
                    final URL url = resource.getURL();
                    if (jaxpResource != null) log.jaxpResourceLoaded(url, this);
                    list.add(url);
                }
            }
        }
        final LocalLoader fallbackLoader = this.fallbackLoader;
        if (fallbackLoader != null) {
            final List<Resource> resourceList = fallbackLoader.loadResourceLocal(canonPath);
            for (Resource resource : resourceList) {
                final URL url = resource.getURL();
                if (jaxpResource != null) log.jaxpResourceLoaded(url, this);
                list.add(url);
            }
        }
        if (jaxpResource != null) {
            final URL url = jaxpResource.getURL();
            log.jaxpResourceLoaded(url, this);
            list.add(url);
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

    private void getMaximalPaths(Set<String> set, Set<Module> visited) throws ModuleLoadException {
        if (visited.add(this)) for (Dependency dependency : getDependenciesInternal()) {
            if (dependency instanceof ModuleDependency) {
                final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                final ModuleLoader moduleLoader = moduleDependency.getModuleLoader();
                final String name = moduleDependency.getName();
                try {
                    moduleLoader.loadModule(name).getMaximalPaths(set, visited);
                } catch (ModuleLoadException e) {
                    if (! moduleDependency.isOptional()) {
                        throw e;
                    }
                }
            } else if (dependency instanceof LocalDependency) {
                set.addAll(((LocalDependency) dependency).getPaths());
            } else if (dependency instanceof ModuleClassLoaderDependency) {
                set.addAll(((ModuleClassLoaderDependency) dependency).getPaths());
            }
        }
    }

    private LocalLoader[] getLoaders(String path) {
        return lazyPaths.getLoaders(path).getItems();
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
        // first build the maximal possible path set
        final Set<String> pathSet = new HashSet<>(128);
        getMaximalPaths(pathSet, new HashSet<>(32));
        final Iterator<String> pathIterator = pathSet.iterator();
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
                    if (! pathIterator.hasNext()) {
                        return false;
                    }
                    path = pathIterator.next();
                    if (filter.accept(path)) {
                        loaderIterator = Arrays.asList(getLoaders(path)).iterator();
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
        // todo
        return Collections.emptySet();
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

    LazyPaths getLazyPaths() {
        return lazyPaths;
    }

    void relink() {
        lazyPaths = new LazyPaths(lazyPaths.getDependencySpecs(), lazyPaths.getDependencies(), moduleLoader);
    }

    void relinkDependencies() throws ModuleLoadException {
        Set<Module> set = null;
        for (Dependency dependency : getDependenciesInternal()) {
            if (dependency instanceof ModuleDependency) {
                if (set == null) {
                    set = new HashSet<>();
                    set.add(this);
                }
                ((ModuleDependency)dependency).load(set);
            }
        }
    }

    void setDependencies(final List<DependencySpec> dependencySpecs) {
        if (dependencySpecs == null) {
            throw new IllegalArgumentException("dependencySpecs is null");
        }
        final DependencySpec[] specs = dependencySpecs.toArray(DependencySpec.NO_DEPENDENCIES);
        for (DependencySpec spec : specs) {
            if (spec == null) {
                throw new IllegalArgumentException("dependencySpecs contains a null dependency specification");
            }
        }
        setDependencies(specs);
    }

    void setDependencies(final DependencySpec[] dependencySpecs) {
        synchronized (this) {
            lazyPaths = new LazyPaths(dependencySpecs, calculateDependencies(dependencySpecs), moduleLoader);
            notifyAll();
        }
    }

    Dependency[] calculateDependencies(final DependencySpec[] dependencySpecs) {
        final Dependency[] dependencies = new Dependency[dependencySpecs.length];
        int i = 0;
        for (DependencySpec spec : dependencySpecs) {
            dependencies[i++] = spec.getDependency(this);
        }
        return dependencies;
    }

    String getMainClass() {
        return mainClassName;
    }

    Package getPackage(final String name) {
        LocalLoader[] loaders = getLoaders(name.replace('.', '/'));
        if (loaders != null) for (LocalLoader localLoader : loaders) {
            Package pkg = localLoader.loadPackageLocal(name);
            if (pkg != null) return pkg;
        }
        return null;
    }

    Package[] getPackages() {
        final ArrayList<Package> packages = new ArrayList<Package>();
        final Set<String> pathSet = lazyPaths.getCurrentPathSet();
        next: for (String path : pathSet) {
            String packageName = path.replace('/', '.');
            for (LocalLoader loader : lazyPaths.getLoaders(path).getItems()) {
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
