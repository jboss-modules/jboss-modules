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
