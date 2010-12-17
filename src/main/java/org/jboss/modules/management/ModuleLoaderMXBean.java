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

package org.jboss.modules.management;

import java.util.List;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ModuleLoaderMXBean {

    /**
     * Get the number of modules currently loaded.
     *
     * @return the loaded module count
     */
    int getLoadedModuleCount();

    /**
     * Obtain a list of the current module names.
     *
     * @return the module names
     */
    List<String> queryLoadedModuleNames();

    /**
     * Attempt to unload a module from this module loader.
     *
     * @param name the string form of the module identifier to unload
     */
    boolean unloadModule(String name);

    /**
     * Attempt to refresh the resource loaders of the given module.
     *
     * @param name the name of the module to refresh
     */
    void refreshResourceLoaders(String name);

    /**
     * Attempt to relink the given module.
     *
     * @param name the name of the module to relink
     */
    void relink(String name);

    /**
     * Get the dependencies of the named module.
     *
     * @param name the module name
     * @return the module's dependencies
     */
    List<DependencyInfo> getDependencies(String name);

    /**
     * Get the resource loaders of the named module.
     *
     * @param name the module name
     * @return the module's resource loaders
     */
    List<ResourceLoaderInfo> getResourceLoaders(String name);
}
