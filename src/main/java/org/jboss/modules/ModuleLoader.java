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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.concurrent.ConcurrentMap;

import static org.jboss.modules.ConcurrentReferenceHashMap.ReferenceType.STRONG;
import static org.jboss.modules.ConcurrentReferenceHashMap.ReferenceType.WEAK;

/**
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public abstract class ModuleLoader {

    private final ConcurrentMap<ModuleIdentifier, FutureModule> moduleMap = new ConcurrentReferenceHashMap<ModuleIdentifier, FutureModule>(
            256, 0.5f, 32, STRONG, WEAK, EnumSet.noneOf(ConcurrentReferenceHashMap.Option.class)
    );

    protected ModuleLoader() {
        // todo permission check: subclass permission?
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
        final Module module = preloadModule(identifier);
        module.linkInitial(new HashSet<Module>());
        return module;
    }

    /**
     * Preload a module based on an identifier.  By default, no delegation is done and this method simply invokes
     * {@link #loadModuleLocal(ModuleIdentifier)}.  A delegating module loader may delegate to the appropriate module
     * loader based on loader-specific criteria (via the {@link #preloadModule(ModuleIdentifier, ModuleLoader)} method).
     *
     * @param identifier the module identifier
     * @return the load result
     * @throws ModuleLoadException if an error occurs
     */
    protected Module preloadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        Module module = loadModuleLocal(identifier);
        if (module == null) {
            throw new ModuleNotFoundException(identifier.toString());
        }
        return module;
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
        if (identifier.equals(ModuleIdentifier.SYSTEM)) {
            return Module.getSystemModule();
        }

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
            final Module module = defineModule(moduleSpec);
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
     * @return The defined Module
     * @throws ModuleLoadException If any dependent modules can not be loaded
     */
    private Module defineModule(ModuleSpec moduleSpec) throws ModuleLoadException {

        final ModuleLogger log = Module.log;
        final ModuleIdentifier moduleIdentifier = moduleSpec.getModuleIdentifier();

        FutureModule futureModule = moduleMap.get(moduleIdentifier);
        if (futureModule == null) {
            // should never happen
            throw new IllegalStateException("Attempted to define a module from outside loadModuleLocal");
        }
        final Module module = new Module(moduleSpec, this, futureModule);
        module.getClassLoaderPrivate().recalculate();
        module.setDependencies(moduleSpec.getDependencies());
        try {
            futureModule.setModule(module);
            return module;
        } catch (ModuleLoadException e) {
            log.trace(e, "Failed to load module %s", moduleIdentifier);
            throw e;
        } catch (RuntimeException e) {
            log.trace(e, "Failed to load module %s", moduleIdentifier);
            throw e;
        } catch (Error e) {
            log.trace(e, "Failed to load module %s", moduleIdentifier);
            throw e;
        }
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

        void setModule(Module m) throws ModuleAlreadyExistsException {
            synchronized (this) {
                module = m == null ? NOT_FOUND : m;
                notifyAll();
            }
        }
    }
}
