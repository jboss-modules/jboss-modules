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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.jboss.modules.ConcurrentReferenceHashMap.ReferenceType.STRONG;
import static org.jboss.modules.ConcurrentReferenceHashMap.ReferenceType.WEAK;

/**
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public abstract class ModuleLoader {

    private static volatile ModuleLoadLogger log = new ModuleLoadLogger() {
        public void moduleLoading(final ModuleIdentifier identifier) {
        }

        public void moduleLoaded(final ModuleIdentifier identifier) {
        }

        public void moduleLoadFailed(final ModuleIdentifier identifier, final Throwable cause) {
        }
    };

    /**
     * Set the logger to be used for module load events.
     *
     * @param moduleLogger the logger to use
     */
    public static void setLogger(ModuleLoadLogger moduleLogger) {
        // todo perm check
        log = moduleLogger;
    }

    private ThreadLocal<Map<ModuleIdentifier, Module>> VISITED = new ThreadLocal<Map<ModuleIdentifier, Module>>() {
        @Override
        protected Map<ModuleIdentifier, Module> initialValue() {
            return new HashMap<ModuleIdentifier, Module>();
        }
    };

    private final ConcurrentMap<ModuleIdentifier, FutureModule> moduleMap = new ConcurrentReferenceHashMap<ModuleIdentifier, FutureModule>(
            256, 0.5f, 32, STRONG, WEAK, EnumSet.noneOf(ConcurrentReferenceHashMap.Option.class)
    );

    /**
     * Load a module based on an identifier.  By default, no delegation is done and this method simply invokes
     * {@link #loadModuleLocal(ModuleIdentifier)}.  A delegating module loader may delegate to the appropriate module
     * loader based on loader-specific criteria.
     *
     * @param identifier The module identifier
     * @return The loaded Module
     * @throws ModuleLoadException if the Module can not be loaded
     */
    public Module loadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        return loadModuleLocal(identifier);
    }

    /**
     * Load a module from this module loader.
     *
     * @param identifier the module identifier
     * @return the module
     * @throws ModuleLoadException
     */
    protected final Module loadModuleLocal(ModuleIdentifier identifier) throws ModuleLoadException {
        if (identifier.equals(ModuleIdentifier.SYSTEM)) {
            return Module.SYSTEM;
        }

        FutureModule futureModule = moduleMap.get(identifier);
        if (futureModule == null) {
            FutureModule newFuture = new FutureModule(identifier);
            futureModule = moduleMap.putIfAbsent(identifier, newFuture);
            if (futureModule == null) {
                boolean ok = false;
                try {
                    log.moduleLoading(identifier);
                    final ModuleSpec moduleSpec = findModule(identifier);
                    if (moduleSpec == null) {
                        final ModuleNotFoundException e = new ModuleNotFoundException(identifier.toString());
                        log.moduleLoadFailed(identifier, e);
                        throw e;
                    }
                    ok = true;
                    log.moduleLoaded(identifier);
                    return defineModule(moduleSpec);
                } finally {
                    if (! ok) {
                        newFuture.setModule(null);
                        moduleMap.remove(identifier, newFuture);
                    }
                }
            }
        }
        return futureModule.getModule();
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
     * Defines a Module based on a specification. 
     *
     * @param moduleSpec The module specification to create the Module from
     * @return The defined Module
     * @throws ModuleLoadException If any dependent modules can not be loaded
     */
    private Module defineModule(ModuleSpec moduleSpec) throws ModuleLoadException {

        final ModuleIdentifier moduleIdentifier = moduleSpec.getIdentifier();
        FutureModule futureModule = moduleMap.get(moduleIdentifier);
        if (futureModule == null) {
            FutureModule newFuture = new FutureModule(moduleIdentifier);
            futureModule = moduleMap.putIfAbsent(moduleIdentifier, newFuture);
            if (futureModule == null) futureModule = newFuture;
        }
        // early detect
        if (futureModule.module != null) {
            throw new ModuleAlreadyExistsException(moduleIdentifier.toString());
        }

        final Module module = new Module(moduleSpec, moduleSpec.getModuleFlags(), this);
        final Map<ModuleIdentifier, Module> visited = VISITED.get();
        visited.put(moduleIdentifier, module);
        try {
            final List<Dependency> dependencies = new ArrayList<Dependency>(moduleSpec.getDependencies().length);
            for (DependencySpec dependencySpec : moduleSpec.getDependencies()) {
                final ModuleIdentifier dependencyIdentifier = dependencySpec.getModuleIdentifier();
                Module dependencyModule;
                try {
                    dependencyModule = visited.get(dependencyIdentifier);
                    if(dependencyModule == null)
                        dependencyModule = loadModule(dependencySpec.getModuleIdentifier());
                } catch (ModuleLoadException e) {
                    if (dependencySpec.isOptional()) {
                        continue;
                    } else {
                        throw e;
                    }
                }
                final Dependency dependency = new Dependency(dependencyModule, dependencySpec.isExport(), dependencySpec.getExportFilter());
                dependencies.add(dependency);
            }
            module.setDependencies(dependencies);
            synchronized (futureModule) {
                futureModule.setModule(module);
            }
            return module;
        } catch (ModuleLoadException e) {
            log.moduleLoadFailed(moduleIdentifier, e);
            throw e;
        } catch (RuntimeException e) {
            log.moduleLoadFailed(moduleIdentifier, e);
            throw e;
        } catch (Error e) {
            log.moduleLoadFailed(moduleIdentifier, e);
            throw e;
        } finally {
            visited.remove(moduleIdentifier);
        }
    }

    /**
     * Create an aggregate module based on a module identifier and list of dependencies to import/export.
     *
     * @param moduleIdentifier The module identifier
     * @param dependencies The module identifiers to aggregate
     * @return The loaded Module
     * @throws ModuleLoadException If any dependent module can not be loaded
     */
    public Module createAggregate(ModuleIdentifier moduleIdentifier, List<ModuleIdentifier> dependencies) throws ModuleLoadException {
        final ModuleSpec.Builder moduleSpecBuilder = ModuleSpec.build(moduleIdentifier);
        for(ModuleIdentifier identifier : dependencies) {
            moduleSpecBuilder.addDependency(identifier).setExport(true);
        }
        return defineModule(moduleSpecBuilder.create());
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
