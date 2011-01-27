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

import static org.jboss.modules.ConcurrentReferenceHashMap.ReferenceType.STRONG;
import static org.jboss.modules.ConcurrentReferenceHashMap.ReferenceType.WEAK;
import static org.jboss.modules.management.ObjectProperties.property;

import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
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
 * @author Jason T. Greene
 *
 * @apiviz.landmark
 */
public abstract class ModuleLoader {

    private static final RuntimePermission ML_PERM = new RuntimePermission("canCreateModuleLoader");
    private static final RuntimePermission MODULE_REDEFINE_PERM = new RuntimePermission("canRedefineModule");
    private static final RuntimePermission MODULE_REDEFINE_ANY_PERM = new RuntimePermission("canRedefineAnyModule");
    private static final RuntimePermission MODULE_UNLOAD_ANY_PERM = new RuntimePermission("canUnloadAnyModule");

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    private static volatile MBeanReg REG_REF = new TempMBeanReg();

    private final ConcurrentMap<ModuleIdentifier, FutureModule> moduleMap = new ConcurrentReferenceHashMap<ModuleIdentifier, FutureModule>(
            256, 0.5f, 32, STRONG, WEAK, EnumSet.noneOf(ConcurrentReferenceHashMap.Option.class)
    );

    private final boolean canRedefine;
    private final ModuleLoaderMXBean mxBean;

