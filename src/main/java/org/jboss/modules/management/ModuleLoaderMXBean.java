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
     * Get the estimated CPU time (in nanoseconds) spent loading modules into this loader.
     *
     * @return the estimated time in nanoseconds
     */
    long getLoadTime();

    /**
     * Get the estimated CPU time (in nanoseconds) spent defining classes for this loader.
     *
     * @return the estimated time in nanoseconds
     */
    long getClassDefineTime();

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
     * Get the number of times a class was defined by two threads at once.
     *
     * @return the race count
     */
    int getRaceCount();

    /**
     * Get the number of classes defined in this module loader.
     *
     * @return the number of classes defined in this module loader
     */
    int getClassCount();

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
