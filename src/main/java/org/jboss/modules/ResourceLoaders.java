/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.io.File;
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
}
