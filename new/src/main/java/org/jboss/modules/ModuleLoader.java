package org.jboss.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public abstract class ModuleLoader {
    private final HashMap<ModuleIdentifier, Module> moduleMap = new HashMap<ModuleIdentifier, Module>();

    /**
     * Load a module based on an identifier.
     *
     * @param identifier The module identifier
     * @return The loaded Module
     * @throws ModuleNotFoundException if the Module can not be found
     */
    public Module loadModule(ModuleIdentifier identifier) throws ModuleNotFoundException {
        final Module module = findModule(identifier);
        if (module == null) {
            throw new ModuleNotFoundException(identifier.toString());
        }
        return module;
    }

    /**
     * Find a Module by its identifier.  This should be overriden by sub-classes
     * to provide custom Module loading strategies.  Implementations of this method
     * should call {@link #defineModule}
     *
     * @param moduleIdentifier The modules Identifier
     * @return The Module  
     */
    protected abstract Module findModule(final ModuleIdentifier moduleIdentifier);

    /**
     * Defines a Module based on a specification.  Use of this method is required by
     * any ModuleLoader implementations in order to fully define a Module. 
     *
     * @param moduleSpec The module specification to create the Module from
     * @return The defined Module
     * @throws ModuleNotFoundException If any dependent modules can not be found
     */
    protected final Module defineModule(ModuleSpec moduleSpec) throws ModuleNotFoundException {

        final ModuleIdentifier moduleIdentifier = moduleSpec.getIdentifier();

        synchronized (moduleMap) {
            final Module oldModule = moduleMap.get(moduleIdentifier);
            if (oldModule != null) {
                throw new ModuleAlreadyExistsException(moduleIdentifier.toString());
            }
            final List<Module> importModules = new ArrayList<Module>(moduleSpec.getImports().size());
            for (ModuleIdentifier importId : moduleSpec.getImports()) {
                importModules.add(loadModule(importId));
            }
            final List<Module> exportModules = new ArrayList<Module>(moduleSpec.getImports().size());
            for (ModuleIdentifier exportId : moduleSpec.getExports()) {
                exportModules.add(loadModule(exportId));
            }

            final Module module = new Module(moduleSpec, importModules, exportModules);
            moduleMap.put(moduleIdentifier, module);
            return module;
        }
    }
}
