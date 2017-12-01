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

import java.io.File;
import java.nio.file.Path;
import java.security.AccessController;
import java.util.jar.JarFile;
import org.jboss.modules.filter.PathFilter;

/**
 * Static factory methods for various types of resource loaders.
 *
 * @apiviz.exclude
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ResourceLoaders {
    static final boolean USE_INDEXES;
    static final boolean WRITE_INDEXES;

    static {
        USE_INDEXES = Boolean.parseBoolean(AccessController.doPrivileged(new PropertyReadAction("jboss.modules.use-indexes", "false")));
        WRITE_INDEXES = USE_INDEXES && Boolean.parseBoolean(AccessController.doPrivileged(new PropertyReadAction("jboss.modules.write-indexes", "false")));
    }

    private ResourceLoaders() {
    }

    /**
     * Create a filesystem-backed resource loader with support for native libraries.  Created classes
     * have a code source with a {@code file:} URL.
     *
     * @param name the name of the resource root
     * @param root the root file of the resource loader
     * @return the resource loader
     */
    public static IterableResourceLoader createFileResourceLoader(final String name, final File root) {
        return new FileResourceLoader(name, root, AccessController.getContext());
    }

    public static ResourceLoader createFileResourceLoader$$bridge(final String name, final File root) {
        return createFileResourceLoader(name, root);
    }

    /**
     * Create a filesystem-backed iterable resource loader with support for native libraries.  Created classes
     * have a code source with a {@code file:} URL.
     *
     * @param name the name of the resource root
     * @param root the root file of the resource loader
     * @return the resource loader
     * @deprecated Use {@link #createFileResourceLoader(String, File)} instead.
     */
    public static IterableResourceLoader createIterableFileResourceLoader(final String name, final File root) {
        return createFileResourceLoader(name, root);
    }

    /**
     * Create a JAR-backed resource loader.  JAR resource loaders do not have native library support.
     * Created classes have a code source with a {@code jar:} URL; nested JARs are not supported.
     *
     * @param name the name of the resource root
     * @param jarFile the backing JAR file
     * @return the resource loader
     */
    public static IterableResourceLoader createJarResourceLoader(final String name, final JarFile jarFile) {
        return new JarFileResourceLoader(name, jarFile);
    }

    public static ResourceLoader createJarResourceLoader$$bridge(final String name, final JarFile jarFile) {
        return createJarResourceLoader(name, jarFile);
    }

    /**
     * Create a JAR-backed resource loader.  JAR resource loaders do not have native library support.
     * Created classes have a code source with a {@code jar:} URL; nested JARs are not supported.  The given
     * relative path within the JAR is used as the root of the loader.
     *
     * @param name the name of the resource root
     * @param jarFile the backing JAR file
     * @return the resource loader
     */
    public static IterableResourceLoader createJarResourceLoader(final String name, final JarFile jarFile, final String relativePath) {
        return new JarFileResourceLoader(name, jarFile, relativePath);
    }

    /**
     * Create a JAR-backed iterable resource loader.  JAR resource loaders do not have native library support.
     * Created classes have a code source with a {@code jar:} URL; nested JARs are not supported.
     *
     * @param name the name of the resource root
     * @param jarFile the backing JAR file
     * @return the resource loader
     * @deprecated Use {@link #createJarResourceLoader(String, JarFile)} instead.
     */
    @Deprecated
    public static IterableResourceLoader createIterableJarResourceLoader(final String name, final JarFile jarFile) {
        return createJarResourceLoader(name, jarFile);
    }

    /**
     * Create a filtered view of a resource loader, which allows classes to be included or excluded on a name basis.
     * The given filter is matched against the actual class or resource name, not the directory name.
     *
     * @param pathFilter the path filter to apply
     * @param originalLoader the original loader to apply to
     * @return the filtered resource loader
     */
    public static ResourceLoader createFilteredResourceLoader(final PathFilter pathFilter, final ResourceLoader originalLoader) {
        return originalLoader instanceof IterableResourceLoader ? new FilteredIterableResourceLoader(pathFilter, (IterableResourceLoader) originalLoader) : new FilteredResourceLoader(pathFilter, originalLoader);
    }

    /**
     * Create a filtered view of an iterable resource loader, which allows classes to be included or excluded on a name basis.
     * The given filter is matched against the actual class or resource name, not the directory name.
     *
     * @param pathFilter the path filter to apply
     * @param originalLoader the original loader to apply to
     * @return the filtered resource loader
     */
    public static IterableResourceLoader createFilteredResourceLoader(final PathFilter pathFilter, final IterableResourceLoader originalLoader) {
        return new FilteredIterableResourceLoader(pathFilter, originalLoader);
    }

    /**
     * Create a filtered view of an iterable resource loader, which allows classes to be included or excluded on a name basis.
     * The given filter is matched against the actual class or resource name, not the directory name.
     *
     * @param pathFilter the path filter to apply
     * @param originalLoader the original loader to apply to
     * @return the filtered resource loader
     * @deprecated Use {@link #createFileResourceLoader(String, File)} instead.
     */
    public static IterableResourceLoader createIterableFilteredResourceLoader(final PathFilter pathFilter, final IterableResourceLoader originalLoader) {
        return createFilteredResourceLoader(pathFilter, originalLoader);
    }

    /**
     * Create a NIO2 Path-backed iterable resource loader.
     *
     * @param name the name of the resource root
     * @param path the root path of the resource loader
     * @return the resource loader
     */
    public static IterableResourceLoader createPathResourceLoader(final String name, final Path path) {
        return new PathResourceLoader(name, path, AccessController.getContext());
    }
}
