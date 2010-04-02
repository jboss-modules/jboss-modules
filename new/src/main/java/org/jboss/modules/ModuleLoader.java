package org.jboss.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public abstract class ModuleLoader {

    private static ThreadLocal<Set<ModuleIdentifier>> VISITED = new ThreadLocal<Set<ModuleIdentifier>>() {
        @Override
        protected Set<ModuleIdentifier> initialValue() {
            return new LinkedHashSet<ModuleIdentifier>();
        }
    };

    private final HashMap<ModuleIdentifier, Module> moduleMap = new HashMap<ModuleIdentifier, Module>();

    /**
     * Load a module based on an identifier.
     *
     * @param identifier The module identifier
     * @return The loaded Module
     * @throws ModuleLoadException if the Module can not be loaded
     */
    public Module loadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        final Set<ModuleIdentifier> visited = VISITED.get();

        if(visited.contains(identifier))
            throw new ModuleLoadException("Failed to load " + identifier + "; module cycle discovered: " + visited);

        synchronized (moduleMap) {
            final Module module = moduleMap.get(identifier);
            if (module != null) {
                return module;
            }
        }

        visited.add(identifier);
        try {
            final Module module = findModule(identifier);
            if (module == null) {
                throw new ModuleNotFoundException(identifier.toString());
            }
            return module;
        } finally {
            visited.remove(identifier);
        }
    }

    /**
     * Find a Module by its identifier.  This should be overriden by sub-classes
     * to provide custom Module loading strategies.  Implementations of this method
     * should call {@link #defineModule}
     *
     * @param moduleIdentifier The modules Identifier
     * @return The Module
     * @throws ModuleLoadException If any problems occur finding the module
     */
    protected abstract Module findModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException;

    /**
     * Defines a Module based on a specification.  Use of this method is required by
     * any ModuleLoader implementations in order to fully define a Module. 
     *
     * @param moduleSpec The module specification to create the Module from
     * @return The defined Module
     * @throws ModuleLoadException If any dependent modules can not be loaded
     */
    protected final Module defineModule(ModuleSpec moduleSpec) throws ModuleLoadException {

        final ModuleIdentifier moduleIdentifier = moduleSpec.getIdentifier();

        synchronized (moduleMap) {
            final Module oldModule = moduleMap.get(moduleIdentifier);
            if (oldModule != null) {
                throw new ModuleAlreadyExistsException(moduleIdentifier.toString());
            }
            final List<Dependency> dependencies = new ArrayList<Dependency>(moduleSpec.getDependencies().size());
            for (DependencySpec dependencySpec : moduleSpec.getDependencies()) {
                final Dependency dependency = new Dependency(loadModule(dependencySpec.getModuleIdentifier()), dependencySpec.isExport());
                dependencies.add(dependency);
            }

            final Module module = new Module(moduleSpec, dependencies, moduleSpec.getModuleFlags(), this);
            moduleMap.put(moduleIdentifier, module);
            return module;
        }
    }
}
