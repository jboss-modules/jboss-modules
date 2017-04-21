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

import static java.security.AccessController.doPrivileged;
import static org.jboss.modules.management.ObjectProperties.property;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.modules.log.ModuleLogger;
import org.jboss.modules.management.DependencyInfo;
import org.jboss.modules.management.ModuleInfo;
import org.jboss.modules.management.ModuleLoaderMXBean;
import org.jboss.modules.management.ObjectProperties;
import org.jboss.modules.management.ResourceLoaderInfo;
import org.jboss.modules.ref.Reaper;
import org.jboss.modules.ref.Reference;
import org.jboss.modules.ref.WeakReference;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * A repository for modules, from which a module may be loaded by identifier.  Module loaders may additionally
 * delegate to one or more other module loaders.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author Jason T. Greene
 *
 * @apiviz.landmark
 */
public class ModuleLoader {

    private static final RuntimePermission ML_PERM = new RuntimePermission("canCreateModuleLoader");
    private static final RuntimePermission MODULE_REDEFINE_PERM = new RuntimePermission("canRedefineModule");
    private static final RuntimePermission MODULE_REDEFINE_ANY_PERM = new RuntimePermission("canRedefineAnyModule");
    private static final RuntimePermission MODULE_UNLOAD_ANY_PERM = new RuntimePermission("canUnloadAnyModule");
    private static final RuntimePermission MODULE_ITERATE_PERM = new RuntimePermission("canIterateModules");

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final Class<?>[] JUST_MODULE_IDENTIFIER = { ModuleIdentifier.class };

    private static volatile MBeanReg REG_REF = new TempMBeanReg();

    /**
     * A constant representing zero module finders.
     */
    public static final ModuleFinder[] NO_FINDERS = new ModuleFinder[0];

    private final ConcurrentMap<String, FutureModule> moduleMap = new ConcurrentHashMap<>();
    private final ModuleFinder[] finders;

    private final boolean canRedefine;
    private final ModuleLoaderMXBean mxBean;

    private final AtomicLong linkTime = new AtomicLong();
    private final AtomicLong loadTime = new AtomicLong();
    private final AtomicLong classLoadTime = new AtomicLong();
    private final AtomicInteger scanCount = new AtomicInteger();
    private final AtomicInteger raceCount = new AtomicInteger();
    private final AtomicInteger classCount = new AtomicInteger();

    private final boolean overridesFindModuleByIdentifier;
    private final boolean overridesPreloadModuleByIdentifier;

    ModuleLoader(boolean canRedefine, boolean skipRegister) {
        this(canRedefine, skipRegister, NO_FINDERS);
    }

    // Bypass security check for classes in this package
    ModuleLoader(boolean canRedefine, boolean skipRegister, ModuleFinder[] finders) {
        this.canRedefine = canRedefine;
        this.finders = finders;
        mxBean = skipRegister ? null : doPrivileged(new PrivilegedAction<ModuleLoaderMXBean>() {
            public ModuleLoaderMXBean run() {
                ObjectName objectName;
                try {
                    objectName = new ObjectName("jboss.modules", ObjectProperties.properties(property("type", "ModuleLoader"), property("name", ModuleLoader.this.getClass().getSimpleName() + "-" + Integer.toString(SEQ.incrementAndGet()))));
                } catch (MalformedObjectNameException e) {
                    return null;
                }
                try {
                    MXBeanImpl mxBean = new MXBeanImpl(ModuleLoader.this, objectName);
                    REG_REF.addMBean(objectName, mxBean);
                    return mxBean;
                } catch (Throwable ignored) {
                }
                return null;
            }
        });
        // determine if the subclass overrides the crusty old find-module-by-identifier method
        boolean foundFindModule = false;
        boolean foundPreloadModule = false;
        for (Class<? extends ModuleLoader> clazz = getClass(); clazz != ModuleLoader.class; clazz = clazz.getSuperclass().asSubclass(ModuleLoader.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if ((method.getModifiers() & (Modifier.STATIC | Modifier.PRIVATE)) == 0) {
                    if (method.getName().equals("findModule")) {
                        if (ModuleSpec.class.isAssignableFrom(method.getReturnType())) {
                            if (Arrays.equals(method.getParameterTypes(), JUST_MODULE_IDENTIFIER)) {
                                foundFindModule = true;
                                if (foundPreloadModule) break;
                            }
                        }
                    } else if (method.getName().equals("preloadModule")) {
                        if (Module.class.isAssignableFrom(method.getReturnType())) {
                            if (Arrays.equals(method.getParameterTypes(), JUST_MODULE_IDENTIFIER)) {
                                foundPreloadModule = true;
                                if (foundFindModule) break;
                            }
                        }
                    }
                }
            }
        }
        overridesFindModuleByIdentifier = foundFindModule;
        overridesPreloadModuleByIdentifier = foundPreloadModule;
    }

    /**
     * Construct a new instance.
     */
    protected ModuleLoader() {
        this(NO_FINDERS);
    }

    /**
     * Construct a new instance.
     *
     * @param finders the module finders to search, in order
     */
    public ModuleLoader(final ModuleFinder[] finders) {
        this(checkPermissions(), false, safeClone(finders));
    }

