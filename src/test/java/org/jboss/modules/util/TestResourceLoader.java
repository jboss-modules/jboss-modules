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

import static junit.framework.Assert.assertTrue;
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
    private Set<String> paths = new HashSet<String>();
    private Manifest manifest;

    @Override
    public ClassSpec getClassSpec(final String name) throws IOException {
        final Map<String, ClassSpec> classSpecs = this.classSpecs;
        return classSpecs.get(name);
    }

    void addClassSpec(final String name, final ClassSpec classSpec) {
        final Map<String, ClassSpec> classSpecs = this.classSpecs;
        classSpecs.put(name, classSpec);
        addPaths(getPathFromClassName(name));
    }

    @Override
    public PackageSpec getPackageSpec(final String name) throws IOException {
        final PackageSpec spec = new PackageSpec();
        final Manifest manifest = getManifest();
        if (manifest == null) {
            return spec;
        }
        final Attributes mainAttribute = manifest.getMainAttributes();
        final Attributes entryAttribute = manifest.getAttributes(name);
        spec.setSpecTitle(getDefinedAttribute(Attributes.Name.SPECIFICATION_TITLE, entryAttribute, mainAttribute));
        spec.setSpecVersion(getDefinedAttribute(Attributes.Name.SPECIFICATION_VERSION, entryAttribute, mainAttribute));
        spec.setSpecVendor(getDefinedAttribute(Attributes.Name.SPECIFICATION_VENDOR, entryAttribute, mainAttribute));
        spec.setImplTitle(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_TITLE, entryAttribute, mainAttribute));
        spec.setImplVersion(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_VERSION, entryAttribute, mainAttribute));
        spec.setImplVendor(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_VENDOR, entryAttribute, mainAttribute));
        return spec;
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
        final String path = idx > -1 ? resourcePath.substring(0, idx) : resourcePath;
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

        public TestResourceLoaderBuilder addClass(final Class aClass) throws Exception {
            final ClassSpec classSpec = new ClassSpec();
            classSpec.setCodeSource(aClass.getProtectionDomain().getCodeSource());
            final byte[] classBytes = getClassBytes(aClass);
            classSpec.setBytes(classBytes);
            addClassSpec(aClass.getName(), classSpec);
            return this;
        }

        public TestResourceLoaderBuilder addClasses(final Class... classes) throws Exception {
            for(Class aClass : classes) {
                addClass(aClass);
            }
            return this;
        }

        public TestResourceLoaderBuilder addClassSpec(final String name, final ClassSpec classSpec) {
            final TestResourceLoader resourceLoader = this.resourceLoader;
            resourceLoader.addClassSpec(name, classSpec);
            return this;
        }


        public TestResourceLoaderBuilder addExportInclude(final String path) {
            resourceLoader.addExportInclude(path);
            return this;
        }

        public TestResourceLoaderBuilder addExportExclude(final String path) {
            resourceLoader.addExportExclude(path);
            return this;
        }
    }
}
