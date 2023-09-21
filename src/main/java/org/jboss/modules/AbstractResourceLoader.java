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
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public abstract class AbstractResourceLoader implements ResourceLoader {

    /**
     * Construct a new instance.
     */
    protected AbstractResourceLoader() {
    }

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

    public String getRootName() {
        return "";
    }

    public ClassSpec getClassSpec(final String fileName) throws IOException {
        return null;
    }

    public PackageSpec getPackageSpec(final String name) throws IOException {
        return null;
    }

    public Resource getResource(final String name) {
        return null;
    }

    public String getLibrary(final String name) {
        return null;
    }

    public Collection<String> getPaths() {
        return Collections.emptySet();
    }

}
