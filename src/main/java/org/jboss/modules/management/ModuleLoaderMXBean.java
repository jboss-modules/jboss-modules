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
import java.util.SortedMap;

/**
 * An MXBean for getting runtime information about a module loader.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ModuleLoaderMXBean {

    /**
     * Get a description of this module loader.
     *
     * @return a description of this module loader
     */
    String getDescription();

    /**
     * Get the estimated CPU time (in nanoseconds) spent linking in the life of this module loader.
     *
     * @return the estimated time in nanoseconds
     */
    long getLinkTime();

    /**
     * Get the number of times that dependencies of a module from this loader have been scanned.
     *
     * @return the count
     */
    int getScanCount();

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
     * Dump all information for a single module as a string.
     *
     * @param name the module name
     * @return the string of module information
     */
    String dumpModuleInformation(String name);

    /**
     * Dump all information for all modules as a string.
     *
     * @return the string of module information
     */
    String dumpAllModuleInformation();

    /**
     * Attempt to unload a module from this module loader.
     *
     * @param name the string form of the module identifier to unload
     * @return {@code true} if the module was unloaded
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

    /**
     * Get the complete description of this module.
     *
     * @param name the module name
     * @return the module description
     */
    ModuleInfo getModuleDescription(String name);

    /**
     * Get a paths map for a given module.
     *
     * @param name the module name
     * @param exports {@code true} for the exported paths, {@code false} for all paths
     * @return the paths map information
     */
    SortedMap<String, List<String>> getModulePathsInfo(String name, boolean exports);
}
