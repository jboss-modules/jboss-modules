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

package org.jboss.modules;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Module content loader used to load resources from a collection of ResourceLoaders.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ModuleContentLoader {

    private final Map<String, ResourceLoader> resourceLoaders;

    public ModuleContentLoader(Map<String, ResourceLoader> resourceLoaders) {
        this.resourceLoaders = resourceLoaders;
    }

    /**
     * Get a resource loader by a root name
     *
     * @param root The resource loader root
     * @return The ResourceLoader at the provided root or null if not found
     */
    public ResourceLoader getResourceLoader(String root) {
        return resourceLoaders.get(root);
    }

    /**
     * Get a resource by name.  Will return the first resource found within the
     * resource loaders.
     *
     * @param name The resource name
     * @return The resource or null if not found in any resource loader
     */
    public Resource getResource(String name) {
        for (ResourceLoader resourceLoader : resourceLoaders.values()) {
            final Resource resource = resourceLoader.getResource(name);
            if (resource != null)
                return resource;
        }
        return null;
    }

    /**
     * Get a resource from a specific root.
     *
     * @param root The root to get the resource from
     * @param name The name of the resource
     * @return The resource or null if not found
     */
    public Resource getResource(String root, String name) {
        final ResourceLoader resourceLoader = resourceLoaders.get(root);
        if (resourceLoader != null) {
            return resourceLoader.getResource(name);
        }
        return null;
    }

    /**
     * Get all resources for a specific name.  This will check each resource loader
     * to find the resource and will return all resources found.
     *
     * @param name The name of the resource
     * @return The Resource or an empty Iterable if none are found
     */
    public Iterable<Resource> getResources(String name) {
        final List<Resource> resources = new LinkedList<Resource>();
        for (ResourceLoader resourceLoader : resourceLoaders.values()) {
            final Resource resource = resourceLoader.getResource(name);
            if (resource != null)
                resources.add(resource);
        }
        return resources;
    }

    /**
     * Get the class specification by checking all resource loaders and
     * returning the first instance found.
     *
     * @param className The class name to get the specification for
     * @return The ClassSpec or null if not found
     * @throws IOException If any issues occur getting the class spec
     */
    public ClassSpec getClassSpec(String className) throws IOException {
        for (ResourceLoader resourceLoader : resourceLoaders.values()) {
            final ClassSpec classSpec = resourceLoader.getClassSpec(className);
            if (classSpec != null)
                return classSpec;
        }
        return null;
    }

    /**
     * Get the package specification by checking all resource loaders and
     * returning the first instance found.
     *
     * @param packageName The class name to get the specification for
     * @return The ClassSpec or null if not found
     */
    public PackageSpec getPackageSpec(String packageName) throws IOException  {
        for (ResourceLoader resourceLoader : resourceLoaders.values()) {
            final PackageSpec packageSpec = resourceLoader.getPackageSpec(packageName);
            if (packageSpec != null)
                return packageSpec;
        }
        return null;
    }
}
