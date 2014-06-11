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

import java.util.HashMap;
import java.util.Map;

/**
 * A module loader which selects a delegate module loader based upon the prefix of the module name.  Longer
 * names are matched first always.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ClassifyingModuleLoader extends ModuleLoader {
    private volatile Map<String, ModuleLoader> delegates;
    private final ModuleLoader defaultLoader;
    private final String name;

    /**
     * Construct a new instance.  The given delegates map is copied.
     *
     * @param delegates the default delegates map to use
     * @param defaultLoader the default loader to use if no delegate mapping exists
     */
    public ClassifyingModuleLoader(final String name, final Map<String, ModuleLoader> delegates, final ModuleLoader defaultLoader) {
        super(true, false);
        this.defaultLoader = defaultLoader;
        this.delegates = new HashMap<String, ModuleLoader>(delegates);
        this.name = name;
    }

    /** {@inheritDoc} */
    protected Module preloadModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        String name = moduleIdentifier.getName();
        int idx;
        final Map<String, ModuleLoader> delegates = this.delegates;
        for (;;) {
            final ModuleLoader loader = delegates.get(name);
            if (loader != null) {
                return preloadModule(moduleIdentifier, loader);
            }
            idx = name.lastIndexOf('.');
            if (idx == -1) {
                return preloadModule(moduleIdentifier, defaultLoader);
            }
            name = name.substring(0, idx);
        }
    }

    /** {@inheritDoc} */
    protected ModuleSpec findModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        // We have no modules of our own!
        return null;
    }

    /**
     * Change the delegates map.  A copy is made of the given map.
     *
     * @param delegates the new delegates map to use
     */
    public void setDelegates(Map<String, ModuleLoader> delegates) {
        this.delegates = new HashMap<String, ModuleLoader>(delegates);
    }

    public String toString() {
        return String.format("Classifying Module Loader @%x \"%s\"", Integer.valueOf(hashCode()), name);
    }
}