    // Bypass security check for classes in this package
    ModuleLoader(boolean canRedefine, boolean skipRegister) {
        this.canRedefine = canRedefine;
        mxBean = skipRegister ? null : AccessController.doPrivileged(new PrivilegedAction<ModuleLoaderMXBean>() {
            public ModuleLoaderMXBean run() {
                ObjectName objectName;
                try {
                    Hashtable<String, String> table = new Hashtable<String, String>();
                    table.put("type", "ModuleLoader");
                    table.put("name", ModuleLoader.this.getClass().getSimpleName() + "-" + Integer.toString(SEQ.incrementAndGet()));
                    objectName = new ObjectName("jboss.modules", ObjectProperties.properties(
                            property("type", "ModuleLoader"),
                            property("name", ModuleLoader.this.getClass().getSimpleName() + "-" + Integer.toString(SEQ.incrementAndGet()))
                    ));
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
    }

    /**
     * Construct a new instance.
     */
    protected ModuleLoader() {
        this(checkPermissions(), false);
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
     * Get the string representation of this module loader.
     *
     * @return the string representation
     */
    public abstract String toString();

    static void installMBeanServer() {
        REG_REF.installReal();
    }

    /**
     * Load a module based on an identifier.  This method delegates to {@link #preloadModule(ModuleIdentifier)} and then
     * links the returned module if necessary.
     *
     * @param identifier The module identifier
     * @return The loaded Module
     * @throws ModuleLoadException if the Module can not be loaded
     */
    public final Module loadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        return loadModule(identifier, new FastCopyHashSet<Module>());
    }

    final Module loadModule(ModuleIdentifier identifier, Set<Module> visited) throws ModuleLoadException {
        final Module module = preloadModule(identifier);
        if (module == null) {
            throw new ModuleNotFoundException(identifier.toString());
        }
        module.linkExportsIfNeeded(visited);
        return module;
    }

    /**
     * Preload a module based on an identifier.  By default, no delegation is done and this method simply invokes
     * {@link #loadModuleLocal(ModuleIdentifier)}.  A delegating module loader may delegate to the appropriate module
     * loader based on loader-specific criteria (via the {@link #preloadModule(ModuleIdentifier, ModuleLoader)} method).
     *
     * @param identifier the module identifier
     * @return the load result, or {@code null} if the module is not found
     * @throws ModuleLoadException if an error occurs
     */
    protected Module preloadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        return loadModuleLocal(identifier);
    }

    /**
     * Utility method to delegate to another module loader, accessible from subclasses.
     *
     * @param identifier the module identifier
     * @param moduleLoader the module loader to delegate to
     * @return the delegation result
     * @throws ModuleLoadException if an error occurs
     */
    protected static Module preloadModule(ModuleIdentifier identifier, ModuleLoader moduleLoader) throws ModuleLoadException {
        return moduleLoader.preloadModule(identifier);
    }

    /**
     * Try to load a module from this module loader.  Returns {@code null} if the module is not found.  The returned
     * module may not yet be resolved.
     *
     * @param identifier the module identifier
     * @return the module
     * @throws ModuleLoadException if an error occurs while loading the module
     */
    protected final Module loadModuleLocal(ModuleIdentifier identifier) throws ModuleLoadException {
        FutureModule futureModule = moduleMap.get(identifier);
        if (futureModule != null) {
            return futureModule.getModule();
        }

        FutureModule newFuture = new FutureModule(identifier);
        futureModule = moduleMap.putIfAbsent(identifier, newFuture);
        if (futureModule != null) {
            return futureModule.getModule();
        }

        boolean ok = false;
        try {
            final ModuleLogger log = Module.log;
            log.trace("Locally loading module %s from %s", identifier, this);
            final ModuleSpec moduleSpec = findModule(identifier);
            if (moduleSpec == null) {
                log.trace("Module %s not found from %s", identifier, this);
                return null;
            }
            if (! moduleSpec.getModuleIdentifier().equals(identifier)) {
                throw new ModuleLoadException("Module loader found a module with the wrong name");
            }
            final Module module = defineModule(moduleSpec, newFuture);
            log.trace("Loaded module %s from %s", identifier, this);
            ok = true;
            return module;
        } finally {
            if (! ok) {
                newFuture.setModule(null);
                moduleMap.remove(identifier, newFuture);
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
     */
    protected final Module findLoadedModuleLocal(ModuleIdentifier identifier) {
        FutureModule futureModule = moduleMap.get(identifier);
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
     */
    protected final void unloadModuleLocal(Module module) throws SecurityException {
        final ModuleLoader moduleLoader = module.getModuleLoader();
        if (moduleLoader != this) {
            throw new SecurityException("Attempted to unload " + module + " from a different module loader");
        }
        final ModuleIdentifier id = module.getIdentifier();
        final FutureModule futureModule = moduleMap.get(id);
        if (futureModule.module == module) {
            moduleMap.remove(id, futureModule);
        }
    }

    /**
     * Find a Module's specification in this ModuleLoader by its identifier.  This should be overriden by sub-classes
     * to implement the Module loading strategy for this loader.
     * <p/>
     * If no module is found in this module loader with the given identifier, then this method should return {@code null}.
     * If the module is found but some problem occurred (for example, a transitive dependency failed to load) then this
     * method should throw a {@link ModuleLoadException} of the relevant type.
     *
     * @param moduleIdentifier The modules Identifier
     * @return the module specification, or {@code null} if no module is found with the given identifier
     * @throws ModuleLoadException if any problems occur finding the module
     */
    protected abstract ModuleSpec findModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException;

    /**
     * Defines a Module based on a specification.  May only be called from {@link #loadModuleLocal(ModuleIdentifier)}.
     *
     * @param moduleSpec The module specification to create the Module from
     * @param futureModule the future module to populate
     * @return The defined Module
     * @throws ModuleLoadException If any dependent modules can not be loaded
     */
    private Module defineModule(ModuleSpec moduleSpec, final FutureModule futureModule) throws ModuleLoadException {

        final ModuleLogger log = Module.log;
        final ModuleIdentifier moduleIdentifier = moduleSpec.getModuleIdentifier();

        final Module module = new Module(moduleSpec, this, futureModule);
        module.getClassLoaderPrivate().recalculate();
        module.initializeDependencies(Arrays.asList(moduleSpec.getDependencies()));
        log.moduleDefined(moduleIdentifier, this);
        try {
            futureModule.setModule(module);
            return module;
        } catch (RuntimeException e) {
            log.trace(e, "Failed to load module %s", moduleIdentifier);
            throw e;
        } catch (Error e) {
            log.trace(e, "Failed to load module %s", moduleIdentifier);
            throw e;
        }
    }

    /**
     * Refreshes the paths provided by resource loaders associated with the
     * specified Module. This is an advanced method that is intended to be
     * called on modules that have a resource loader implementation that has
     * changed and is returning different paths.
     *
     * @param module the module to refresh
     */
    protected void refreshResourceLoaders(Module module) {
        if (!canRedefine)
            throw new SecurityException("Module redefinition requires canRedefineModule permission");

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
     */
    protected void setAndRefreshResourceLoaders(Module module, Collection<ResourceLoaderSpec> loaders) {
        if (!canRedefine)
            throw new SecurityException("Module redefinition requires canRedefineModule permission");

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
     */
    protected void relink(Module module) throws ModuleLoadException {
        if (!canRedefine)
            throw new SecurityException("Module redefinition requires canRedefineModule permission");

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
     */
    protected void setAndRelinkDependencies(Module module, List<DependencySpec> dependencies) throws ModuleLoadException {
        if (!canRedefine)
            throw new SecurityException("Module redefinition requires canRedefineModule permission");

        module.setDependencies(dependencies);
    }

    private static final class FutureModule {
        private static final Object NOT_FOUND = new Object();

        private final ModuleIdentifier identifier;
        private volatile Object module;

        FutureModule(final ModuleIdentifier identifier) {
            this.identifier = identifier;
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
                if (module == NOT_FOUND) throw new ModuleNotFoundException(identifier.toString());
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

    private static final class MXBeanImpl implements ModuleLoaderMXBean {
        private final Reference<ModuleLoader, ObjectName> reference;

        public MXBeanImpl(final ModuleLoader moduleLoader, final ObjectName objectName) {
            reference = new WeakReference<ModuleLoader, ObjectName>(moduleLoader, objectName, reaper);
        }

        public int getLoadedModuleCount() {
            return getModuleLoader().moduleMap.size();
        }

        public List<String> queryLoadedModuleNames() {
            ModuleLoader loader = getModuleLoader();
            final Set<ModuleIdentifier> identifiers = loader.moduleMap.keySet();
            final ArrayList<String> list = new ArrayList<String>(identifiers.size());
            for (ModuleIdentifier identifier : identifiers) {
                list.add(identifier.toString());
            }
            Collections.sort(list);
            return list;
        }

        public boolean unloadModule(final String name) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(MODULE_UNLOAD_ANY_PERM);
            }
            final ModuleLoader loader = getModuleLoader();
            final Module module = loader.findLoadedModuleLocal(ModuleIdentifier.fromString(name));
            if (module == null) {
                return false;
            } else {
                loader.unloadModuleLocal(module);
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
            Dependency[] dependencies = module.getDependencies();
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
                    info = new DependencyInfo(dependencyType, exportFilter, importFilter, moduleDependency.getModuleLoader().mxBean, moduleDependency.getIdentifier().toString(), moduleDependency.isOptional(), null, null);
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
                list.add(new ResourceLoaderInfo(resourceLoader.getClass().getName(), new ArrayList<String>(resourceLoader.getPaths())));
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
                paths = module.getPaths(exports);
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
                final Module module = loader.findLoadedModuleLocal(ModuleIdentifier.fromString(name));
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
            server = AccessController.doPrivileged(new PrivilegedAction<MBeanServer>() {
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
