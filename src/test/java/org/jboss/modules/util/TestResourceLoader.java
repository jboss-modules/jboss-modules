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

package org.jboss.modules.util;

import org.jboss.modules.AbstractResourceLoader;
import org.jboss.modules.ClassSpec;
import org.jboss.modules.PackageSpec;
import org.jboss.modules.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.Assert.assertTrue;
import static org.jboss.modules.util.Util.getClassBytes;

/**
 * A test resource loader that simple retrieves resources frm maps.  This allows tests to build
 * arbitrary modules with arbitrary content without having to have the module on disk.
 *
 * @author John E. Bailey
 */
public class TestResourceLoader extends AbstractResourceLoader {
    private final Map<String, ClassSpec> classSpecs = new HashMap<String, ClassSpec>();
    private final Map<String, Resource> resources = new HashMap<String, Resource>();
    private final Set<String> paths = new HashSet<String>();
    private Manifest manifest;

    public String getRootName() {
        return "test";
    }

    @Override
    public ClassSpec getClassSpec(final String fileName) throws IOException {
        final Map<String, ClassSpec> classSpecs = this.classSpecs;
        return classSpecs.get(fileName);
    }

    void addClassSpec(final String name, final ClassSpec classSpec) {
        final Map<String, ClassSpec> classSpecs = this.classSpecs;
        classSpecs.put(name.replace('.', '/') + ".class", classSpec);
        addPaths(getPathFromClassName(name));
    }

    @Override
    public PackageSpec getPackageSpec(final String name) throws IOException {
        return getPackageSpec(name, getManifest(), null);
    }

    private Manifest getManifest() throws IOException {
        if(manifest != null)
            return manifest;

        final Resource manifestResource = getResource("META-INF/MANIFEST.MF");
        if(manifestResource  == null)
            return null;
        final InputStream manifestStream = manifestResource.openStream();
        try {
            manifest = new Manifest(manifestStream);
        } finally {
            if(manifestStream != null) manifestStream.close();
        }
        return manifest;
    }

    private static String getDefinedAttribute(Attributes.Name name, Attributes entryAttribute, Attributes mainAttribute) {
        final String value = entryAttribute == null ? null : entryAttribute.getValue(name);
        return value == null ? mainAttribute == null ? null : mainAttribute.getValue(name) : value;
    }

    @Override
    public Resource getResource(final String name) {
        String resourceName = name;
        if (resourceName.startsWith("/"))
            resourceName = resourceName.substring(1);
        final Map<String, Resource> resources = this.resources;
        return resources.get(resourceName);
    }

    void addResource(final String name, final Resource resource) {
        final Map<String, Resource> resources = this.resources;
        resources.put(name, resource);
        addPaths(getPathFromResourceName(name));
    }

    private void addPaths(String path) {
        final String[] parts = path.split("/");
        String current = "";
        for(String part : parts) {
            current += part;
            paths.add(current);
            current += "/";
        }
    }

    @Override
    public String getLibrary(String name) {
        return null;
    }

    @Override
    public Collection<String> getPaths() {
        return paths;
    }

    private String getPathFromResourceName(final String resourcePath) {
        int idx = resourcePath.lastIndexOf('/');
        final String path = idx > -1 ? resourcePath.substring(0, idx) : "";
        return path;
    }

    private String getPathFromClassName(final String className) {
        int idx = className.lastIndexOf('.');
        return idx > -1 ? className.substring(0, idx).replace('.', '/') : "";
    }

    public static TestResourceLoaderBuilder build() {
        return new TestResourceLoaderBuilder();
    }

    public static class TestResourceLoaderBuilder {
        private final TestResourceLoader resourceLoader = new TestResourceLoader();

        public TestResourceLoader create() {
            return resourceLoader;
        }

        public TestResourceLoaderBuilder addResource(final String name, final URL resourceUrl) {
            addResource(name, new Resource() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public URL getURL() {
                    return resourceUrl;
                }

                @Override
                public InputStream openStream() throws IOException {
                    return resourceUrl.openStream();
                }

                @Override
                public long getSize() {
                    return 0L;
                }
            });
            return this;
        }

        public TestResourceLoaderBuilder addResource(final String name, final File resource) throws MalformedURLException {
            final URL url = resource.toURI().toURL();
            addResource(name, new Resource() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public URL getURL() {
                    return url;
                }

                @Override
                public InputStream openStream() throws IOException {
                    return new FileInputStream(resource);
                }

                @Override
                public long getSize() {
                    return resource.length();
                }
            });
            return this;
        }

        public TestResourceLoaderBuilder addResource(final String name, final Resource resource) {
            final TestResourceLoader resourceLoader = this.resourceLoader;
            resourceLoader.addResource(name, resource);
            return this;
        }

        public TestResourceLoaderBuilder addResources(final File base) throws Exception {
            addResources("", base);
            return this;
        }

        private void addResources(final String pathBase, final File base) throws Exception {
            assertTrue(base.isDirectory());
            final File[] children = base.listFiles();
            for (File child : children) {
                final String childPath = pathBase + child.getName();
                if (child.isDirectory()) {
                    addResources(childPath + "/", child);
                } else {
                    addResource(childPath, child);
                }
            }
        }

        public TestResourceLoaderBuilder addClass(final Class<?> aClass) throws Exception {
            final ClassSpec classSpec = new ClassSpec();
            classSpec.setCodeSource(aClass.getProtectionDomain().getCodeSource());
            final byte[] classBytes = getClassBytes(aClass);
            classSpec.setBytes(classBytes);
            addClassSpec(aClass.getName(), classSpec);
            return this;
        }

        public TestResourceLoaderBuilder addClasses(final Class<?>... classes) throws Exception {
            for(Class<?> aClass : classes) {
                addClass(aClass);
            }
            return this;
        }

        public TestResourceLoaderBuilder addClassSpec(final String name, final ClassSpec classSpec) {
            final TestResourceLoader resourceLoader = this.resourceLoader;
            resourceLoader.addClassSpec(name, classSpec);
            return this;
        }
    }
}