    private static ModuleFinder[] safeClone(ModuleFinder[] finders) {
        if (finders == null || finders.length == 0) {
            return NO_FINDERS;
        }
        finders = finders.clone();
        for (ModuleFinder finder : finders) {
            if (finder == null) {
                throw new IllegalArgumentException("Module finder cannot be null");
            }
        }
        return finders;
    }

    private static boolean checkPermissions() {
        SecurityManager manager = System.getSecurityManager();
        if (manager == null) {
            return true;
        }
        manager.checkPermission(ML_PERM);
        try {
            manager.checkPermission(MODULE_REDEFINE_PERM);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Get the module loader for a class.
     *
     * @param clazz the class
     *
     * @return the module loader or {@code null} if the class's class loader does not belong to a module loader.
     */
    public static ModuleLoader forClass(Class<?> clazz) {
        final Module module = Module.forClass(clazz);
        if (module == null) {
            return null;
        }
        return module.getModuleLoader();
    }

    /**
     * Get the module loader for a class loader.
     *
     * @param classLoader the class loader
     *
     * @return the module loader or {@code null} if the class loader does not belong to a module loader.
     */
    public static ModuleLoader forClassLoader(ClassLoader classLoader) {
        final Module module = Module.forClassLoader(classLoader, true);
        if (module == null) {
            return null;
        }
        return module.getModuleLoader();
    }

    /**
     * Get the string representation of this module loader.
     *
     * @return the string representation
     */
    public String toString() {
        return String.format("%s@%x for finders %s", getClass().getSimpleName(), hashCode(), Arrays.toString(finders));
    }

    /**
     * Get a string representation of the given module, used in debug output and stack traces.  This method may be
     * overridden to provide a more detailed description.  By default it returns a string consisting of the module's
     * name and version (if any).
     *
     * @param module the module to describe
     * @return the description string
     */
    public String getModuleDescription(final Module module) {
        final Version version = module.getVersion();
        return version == null ? module.getName() : module.getName() + "@" + version;
    }

    static void installMBeanServer() {
        REG_REF.installReal();
    }

    /**
     * Load a module based on an identifier.  This method delegates to {@link #preloadModule(String)} and then
     * links the returned module if necessary.
     *
     * @param identifier The module identifier
     * @return The loaded Module
     * @throws ModuleLoadException if the Module can not be loaded
     * @deprecated Use {@link #loadModule(String)} instead.
     */
    @Deprecated
    public final Module loadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        return loadModule(identifier.toString());
    }

    /**
     * Load a module based on an identifier.  This method delegates to {@link #preloadModule(String)} and then
     * links the returned module if necessary.
     *
     * @param name The module name
     * @return The loaded Module
     * @throws ModuleLoadException if the Module can not be loaded
     */
    public final Module loadModule(String name) throws ModuleLoadException {
        final Module module = preloadModule(name);
        if (module == null) {
            throw new ModuleNotFoundException(name);
        }
        module.relinkIfNecessary();
        return module;
    }

    /**
     * Iterate the modules which can be located via this module loader.
     *
     * @param baseIdentifier the identifier to start with, or {@code null} to iterate all modules
     * @param recursive {@code true} to find recursively nested modules, {@code false} to only find immediately nested modules
     * @return an iterator for the modules in this module finder
     * @throws SecurityException if the caller does not have permission to iterate module loaders
     * @deprecated Use {@link #iterateModules(String, boolean)} instead.
     */
    @Deprecated
    public final Iterator<ModuleIdentifier> iterateModules(final ModuleIdentifier baseIdentifier, final boolean recursive) {
        return IteratorUtils.transformingIterator(iterateModules(baseIdentifier == null ? (String) null : baseIdentifier.toString(), recursive), ModuleIdentifier::fromString);
    }

    /**
     * Iterate the modules which can be located via this module loader.
     *
     * @param baseName the identifier to start with, or {@code null} to iterate all modules; ignored if this module
     * loader does not have a concept of nested modules
     * @param recursive {@code true} to find recursively nested modules, {@code false} to only find immediately nested
     * modules; ignored if this module finder does not have a concept of nested modules
     * @return an iterator for the modules in this module finder
     * @throws SecurityException if the caller does not have permission to iterate module loaders
     */
    public final Iterator<String> iterateModules(final String baseName, final boolean recursive) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(MODULE_ITERATE_PERM);
        }
        return new Iterator<String>() {
            int idx;
            Iterator<String> nested;

            public boolean hasNext() {
                for (;;) {
                    while (nested == null) {
                        if (idx == finders.length) {
                            return false;
                        }
                        final ModuleFinder finder = finders[idx++];
                        if (finder instanceof IterableModuleFinder) {
                            nested = ((IterableModuleFinder) finder).iterateModules(baseName, recursive);
                        }
                    }

                    if (! nested.hasNext()) {
                        nested = null;
                    } else {
                        return true;
                    }
                }
            }

            public String next() {
                if (! hasNext()) {
                    throw new NoSuchElementException();
                }
                return nested.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * This compatibility method just calls {@code preloadModule(methodIdentifier.toString())} and is usually not used;
     * if however a legacy subclass overrides this method, then it will be called instead of {@link #preloadModule(String)},
     * and that method will redirect to this one.
     *
     * @deprecated Use {@link #preloadModule(String)} instead.
     *
     * @param identifier the module identifier (must not be {@code null})
     * @return the load result, or {@code null} if the module is not found
     * @throws ModuleLoadException if an error occurs
     */
    @Deprecated
    protected Module preloadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        if (overridesPreloadModuleByIdentifier) {
            return loadModuleLocal(identifier);
        } else {
            return preloadModule(identifier.toString());
        }
    }

    /**
     * Preload a module based on an identifier.  By default, no delegation is done and this method simply invokes
     * {@link #loadModuleLocal(String)}.  A delegating module loader may delegate to the appropriate module
     * loader based on loader-specific criteria (via the {@link #preloadModule(String, ModuleLoader)} method).
     *
     * @param name the module identifier
     * @return the load result, or {@code null} if the module is not found
     * @throws ModuleLoadException if an error occurs
     */
    protected Module preloadModule(String name) throws ModuleLoadException {
        if (overridesPreloadModuleByIdentifier) {
            return preloadModule(ModuleIdentifier.fromString(name));
        } else {
            return loadModuleLocal(name);
        }
    }

    /**
     * Preload an "exported" module based on an identifier.  By default this simply delegates to {@link #preloadModule(String)}.
     *
     * @param name the module name
     * @return the load result, or {@code null} if the module is not found
     * @throws ModuleLoadException if an error occurs
     */
    protected Module preloadExportedModule(String name) throws ModuleLoadException {
        return preloadModule(name);
    }

    /**
     * Preload an "exported" module based on an identifier.  By default this simply delegates to {@link #preloadModule(String)}.
     *
     * @param identifier the module identifier
     * @return the load result, or {@code null} if the module is not found
     * @throws ModuleLoadException if an error occurs
     * @deprecated Use {@link #preloadModule(String)} instead.
     */
    @Deprecated
    protected Module preloadExportedModule(ModuleIdentifier identifier) throws ModuleLoadException {
        return preloadModule(identifier.getName());
    }

    /**
     * Utility method to delegate to another module loader, accessible from subclasses.  The delegate module loader
     * will be queried for exported modules only.
     *
     * @param name the module name
     * @param moduleLoader the module loader to delegate to
     * @return the delegation result
     * @throws ModuleLoadException if an error occurs
     */
    protected static Module preloadModule(String name, ModuleLoader moduleLoader) throws ModuleLoadException {
        return moduleLoader.preloadExportedModule(name);
    }

    /**
     * Utility method to delegate to another module loader, accessible from subclasses.  The delegate module loader
     * will be queried for exported modules only.
     *
     * @param identifier the module identifier
     * @param moduleLoader the module loader to delegate to
     * @return the delegation result
     * @throws ModuleLoadException if an error occurs
     * @deprecated Use {@link #preloadModule(String, ModuleLoader)} instead.
     */
    @Deprecated
    protected static Module preloadModule(ModuleIdentifier identifier, ModuleLoader moduleLoader) throws ModuleLoadException {
        return moduleLoader.preloadExportedModule(identifier.toString());
    }

    /**
     * Try to load a module from this module loader.  Returns {@code null} if the module is not found.  The returned
     * module may not yet be resolved.  The returned module may have a different name than the given identifier if
     * the identifier is an alias for another module.
     *
     * @param moduleIdentifier the module identifier
     * @return the module
     * @throws ModuleLoadException if an error occurs while loading the module
     * @deprecated Use {@link #loadModuleLocal(String)} instead.
     */
    @Deprecated
    protected final Module loadModuleLocal(ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        return loadModuleLocal(moduleIdentifier.toString());
    }

    /**
     * Try to load a module from this module loader.  Returns {@code null} if the module is not found.  The returned
     * module may not yet be resolved.  The returned module may have a different name than the given identifier if
     * the identifier is an alias for another module.
     *
     * @param name the module name
     * @return the module
     * @throws ModuleLoadException if an error occurs while loading the module
     */
    protected final Module loadModuleLocal(String name) throws ModuleLoadException {
        FutureModule futureModule = moduleMap.get(name);
        if (futureModule != null) {
            return futureModule.getModule();
        }

        FutureModule newFuture = new FutureModule(name);
        futureModule = moduleMap.putIfAbsent(name, newFuture);
        if (futureModule != null) {
            return futureModule.getModule();
        }

        boolean ok = false;
        try {
            final ModuleLogger log = Module.log;
            log.trace("Locally loading module %s from %s", name, this);
            final long startTime = Metrics.getCurrentCPUTime();
            final ModuleSpec moduleSpec = findModule(name);
            loadTime.addAndGet(Metrics.getCurrentCPUTime() - startTime);
            if (moduleSpec == null) {
                log.trace("Module %s not found from %s", name, this);
                return null;
            }
            if (! moduleSpec.getName().equals(name)) {
                throw new ModuleLoadException("Module loader found a module with the wrong name");
            }
            final Module module;
            if (moduleSpec instanceof AliasModuleSpec) {
                final AliasModuleSpec aliasModuleSpec = (AliasModuleSpec) moduleSpec;
                Module aliasedModule;
                while (true) {
                    aliasedModule = loadModuleLocal(aliasModuleSpec.getAliasName());
                    if (aliasedModule == null) {
                        throw new ModuleLoadException("Alias module " + name + " is referencing not existing module");
                    }
                    final Set<String> aliases = aliasedModule.aliases;
                    if (aliases == null) continue; // race condition detected, retry
                    synchronized (aliases) {
                        if (aliases == aliasedModule.aliases) {
                            // no race-condition with removal, go ahead
                            aliases.add(moduleSpec.getName());
                            newFuture.setModule(module = aliasedModule);
                            log.trace("Added module %s as alias of %s from %s", moduleSpec.getName(), aliasModuleSpec.getAliasName(), this);
                            ok = true;
                            break;
                        }
                    }
                }
            } else {
                module = defineModule((ConcreteModuleSpec) moduleSpec, newFuture);
                log.trace("Loaded module %s from %s", name, this);
                ok = true;
            }
            return module;
        } finally {
            if (! ok) {
                newFuture.setModule(null);
                moduleMap.remove(name, newFuture);
            }
        }
    }

    /**
     * Find an already-loaded module, returning {@code null} if the module isn't currently loaded.  May block
     * while the loaded state of the module is in question (if the module is being concurrently loaded from another
     * thread, for example).
     *
     * @param identifier the module identifier
     * @return the module, or {@code null} if it wasn't found
     * @deprecated Use {@link #findLoadedModuleLocal(String)} instead.
     */
    @Deprecated
    protected final Module findLoadedModuleLocal(ModuleIdentifier identifier) {
        return findLoadedModuleLocal(identifier.toString());
    }

    /**
     * Find an already-loaded module, returning {@code null} if the module isn't currently loaded.  May block
     * while the loaded state of the module is in question (if the module is being concurrently loaded from another
     * thread, for example).
     *
     * @param name the module identifier
     * @return the module, or {@code null} if it wasn't found
     */
    protected final Module findLoadedModuleLocal(String name) {
        FutureModule futureModule = moduleMap.get(name);
        if (futureModule != null) {
            try {
                return futureModule.getModule();
            } catch (ModuleNotFoundException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Unload a module from this module loader.  Note that this has no effect on existing modules which refer to the
     * module being unloaded.  Also, only modules from the current module loader can be unloaded.  Unloading the same
     * module more than once has no additional effect.  This method only removes the mapping for the module; any running
     * threads which are currently accessing or linked to the module will continue to function, however attempts to load
     * this module will fail until a new module is loaded with the same name.  Once this happens, if all references to
     * the previous module are not cleared, the same module may be loaded more than once, causing possible class duplication
     * and class cast exceptions if proper care is not taken.
     *
     * @param module the module to unload
     * @throws SecurityException if an attempt is made to unload a module which does not belong to this module loader
     * @throws SecurityException if the module was not defined by this module loader
     * @deprecated use {@link #unloadModuleLocal(String,Module)} instead
     */
    @Deprecated
    protected final void unloadModuleLocal(Module module) throws SecurityException {
        if (module == null) {
            throw new NullPointerException("Module parameter cannot be null");
        }
        unloadModuleLocal(module.getName(), module);
    }

    /**
     * Unload a module from this module loader.  Note that this has no effect on existing modules which refer to the
     * module being unloaded.  Also, only modules from the current module loader can be unloaded.  Unloading the same
     * module more than once has no additional effect.  This method only removes the mapping for the module; any running
     * threads which are currently accessing or linked to the module will continue to function, however attempts to load
     * this module will fail until a new module is loaded with the same name.  Once this happens, if all references to
     * the previous module are not cleared, the same module may be loaded more than once, causing possible class duplication
     * and class cast exceptions if proper care is not taken.
     *
     * If the given {@code moduleName} is an alias of the given {@code module}
     * then only alias mapping to {@code module} is removed. Aliased {@code module}
     * with its canonical name and other aliases mapping is left intact.
     *
     * If the given {@code moduleName} is the canonical {@code module} name
     * then all its alias plus canonical {@code moduleName} association mappings are removed.
     *
     * @param moduleId the module identifier
     * @param module the module to unload
     * @return true iff module was unloaded
     * @throws SecurityException if an attempt is made to unload a module which does not belong to this module loader
     * @throws SecurityException if the module was not defined by this module loader
     */
    protected final boolean unloadModuleLocal(final String moduleId, final Module module) throws SecurityException {
        if (moduleId == null || module == null) {
            throw new NullPointerException("Method parameters cannot be null");
        }
        final ModuleLoader moduleLoader = module.getModuleLoader();
        if (moduleLoader != this) {
            throw new SecurityException("Attempted to unload " + module + " from a different module loader");
        }
        final FutureModule futureModule = moduleMap.get(moduleId);
        if (futureModule != null && futureModule.module == module) {
            final Set<String> aliases = module.aliases;
            if (aliases != null) {
                synchronized (aliases) {
                    if (aliases.remove(moduleId)) {
                        // unregistered alias from aliased module
                    } else {
                        // cleaning aliased module and all its aliases
                        module.aliases = null;
                        for (String alias : aliases) {
                            moduleMap.remove(alias);
                        }
                    }
                }
            }
            return moduleMap.remove(moduleId, futureModule);
        }
        return false;
    }

    /**
     * This compatibility method just calls {@code findModule(methodIdentifier.toString())} and is usually not used;
     * if however a legacy subclass overrides this method, then it will be called instead of {@link #findModule(String)},
     * and that method will redirect to this one.
     *
     * @deprecated Use {@link #findModule(String)} instead.
     *
     * @param moduleIdentifier the module identifier
     * @return the module specification
     * @throws ModuleLoadException
     */
    @Deprecated
    protected ModuleSpec findModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        if (overridesFindModuleByIdentifier) {
            return findModule0(moduleIdentifier.toString());
        } else {
            return findModule(moduleIdentifier.toString());
        }
    }

    /**
     * Find a Module's specification in this ModuleLoader by its name.  This can be overriden by sub-classes to
     * implement the Module loading strategy for this loader.  The default implementation iterates the module finders
     * provided during construction.
     * <p/>
     * If no module is found in this module loader with the given identifier, then this method should return {@code
     * null}. If the module is found but some problem occurred (for example, a transitive dependency failed to load)
     * then this method should throw a {@link ModuleLoadException} of the relevant type.
     *
     * @param name the module name
     * @return the module specification, or {@code null} if no module is found with the given identifier
     * @throws ModuleLoadException if any problems occur finding the module
     */
    protected ModuleSpec findModule(final String name) throws ModuleLoadException {
        if (overridesFindModuleByIdentifier) {
            return findModule(ModuleIdentifier.fromString(name));
        } else {
            return findModule0(name);
        }
    }

    private ModuleSpec findModule0(final String name) throws ModuleLoadException {
        for (ModuleFinder finder : finders) {
            if (finder != null) {
                final ModuleSpec spec = finder.findModule(name, this);
                if (spec != null) {
                    return spec;
                }
            }
        }
        return null;
    }

    /**
     * Get the module finders configured for this module loader.
     *
     * @return the module finders
     */
    protected final ModuleFinder[] getFinders() {
        return finders.length > 0 ? finders.clone() : NO_FINDERS;
    }

    /**
     * Defines a Module based on a specification.  May only be called from {@link #loadModuleLocal(String)}.
     *
     * @param moduleSpec The module specification to create the Module from
     * @param futureModule the future module to populate
     * @return The defined Module
     * @throws ModuleLoadException If any dependent modules can not be loaded
     */
    private Module defineModule(final ConcreteModuleSpec moduleSpec, final FutureModule futureModule) throws ModuleLoadException {
        try {
            return doPrivileged(new PrivilegedExceptionAction<Module>() {
                public Module run() throws Exception {
                    final ModuleLogger log = Module.log;
                    final String name = moduleSpec.getName();

                    final Module module = new Module(moduleSpec, ModuleLoader.this);
                    module.getClassLoaderPrivate().recalculate();
                    module.setDependencies(moduleSpec.getDependenciesInternal());
                    log.moduleDefined(name, ModuleLoader.this);
                    try {
                        futureModule.setModule(module);
                        return module;
                    } catch (RuntimeException e) {
                        log.trace(e, "Failed to load module %s", name);
                        throw e;
                    } catch (Error e) {
                        log.trace(e, "Failed to load module %s", name);
                        throw e;
                    }
                }
            });
        } catch (PrivilegedActionException pe) {
            try {
                throw pe.getException();
            } catch (RuntimeException e) {
                throw e;
            } catch (ModuleLoadException e) {
                throw e;
            } catch (Exception e) {
                throw new UndeclaredThrowableException(e);
            }
        }
    }

    /**
     * Refreshes the paths provided by resource loaders associated with the
     * specified Module. This is an advanced method that is intended to be
     * called on modules that have a resource loader implementation that has
     * changed and is returning different paths.
     *
     * @param module the module to refresh
     * @throws SecurityException if the module was not defined by this module loader, or if the module loader does not
     *      have the required permissions associated with it
     */
    protected void refreshResourceLoaders(Module module) {
        if (! canRedefine) {
            throw new SecurityException("Module redefinition requires canRedefineModule permission");
        }
        if (module.getModuleLoader() != this) {
            throw new SecurityException("Module is not defined by this module loader");
        }

        module.getClassLoaderPrivate().recalculate();
    }

    /**
     * Replaces the resources loaders for the specified module and refreshes the
     * internal path list that is derived from the loaders. This is an advanced
     * method that should be used carefully, since it alters a live module.
     * Modules that import resources from the specified module will not
     * automatically be updated to reflect the change. For this to occur
     * {@link #relink(Module)} must be called on all of them.
     *
     * @param module the module to update and refresh
     * @param loaders the new collection of loaders the module should use
     * @throws SecurityException if the module was not defined by this module loader, or if the module loader does not
     *      have the required permissions associated with it
     */
    protected void setAndRefreshResourceLoaders(Module module, Collection<ResourceLoaderSpec> loaders) {
        if (! canRedefine) {
            throw new SecurityException("Module redefinition requires canRedefineModule permission");
        }
        if (module.getModuleLoader() != this) {
            throw new SecurityException("Module is not defined by this module loader");
        }

        module.getClassLoaderPrivate().setResourceLoaders(loaders.toArray(new ResourceLoaderSpec[loaders.size()]));
    }

    /**
     * Relinks the dependencies associated with the specified Module. This is an
     * advanced method that is intended to be called on all modules that
     * directly or indirectly import dependencies that are re-exported by a module
     * that has recently been updated and relinked via
     * {@link #setAndRelinkDependencies(Module, java.util.List)}.
     *
     * @param module the module to relink
     * @throws ModuleLoadException if relinking failed
     * @throws SecurityException if the module was not defined by this module loader, or if the module loader does not
     *      have the required permissions associated with it
     */
    protected void relink(Module module) throws ModuleLoadException {
        if (! canRedefine) {
            throw new SecurityException("Module redefinition requires canRedefineModule permission");
        }
        if (module.getModuleLoader() != this) {
            throw new SecurityException("Module is not defined by this module loader");
        }

        module.relink();
    }

    /**
     * Replaces the dependencies for the specified module and relinks against
     * the new modules This is an advanced method that should be used carefully,
     * since it alters a live module. Modules that import dependencies that are
     * re-exported from the specified module will not automatically be updated
     * to reflect the change. For this to occur {@link #relink(Module)} must be
     * called on all of them.
     *
     * @param module the module to update and relink
     * @param dependencies the new dependency list
     * @throws ModuleLoadException if relinking failed
     * @throws SecurityException if the module was not defined by this module loader, or if the module loader does not
     *      have the required permissions associated with it
     */
    protected void setAndRelinkDependencies(Module module, List<DependencySpec> dependencies) throws ModuleLoadException {
        if (! canRedefine) {
            throw new SecurityException("Module redefinition requires canRedefineModule permission");
        }
        if (module.getModuleLoader() != this) {
            throw new SecurityException("Module is not defined by this module loader");
        }

        module.setDependencies(dependencies);
        module.relinkIfNecessary();
    }

    /**
     * Get the current dependency list for a module which was defined by this module loader, without any access checks.
     *
     * @return the current dependency list for the module
     * @throws SecurityException if the module was not defined by this module loader
     */
    protected DependencySpec[] getDependencies(Module module) {
        if (module.getModuleLoader() != this) {
            throw new SecurityException("Module is not defined by this module loader");
        }
        return module.getDependencySpecsInternal().clone();
    }

    void addLinkTime(long amount) {
        if (amount != 0L) linkTime.addAndGet(amount);
    }

    void addClassLoadTime(final long time) {
        if (time != 0L) classLoadTime.addAndGet(time);
    }

    void incScanCount() {
        if (Metrics.ENABLED) scanCount.getAndIncrement();
    }

    void incRaceCount() {
        if (Metrics.ENABLED) raceCount.getAndIncrement();
    }

    void incClassCount() {
        if (Metrics.ENABLED) classCount.getAndIncrement();
    }

    private static final class FutureModule {
        private static final Object NOT_FOUND = new Object();

        final String name;
        volatile Object module;

        FutureModule(final String name) {
            this.name = name;
        }

        Module getModule() throws ModuleNotFoundException {
            boolean intr = false;
            try {
                Object module = this.module;
                if (module == null) synchronized (this) {
                    while ((module = this.module) == null) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            intr = true;
                        }
                    }
                }
                if (module == NOT_FOUND) throw new ModuleNotFoundException(name);
                return (Module) module;
            } finally {
                if (intr) Thread.currentThread().interrupt();
            }
        }

        void setModule(Module m) {
            synchronized (this) {
                module = m == null ? NOT_FOUND : m;
                notifyAll();
            }
        }
    }

    private static final Reaper<ModuleLoader, ObjectName> reaper = new Reaper<ModuleLoader, ObjectName>() {
        public void reap(final Reference<ModuleLoader, ObjectName> reference) {
            REG_REF.removeMBean(reference.getAttachment());
        }
    };

    static final class MXBeanImpl implements ModuleLoaderMXBean {
        private final Reference<ModuleLoader, ObjectName> reference;

        MXBeanImpl(final ModuleLoader moduleLoader, final ObjectName objectName) {
            reference = new WeakReference<ModuleLoader, ObjectName>(moduleLoader, objectName, reaper);
        }

        public String getDescription() {
            return getModuleLoader().toString();
        }

        public long getLinkTime() {
            return getModuleLoader().linkTime.get();
        }

        public long getLoadTime() {
            return getModuleLoader().loadTime.get();
        }

        public long getClassDefineTime() {
            return getModuleLoader().classLoadTime.get();
        }

        public int getScanCount() {
            return getModuleLoader().scanCount.get();
        }

        public int getLoadedModuleCount() {
            return getModuleLoader().moduleMap.size();
        }

        public int getRaceCount() {
            return getModuleLoader().raceCount.get();
        }

        public int getClassCount() {
            return getModuleLoader().classCount.get();
        }

        public List<String> queryLoadedModuleNames() {
            final ModuleLoader loader = getModuleLoader();
            final Set<String> names = loader.moduleMap.keySet();
            final ArrayList<String> list = new ArrayList<>(names);
            list.sort(Comparator.naturalOrder());
            return list;
        }

        public String dumpAllModuleInformation() {
            final StringBuilder b = new StringBuilder();
            for (String name : queryLoadedModuleNames()) {
                doDumpModuleInformation(name, b);
            }
            return b.toString();
        }

        public String dumpModuleInformation(final String name) {
            final StringBuilder b = new StringBuilder();
            doDumpModuleInformation(name, b);
            return b.toString();
        }

        private void doDumpModuleInformation(final String name, final StringBuilder b) {
            ModuleInfo description = getModuleDescription(name);
            b.append("Module ").append(name).append('\n');
            b.append("    Class loader: ").append(description.getClassLoader()).append('\n');
            String fallbackLoader = description.getFallbackLoader();
            if (fallbackLoader != null) b.append("    Fallback loader: ").append(fallbackLoader).append('\n');
            String mainClass = description.getMainClass();
            if (mainClass != null) b.append("    Main Class: ").append(mainClass).append('\n');
            List<ResourceLoaderInfo> loaders = description.getResourceLoaders();
            b.append("    Resource Loaders:\n");
            for (ResourceLoaderInfo loader : loaders) {
                b.append("        Loader Type: ").append(loader.getType()).append('\n');
                b.append("        Paths:\n");
                for (String path : loader.getPaths()) {
                    b.append("            ").append(path).append('\n');
                }
            }
            b.append("    Dependencies:\n");
            for (DependencyInfo dependencyInfo : description.getDependencies()) {
                b.append("        Type: ").append(dependencyInfo.getDependencyType()).append('\n');
                String moduleName = dependencyInfo.getModuleName();
                if (moduleName != null) {
                    b.append("        Module Name: ").append(moduleName).append('\n');
                }
                if (dependencyInfo.isOptional()) b.append("        (optional)\n");
                b.append("        Export Filter: ").append(dependencyInfo.getExportFilter()).append('\n');
                b.append("        Import Filter: ").append(dependencyInfo.getImportFilter()).append('\n');
                String localLoader = dependencyInfo.getLocalLoader();
                if (localLoader != null) {
                    b.append("        Local Loader: ").append(localLoader).append('\n');
                    b.append("        Paths:\n");
                    for (String path : dependencyInfo.getLocalLoaderPaths()) {
                        b.append("            ").append(path).append('\n');
                    }
                }
            }
        }

        public boolean unloadModule(final String name) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(MODULE_UNLOAD_ANY_PERM);
            }
            final ModuleLoader loader = getModuleLoader();
            final Module module = loader.findLoadedModuleLocal(name);
            if (module == null) {
                return false;
            } else {
                loader.unloadModuleLocal(name, module);
                return true;
            }
        }

        public void refreshResourceLoaders(final String name) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(MODULE_REDEFINE_ANY_PERM);
            }
            final ModuleLoader loader = getModuleLoader();
            final Module module = loadModule(name, loader);
            loader.refreshResourceLoaders(module);
        }

        public void relink(final String name) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(MODULE_REDEFINE_ANY_PERM);
            }
            final ModuleLoader loader = getModuleLoader();
            final Module module = loadModule(name, loader);
            try {
                loader.relink(module);
            } catch (ModuleLoadException e) {
                throw new IllegalStateException("Module load failure for module " + name + ": " + e.toString());
            }
        }

        public List<DependencyInfo> getDependencies(final String name) {
            final ModuleLoader loader = getModuleLoader();
            final Module module = loadModule(name, loader);
            return doGetDependencies(module);
        }

        private List<DependencyInfo> doGetDependencies(final Module module) {
            Dependency[] dependencies = module.getDependenciesInternal();
            if (dependencies == null) {
                return Collections.emptyList();
            }
            ArrayList<DependencyInfo> list = new ArrayList<DependencyInfo>(dependencies.length);
            for (Dependency dependency : dependencies) {
                final String dependencyType = dependency.getClass().getSimpleName();
                final String exportFilter = dependency.getExportFilter().toString();
                final String importFilter = dependency.getImportFilter().toString();
                final DependencyInfo info;
                if (dependency instanceof LocalDependency) {
                    final LocalDependency localDependency = (LocalDependency) dependency;
                    ArrayList<String> pathList = new ArrayList<String>(localDependency.getPaths());
                    Collections.sort(pathList);
                    info = new DependencyInfo(dependencyType, exportFilter, importFilter, null, null, false, localDependency.getLocalLoader().toString(), pathList);
                } else if (dependency instanceof ModuleDependency) {
                    final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                    info = new DependencyInfo(dependencyType, exportFilter, importFilter, moduleDependency.getModuleLoader().mxBean, moduleDependency.getName(), moduleDependency.isOptional(), null, null);
                } else {
                    info = new DependencyInfo(dependencyType, exportFilter, importFilter, null, null, false, null, null);
                }
                list.add(info);
            }
            return list;
        }

        public List<ResourceLoaderInfo> getResourceLoaders(final String name) {
            ModuleLoader loader = getModuleLoader();
            final Module module = loadModule(name, loader);
            return doGetResourceLoaders(module);
        }

        private List<ResourceLoaderInfo> doGetResourceLoaders(final Module module) {
            final ModuleClassLoader classLoader = module.getClassLoaderPrivate();
            final ResourceLoader[] loaders = classLoader.getResourceLoaders();
            final ArrayList<ResourceLoaderInfo> list = new ArrayList<ResourceLoaderInfo>(loaders.length);
            for (ResourceLoader resourceLoader : loaders) {
                list.add(new ResourceLoaderInfo(resourceLoader.getClass().getName(), String.valueOf(resourceLoader.getLocation()), new ArrayList<>(resourceLoader.getPaths())));
            }
            return list;
        }

        public ModuleInfo getModuleDescription(final String name) {
            ModuleLoader loader = getModuleLoader();
            final Module module = loadModule(name, loader);
            final List<DependencyInfo> dependencies = doGetDependencies(module);
            final List<ResourceLoaderInfo> resourceLoaders = doGetResourceLoaders(module);
            final LocalLoader fallbackLoader = module.getFallbackLoader();
            final String fallbackLoaderString = fallbackLoader == null ? null : fallbackLoader.toString();
            return new ModuleInfo(module.getIdentifier().toString(), module.getModuleLoader().mxBean, dependencies, resourceLoaders, module.getMainClass(), module.getClassLoaderPrivate().toString(), fallbackLoaderString);
        }

        public SortedMap<String, List<String>> getModulePathsInfo(final String name, final boolean exports) {
            ModuleLoader loader = getModuleLoader();
            final Module module = loadModule(name, loader);
            final Map<String, List<LocalLoader>> paths;
            try {
                paths = module.getPathsUnchecked();
            } catch (ModuleLoadError e) {
                throw new IllegalArgumentException("Error loading module " + name + ": " + e.toString());
            }
            final TreeMap<String, List<String>> result = new TreeMap<String, List<String>>();
            for (Map.Entry<String, List<LocalLoader>> entry : paths.entrySet()) {
                final String path = entry.getKey();
                final List<LocalLoader> loaders = entry.getValue();
                if (loaders.isEmpty()) {
                    result.put(path, Collections.<String>emptyList());
                } else if (loaders.size() == 1) {
                    result.put(path, Collections.<String>singletonList(loaders.get(0).toString()));
                } else {
                    final ArrayList<String> list = new ArrayList<String>();
                    for (LocalLoader localLoader : loaders) {
                        list.add(localLoader.toString());
                    }
                    result.put(path, list);
                }
            }
            return result;
        }

        private Module loadModule(final String name, final ModuleLoader loader) {
            try {
                final Module module = loader.findLoadedModuleLocal(name);
                if (module == null) {
                    throw new IllegalArgumentException("Module " + name + " not found");
                }
                return module;
            } catch (ModuleLoadError e) {
                throw new IllegalArgumentException("Error loading module " + name + ": " + e.toString());
            }
        }

        private ModuleLoader getModuleLoader() {
            final ModuleLoader loader = reference.get();
            if (loader == null) {
                throw new IllegalStateException("Module Loader is gone");
            }
            return loader;
        }
    }

    private interface MBeanReg {
        boolean addMBean(ObjectName name, Object bean);

        void removeMBean(ObjectName name);

        void installReal();
    }

    private static final class TempMBeanReg implements MBeanReg {
        private final Map<ObjectName, Object> mappings = new LinkedHashMap<ObjectName, Object>();

        public boolean addMBean(final ObjectName name, final Object bean) {
            if (bean == null) {
                throw new IllegalArgumentException("bean is null");
            }
            synchronized (ModuleLoader.class) {
                if (REG_REF == this) {
                    return mappings.put(name, bean) == null;
                } else {
                    return REG_REF.addMBean(name, bean);
                }
            }
        }

        public void removeMBean(final ObjectName name) {
            synchronized (ModuleLoader.class) {
                if (REG_REF == this) {
                    mappings.remove(name);
                } else {
                    REG_REF.removeMBean(name);
                }
            }
        }

        public void installReal() {
            synchronized (ModuleLoader.class) {
                RealMBeanReg real = new RealMBeanReg();
                if (REG_REF == this) {
                    REG_REF = real;
                    for (Map.Entry<ObjectName, Object> entry : mappings.entrySet()) {
                        real.addMBean(entry.getKey(), entry.getValue());
                    }
                    mappings.clear();
                }
            }
        }
    }

    private static final class RealMBeanReg implements MBeanReg {
        private final MBeanServer server;

        RealMBeanReg() {
            server = doPrivileged(new PrivilegedAction<MBeanServer>() {
                public MBeanServer run() {
                    return ManagementFactory.getPlatformMBeanServer();
                }
            });
        }

        public boolean addMBean(final ObjectName name, final Object bean) {
            try {
                server.registerMBean(bean, name);
                return true;
            } catch (Throwable e) {
            }
            return false;
        }

        public void removeMBean(final ObjectName name) {
            try {
                server.unregisterMBean(name);
            } catch (Throwable e) {
            }
        }

        public void installReal() {
            // ignore
        }
    }
}
