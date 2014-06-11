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

import java.beans.ConstructorProperties;
import java.util.List;

/**
 * Management information about a module instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleInfo {
    private final String name;
    private final ModuleLoaderMXBean moduleLoader;
    private final List<DependencyInfo> dependencies;
    private final List<ResourceLoaderInfo> resourceLoaders;
    private final String mainClass;
    private final String classLoader;
    private final String fallbackLoader;

    /**
     * Construct a new instance.
     *
     * @param name the module name
     * @param moduleLoader the module loader
     * @param dependencies the dependencies list
     * @param resourceLoaders the resource loaders list
     * @param mainClass the main class name
     * @param classLoader the class loader
     * @param fallbackLoader the fallback loader
     */
    @ConstructorProperties({"name", "moduleLoader", "dependencies", "resourceLoaders", "mainClass", "classLoader", "fallbackLoader"})
    public ModuleInfo(final String name, final ModuleLoaderMXBean moduleLoader, final List<DependencyInfo> dependencies, final List<ResourceLoaderInfo> resourceLoaders, final String mainClass, final String classLoader, final String fallbackLoader) {
        this.name = name;
        this.moduleLoader = moduleLoader;
        this.dependencies = dependencies;
        this.resourceLoaders = resourceLoaders;
        this.mainClass = mainClass;
        this.classLoader = classLoader;
        this.fallbackLoader = fallbackLoader;
    }

    /**
     * Get the name of the corresponding module.
     *
     * @return the name of the corresponding module
     */
    public String getName() {
        return name;
    }

    /**
     * Get the associated module loader MXBean.
     *
     * @return the associated module loader MXBean
     */
    public ModuleLoaderMXBean getModuleLoader() {
        return moduleLoader;
    }

    /**
     * Get the dependency information list.
     *
     * @return the dependency information list
     */
    public List<DependencyInfo> getDependencies() {
        return dependencies;
    }

    /**
     * Get the resource loader information list.
     *
     * @return the resource loader information list
     */
    public List<ResourceLoaderInfo> getResourceLoaders() {
        return resourceLoaders;
    }

    /**
     * Get the main class name.
     *
     * @return the main class name
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Get the class loader (as a string).
     *
     * @return the class loader (as a string)
     */
    public String getClassLoader() {
        return classLoader;
    }

    /**
     * Get the fallback loader (as a string).
     *
     * @return the fallback loader (as a string)
     */
    public String getFallbackLoader() {
        return fallbackLoader;
    }
}
