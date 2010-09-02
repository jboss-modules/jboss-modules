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

    /**
     * Construct a new instance.  The given delegates map is copied.
     *
     * @param delegates the default delegates map to use
     * @param defaultLoader the default loader to use if no delegate mapping exists
     */
    public ClassifyingModuleLoader(final Map<String, ModuleLoader> delegates, final ModuleLoader defaultLoader) {
        this.defaultLoader = defaultLoader;
        this.delegates = new HashMap<String, ModuleLoader>(delegates);
    }

    /** {@inheritDoc} */
    protected Module preloadModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        String name = moduleIdentifier.getName();
        int idx;
        final Map<String, ModuleLoader> delegates = this.delegates;
        for (;;) {
            final ModuleLoader loader = delegates.get(name);
            if (loader != null) {
                return loader.preloadModule(moduleIdentifier);
            }
            idx = name.lastIndexOf('.');
            if (idx == -1) {
                return defaultLoader.preloadModule(moduleIdentifier);
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
}
