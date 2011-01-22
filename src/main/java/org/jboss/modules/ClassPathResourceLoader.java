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

package org.jboss.modules;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * <p>
 * A {@link ResourceLoader <code>ResourceLoader</code>} that delegates to a {@link java.lang.ClassLoader
 * <code>ClassLoader</code>} when loading {@link Resource <code>Resources</code>}. This <code>ResourceLoader</code>
 * is targeted at use cases where an application's <tt>module repository</tt> is located on that application's
 * classpath, i.e. where all module resources are visible to one of the application's classloaders.
 * </p>
 * <p>
 * A <code>ClassloaderBackedResourceLoader</code> is initialized with
 * <ul>
 * <li>
 * a <code>ClassLoader</code>,</li>
 * <li>
 * a <code>ModuleIdentifier</code>,</li>
 * <li>
 * a <tt>resource root</tt> and</li>
 * <li>
 * a {@link org.jboss.modules.filter.PathFilter <code>PathFilter</code>}.</li>
 * </ul>
 * When serving a request to load a <code>Resource</code> given its <code>name</code>, instances of this class will
 * <ol>
 * <li>
 * translate the given <code>ModuleIdentifier</code> into a path, i.e. <code>my.test.module</code> will become
 * <code>my/test/module/</code>,</li>
 * <li>
 * append the given <tt>resource root</tt> to the path thus obtained, i.e. for a resource root &quot;res/root/&quot; this will
 * yield <code>my/test/module/res/root/</code>,</li>
 * <li>
 * append the requested <code>Resource</code>'s <code>name</code> to the path thus obtained, i.e. for a <code>Resource</code>
 * named &quot;my/glorious/Resource.res&quot; this step will yield <code>my/test/module/res/root/my/glorious/Resource.res</code>
 * , and</li>
 * <li>
 * ask its <code>ClassLoader</code> for that resource, i.e. call {@link java.lang.ClassLoader#getResourceAsStream(String)
 * <code>classLoader.getResourceAsStream(&quot;my/test/module/res/root/my/glorious/Resource.res&quot;)</code> </li>
 * </ol>
 * </p>
 *
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 */
final class ClassPathResourceLoader implements ResourceLoader {

    private final ClassLoader classLoaderDelegate;

    private final String resourceRootName;

    private final String resourceRootPath;

    private final Manifest manifest;

    private final CodeSource codeSource;

    /**
     * @param classLoaderDelegate
     * @param resourceRootName
     * @param resourceRootPath
     */
    ClassPathResourceLoader(final ClassLoader classLoaderDelegate,
                            final String resourceRootName, final String resourceRootPath) {
        if (classLoaderDelegate == null) {
            throw new IllegalArgumentException("classLoaderDelegate is null");
        }
        if (resourceRootName == null) {
            throw new IllegalArgumentException("resourceRootName is null");
        }
        if (resourceRootPath == null) {
            throw new IllegalArgumentException("resourceRootPath is null");
        }
        this.classLoaderDelegate = classLoaderDelegate;
        this.resourceRootName = resourceRootName;
        this.resourceRootPath = resourceRootPath.endsWith("/") ? resourceRootPath : resourceRootPath + "/";
        this.manifest = readManifest();
        this.codeSource = new CodeSource(getResourceUrl(this.resourceRootPath), (CodeSigner[]) null);
    }

    /**
     * @see ResourceLoader#getRootName()
     */
    @Override
    public String getRootName() {
        return this.resourceRootName;
    }

    /**
     * @see org.jboss.modules.ResourceLoader#getClassSpec(java.lang.String)
     */
    public ClassSpec getClassSpec(String fileName) throws IOException {
        final InputStream classStream = openResourceStream(fileName);
        if (classStream == null) {
            return null;
        }
        final ByteArrayOutputStream classBytesBuf = new ByteArrayOutputStream();
        try {
            int bytesRead = 0;
            final byte[] buf = new byte[16384];
            while ((bytesRead = classStream.read(buf, 0, buf.length)) != -1)
                classBytesBuf.write(buf, 0, bytesRead);
            classBytesBuf.flush();
            final byte[] classBytes = classBytesBuf.toByteArray();
            if (classBytes.length > Integer.MAX_VALUE)
                throw new IOException("Resource [" + fileName + "|fullResourcePath = " + toFullResourcePath(fileName)
                        + "] is too large to be a valid class file");

            final ClassSpec classSpec = new ClassSpec();
            classSpec.setBytes(classBytesBuf.toByteArray());
            classSpec.setCodeSource(this.codeSource);

            return classSpec;
        } finally {
            safeClose(classBytesBuf);
            safeClose(classStream);
        }
    }

    /**
     * @see ResourceLoader#getPackageSpec(String)
     */
    @Override
    public PackageSpec getPackageSpec(final String name) throws IOException {
        final PackageSpec spec = new PackageSpec();
        if (this.manifest == null) {
            return spec;
        }
        final Attributes mainAttribute = this.manifest.getAttributes(name);
        final Attributes entryAttribute = this.manifest.getAttributes(name);
        spec.setSpecTitle(getDefinedAttribute(Attributes.Name.SPECIFICATION_TITLE, entryAttribute, mainAttribute));
        spec.setSpecVersion(getDefinedAttribute(Attributes.Name.SPECIFICATION_VERSION, entryAttribute, mainAttribute));
        spec.setSpecVendor(getDefinedAttribute(Attributes.Name.SPECIFICATION_VENDOR, entryAttribute, mainAttribute));
        spec.setImplTitle(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_TITLE, entryAttribute, mainAttribute));
        spec.setImplVersion(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_VERSION, entryAttribute, mainAttribute));
        spec.setImplVendor(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_VENDOR, entryAttribute, mainAttribute));
        if (Boolean.parseBoolean(getDefinedAttribute(Attributes.Name.SEALED, entryAttribute, mainAttribute))) {
            spec.setSealBase(getResourceUrl(""));
        }
        return spec;
    }

    /**
     * @see org.jboss.modules.ResourceLoader#getResource(java.lang.String)
     */
    public Resource getResource(String name) {
        final URL resourceUrl = getResourceUrl(name);
        if (resourceUrl == null)
            return null;
        return new URLResource(resourceUrl);
    }

    /**
     * @see org.jboss.modules.ResourceLoader#getLibrary(java.lang.String)
     */
    public String getLibrary(final String name) {
        // For now we don't support libraries on the classpath
        return null;
    }

    /**
     * @see org.jboss.modules.ResourceLoader#getPaths()
     */
    public Collection<String> getPaths() {
        // First check for an index file
        final URL indexFile = getResourceUrl("META-INF/INDEX.LIST");
        if (indexFile == null) {
            throw new IllegalStateException("No META-INF/INDEX.LIST found at [" + toFullResourcePath("META-INF/INDEX.LIST")
                    + "]. This file is required for classloader-based resource loaders.");
        }

        BufferedReader r = null;
        try {
            final List<String> index = new ArrayList<String>();
            // Manually build index, starting with the root path
            index.add("");

            r = new BufferedReader(new InputStreamReader(indexFile.openStream()));
            String s;
            while ((s = r.readLine()) != null) {
                index.add(s.trim());
            }

            return index;
        } catch (IOException e) {
            safeClose(r);
            throw new RuntimeException("Failed to read index file [META-INF/INDEX.LIST]: " + e.getMessage(), e);
        }
    }

    protected InputStream openResourceStream(final String resourceName) {
        final String fullResourcePath = toFullResourcePath(resourceName);
        final InputStream resourceStream = this.classLoaderDelegate.getResourceAsStream(fullResourcePath);

        return resourceStream;
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private URL getResourceUrl(final String name) {
        return this.classLoaderDelegate.getResource(toFullResourcePath(name));
    }

    protected Manifest readManifest() throws RuntimeException {
        try {
            final InputStream manifestInputStream = openResourceStream("META-INF/MANIFEST.MF");
            if (manifestInputStream == null)
                return null;

            return new Manifest(manifestInputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read MANIFEST file: " + e.getMessage(), e);
        }
    }

    private String getDefinedAttribute(Attributes.Name name, Attributes entryAttribute, Attributes mainAttribute) {
        final String value = entryAttribute == null ? null : entryAttribute.getValue(name);
        return value == null ? mainAttribute == null ? null : mainAttribute.getValue(name) : value;
    }

    /**
     * @param resourceName
     * @return
     */
    protected String toFullResourcePath(final String resourceName) {
        final StringBuilder fullResourcePath = (new StringBuilder())
                .append(this.resourceRootPath)
                .append(resourceName.startsWith("/") ? resourceName.substring(1) : resourceName);
        return fullResourcePath.toString();
    }
}

