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
import java.io.IOException;
import java.io.FileOutputStream;
import java.security.AccessController;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import org.jboss.modules.filter.PathFilter;

/**
 * Static factory methods for various types of resource loaders.
 *
 * @apiviz.exclude
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ResourceLoaders {
    private static final String JBOSS_TMP_DIR_PROPERTY = "jboss.server.temp.dir";
    private static final String JVM_TMP_DIR_PROPERTY = "java.io.tmpdir";
    static final boolean USE_INDEXES;
    static final boolean WRITE_INDEXES;
    static final File TMP_ROOT;

    static {
        USE_INDEXES = Boolean.parseBoolean(AccessController.doPrivileged(new PropertyReadAction("jboss.modules.use-indexes", "false")));
        WRITE_INDEXES = USE_INDEXES && Boolean.parseBoolean(AccessController.doPrivileged(new PropertyReadAction("jboss.modules.write-indexes", "false")));
        String configTmpDir = AccessController.doPrivileged(new PropertyReadAction(JBOSS_TMP_DIR_PROPERTY));
        if (configTmpDir == null)  configTmpDir = AccessController.doPrivileged(new PropertyReadAction(JVM_TMP_DIR_PROPERTY));
        TMP_ROOT = new File(configTmpDir);
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
    public static ResourceLoader createFileResourceLoader(final String name, final File root) {
        return new FileResourceLoader(name, root, AccessController.getContext());
    }

    /**
     * Create a filesystem-backed iterable resource loader with support for native libraries.  Created classes
     * have a code source with a {@code file:} URL.
     *
     * @param name the name of the resource root
     * @param root the root file of the resource loader
     * @return the resource loader
     */
    public static IterableResourceLoader createIterableFileResourceLoader(final String name, final File root) {
        return new FileResourceLoader(name, root, AccessController.getContext());
    }

    /**
     * Create a JAR-backed resource loader.  JAR resource loaders do not have native library support.
     * Created classes have a code source with a {@code jar:} URL; nested JARs are not supported.
     *
     * @param name the name of the resource root
     * @param jarFile the backing JAR file
     * @return the resource loader
     */
    public static ResourceLoader createJarResourceLoader(final String name, final JarFile jarFile) {
        return new JarFileResourceLoader(name, jarFile);
    }

    /**
     * Create a JAR-backed iterable resource loader.  JAR resource loaders do not have native library support.
     * Created classes have a code source with a {@code jar:} URL; nested JARs are not supported.
     *
     * @param name the name of the resource root
     * @param jarFile the backing JAR file
     * @return the resource loader
     */
    public static IterableResourceLoader createIterableJarResourceLoader(final String name, final JarFile jarFile) {
        return new JarFileResourceLoader(name, jarFile);
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
        return new FilteredResourceLoader(pathFilter, originalLoader);
    }

    /**
     * Create a filtered view of an iterable resource loader, which allows classes to be included or excluded on a name basis.
     * The given filter is matched against the actual class or resource name, not the directory name.
     *
     * @param pathFilter the path filter to apply
     * @param originalLoader the original loader to apply to
     * @return the filtered resource loader
     */
    public static IterableResourceLoader createIterableFilteredResourceLoader(final PathFilter pathFilter, final IterableResourceLoader originalLoader) {
        return new FilteredIterableResourceLoader(pathFilter, originalLoader);
    }

    /**
     * Creates a subresource filtered view of an iterable resource loader.
     * Only resources under subresource path will be accessible by new loader.
     *
     * @param name the name of the resource root
     * @param originalLoader the original loader to create filtered view from
     * @param subresourcePath subresource path that will behave like the root of the archive
     * @return a subresource filtered view of an iterable resource loader.
     */
    public static IterableResourceLoader createSubresourceIterableResourceLoader(final String name, final IterableResourceLoader originalLoader, final String subresourcePath) throws IOException {
        if (name == null || originalLoader == null || subresourcePath == null) {
            throw new NullPointerException("Method parameter cannot be null");
        }
        final String subResPath = PathUtils.relativize(PathUtils.canonicalize(subresourcePath));
        if (subResPath.equals("")) {
            throw new IllegalArgumentException("Cannot create subresource loader for archive root");
        }
        final Resource resource = originalLoader.getResource(subResPath);
        if (resource == null) {
            throw new IllegalArgumentException("Subresource '" + subResPath + "' does not exist");
        }
        IterableResourceLoader loader = originalLoader;
        if (resource.isDirectory()) {
            while (loader instanceof FilteredIterableResourceLoader) loader = ((FilteredIterableResourceLoader)loader).getLoader();
            if (loader instanceof FileResourceLoader) {
                return new FileResourceLoader(name, new File(resource.getURL().getFile()), AccessController.getContext());
            } else if (loader instanceof JarFileResourceLoader) {
                return new JarFileResourceLoader(name, new JarFile(((JarFileResourceLoader) loader).getFile()), subResPath);
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            while (loader instanceof FilteredIterableResourceLoader) loader = ((FilteredIterableResourceLoader)loader).getLoader();
            if (loader instanceof FileResourceLoader) {
                return new JarFileResourceLoader(name, new JarFile(resource.getURL().getFile()));
            } else if (loader instanceof JarFileResourceLoader) {
                final File tempFile = new File(TMP_ROOT, getLastToken(subResPath) + ".tmp" + System.currentTimeMillis());
                IOUtils.copyAndClose(resource.openStream(), new FileOutputStream(tempFile));
                // TODO: wrap returned JarFileRL with IterableResourceLoader that will remove created temp file on close.
                return new JarFileResourceLoader(name, new JarFile(tempFile));
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static String getLastToken(final String path) {
        final StringTokenizer st = new StringTokenizer(path, "/");
        String lastToken = st.nextToken();
        while (st.hasMoreTokens()) {
            lastToken = st.nextToken();
        }
        return lastToken;
    }

}
