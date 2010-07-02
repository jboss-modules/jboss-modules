/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.modules.util;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

import java.util.HashMap;
import java.util.Map;

/**
 * Test module loader that allows for modules specs to be added at runtime and it will only load modules from the
 * provided specs.
 * 
 * @author John E. Bailey
 */
public class TestModuleLoader extends ModuleLoader {

    private final Map<ModuleIdentifier, ModuleSpec> moduleSpecs = new HashMap<ModuleIdentifier, ModuleSpec>();
    private final Map<ModuleIdentifier, Module> loadedModules = new HashMap<ModuleIdentifier, Module>();

    @Override
    protected Module findModule(ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        if(loadedModules.containsKey(moduleIdentifier)) {
            return loadedModules.get(moduleIdentifier);
        }
        final ModuleSpec moduleSpec = moduleSpecs.get(moduleIdentifier);
        if(moduleSpec == null) throw new ModuleLoadException("No module spec found for module " + moduleIdentifier);
        final Module module = defineModule(moduleSpec);
        loadedModules.put(moduleIdentifier, module);
        return module;
    }

    public void addModuleSpec(final ModuleSpec moduleSpec) {
        moduleSpecs.put(moduleSpec.getIdentifier(), moduleSpec); 
    }
}
