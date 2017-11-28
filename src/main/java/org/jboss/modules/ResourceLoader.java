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

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

/**
 * A loader for resources from a specific resource root within a module.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ResourceLoader extends AutoCloseable {

    /**
     * Get the name of the root represented by this resource loader.
     *
     * @return the name of the root
     */
    String getRootName();

    /**
     * Get the class specification for the given class name.  If no matching class is found, {@code null} is returned.
     *
     * @param fileName the fileName of the class, e.g. for the class <code>org.jboss.modules.ResourceLoader</code>
     * the fileName will be <code>org/jboss/modules/ResourceLoader.class</code>
     * @return the class specification, or {@code null} if the named class is not found
     * @throws IOException if an I/O error occurs
     */
    ClassSpec getClassSpec(String fileName) throws IOException;

    /**
     * Get the package specification for the given directory name.  Always returns a package specification; this
     * method cannot be used to test for the existence of a package.  A package spec should always be acquired from
     * the same resource loader which provided the class specification.  The directory name will always be specified
     * using "{@code /}" separators.
     *
     * @param name the directory name
     * @return the package specification
     * @throws IOException if an I/O error occurs
     */
    PackageSpec getPackageSpec(String name) throws IOException;

    /**
     * Get a resource with the given name.  If no such resource is available, {@code null} is returned.
     * The resource name will always be specified using "{@code /}" separators for the directory segments.
     *
     * @param name the resource name
     * @return the resource, or {@code null} if it is not available
     */
    Resource getResource(String name);

    /**
     * Get the absolute physical filesystem path for a library with the given name.  The resultant path should be
     * path-separated using "{@code /}" characters.
     *
     * @param name the name
     * @return the path or {@code null} if the library is not present
     */
    String getLibrary(String name);

    /**
     * Get the collection of resource paths.  Called one time only when the resource loader is initialized.  The
     * paths should use "{@code /}" characters to separate the path segments.
     *
     * @return the resource paths
     */
    Collection<String> getPaths();

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     */
    default void close() {}

    /**
     * Get the base location of the resources in this loader (if any).  If the location is not known, or the resource
     * loader does not support locations, or the resource loader does not support this method, {@code null} is returned.
     *
     * @return the base location of the resources in this loader, or {@code null} if not available
     */
    default URI getLocation() {
        return null;
    }

    /**
     * Create a loader which loads resources under a relative subdirectory relative to this loader.  If the resource
     * loader does not support subloaders, {@code null} is returned.
     *
     * @param relativePath the relative path
     * @param rootName the name of the subloader's root
     * @return the resource loader, or {@code null} if subloaders are not supported
     */
    default ResourceLoader createSubloader(String relativePath, String rootName) {
        return null;
    }
}