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
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * An abstract resource loader implementation.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class AbstractResourceLoader implements ResourceLoader {
    private static String getDefinedAttribute(Attributes.Name name, Attributes entryAttribute, Attributes mainAttribute) {
        final String value = entryAttribute == null ? null : entryAttribute.getValue(name);
        return value == null ? mainAttribute == null ? null : mainAttribute.getValue(name) : value;
    }

    /**
     * Convenience method to get a package specification from a {@code Manifest}.
     *
     * @param name the (dot-separated) package name
     * @param manifest the {@code Manifest} instance
     * @param rootUrl the code source URL
     * @return the package specification
     */
    protected static PackageSpec getPackageSpec(final String name, final Manifest manifest, final URL rootUrl) {
        final PackageSpec spec = new PackageSpec();
        if (manifest == null) {
            return null;
        }
        final Attributes mainAttribute = manifest.getMainAttributes();
        final String path = name.replace('.', '/').concat("/");
        final Attributes entryAttribute = manifest.getAttributes(path);
        spec.setSpecTitle(getDefinedAttribute(Attributes.Name.SPECIFICATION_TITLE, entryAttribute, mainAttribute));
        spec.setSpecVersion(getDefinedAttribute(Attributes.Name.SPECIFICATION_VERSION, entryAttribute, mainAttribute));
        spec.setSpecVendor(getDefinedAttribute(Attributes.Name.SPECIFICATION_VENDOR, entryAttribute, mainAttribute));
        spec.setImplTitle(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_TITLE, entryAttribute, mainAttribute));
        spec.setImplVersion(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_VERSION, entryAttribute, mainAttribute));
        spec.setImplVendor(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_VENDOR, entryAttribute, mainAttribute));
        if (Boolean.parseBoolean(getDefinedAttribute(Attributes.Name.SEALED, entryAttribute, mainAttribute))) {
            spec.setSealBase(rootUrl);
        }
        return spec;
    }

    /**
     * Get the name of the root represented by this resource loader.  Returns an empty string by default.
     *
     * @return the name of the root
     */
    public String getRootName() {
        return "";
    }

    /**
     * Get the class specification for the given class name.  If no matching class is found, {@code null} is returned.
     * Returns {@code null} by default.
     *
     * @param fileName the fileName of the class, e.g. for the class <code>org.jboss.modules.ResourceLoader</code> the
     * fileName will be <code>org/jboss/modules/ResourceLoader.class</code>
     *
     * @return the class specification, or {@code null} if the named class is not found
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public ClassSpec getClassSpec(final String fileName) throws IOException {
        return null;
    }

    /**
     * Get the package specification for the given directory name.  Always returns a package specification; this method
     * cannot be used to test for the existence of a package.  A package spec should always be acquired from the same
     * resource loader which provided the class specification.  The directory name will always be specified using "{@code
     * /}" separators.  Returns {@code null} by default.
     *
     * @param name the directory name
     *
     * @return the package specification
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public PackageSpec getPackageSpec(final String name) throws IOException {
        return null;
    }

    /**
     * Get a resource with the given name.  If no such resource is available, {@code null} is returned. The resource name
     * will always be specified using "{@code /}" separators for the directory segments.  Returns {@code null} by default.
     *
     * @param name the resource name
     *
     * @return the resource, or {@code null} if it is not available
     */
    public Resource getResource(final String name) {
        return null;
    }

    /**
     * Get the absolute physical filesystem path for a library with the given name.  The resultant path should be
     * path-separated using "{@code /}" characters.  Returns {@code null} by default.
     *
     * @param name the name
     *
     * @return the path or {@code null} if the library is not present
     */
    public String getLibrary(final String name) {
        return null;
    }

    /**
     * Get the collection of resource paths.  Called one time only when the resource loader is initialized.  The paths
     * should use "{@code /}" characters to separate the path segments.  Returns an empty set by default.
     *
     * @return the resource paths
     */
    public Collection<String> getPaths() {
        return Collections.emptySet();
    }
}
