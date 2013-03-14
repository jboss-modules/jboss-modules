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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

final class FilteredIterableResourceLoader implements IterableResourceLoader {

    private final PathFilter filter;
    private final IterableResourceLoader loader;

    FilteredIterableResourceLoader(final PathFilter filter, final IterableResourceLoader loader) {
        this.filter = filter;
        this.loader = loader;
    }

    public String getRootName() {
        return loader.getRootName();
    }

    public ClassSpec getClassSpec(final String fileName) throws IOException {
        final String canonicalFileName = PathUtils.canonicalize(PathUtils.relativize(fileName));
        return filter.accept(canonicalFileName) ? loader.getClassSpec(canonicalFileName) : null;
    }

    public PackageSpec getPackageSpec(final String name) throws IOException {
        return loader.getPackageSpec(PathUtils.canonicalize(PathUtils.relativize(name)));
    }

    public Resource getResource(final String name) {
        final String canonicalFileName = PathUtils.canonicalize(PathUtils.relativize(name));
        return filter.accept(canonicalFileName) ? loader.getResource(canonicalFileName) : null;
    }

    public String getLibrary(final String name) {
        return loader.getLibrary(PathUtils.canonicalize(PathUtils.relativize(name)));
    }

    public Collection<String> getPaths() {
        return loader.getPaths();
    }

    public Iterator<Resource> iterateResources(final String startPath, final boolean recursive) {
        return PathFilters.filtered(filter, loader.iterateResources(PathUtils.relativize(PathUtils.canonicalize(startPath)), recursive));
    }
}
