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

package org.jboss.modules.util;

import org.jboss.modules.Module;
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

    private final Map<String, ModuleSpec> moduleSpecs = new HashMap<>();

    protected Module preloadModule(final String name) throws ModuleLoadException {
        return super.preloadModule(name);
    }

    @Override
    protected ModuleSpec findModule(String name) throws ModuleLoadException {
        final ModuleSpec moduleSpec = moduleSpecs.get(name);
        if(moduleSpec == null) throw new ModuleLoadException("No module spec found for module " + name);
        return moduleSpec;
    }

    public void addModuleSpec(final ModuleSpec moduleSpec) {
        moduleSpecs.put(moduleSpec.getName(), moduleSpec);
    }

    public String toString() {
        return "test@" + System.identityHashCode(this);
    }
}
