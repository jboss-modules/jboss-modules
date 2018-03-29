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
import org.jboss.modules.filter.PathFilter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class FilteredResourceLoader implements ResourceLoader {

    private final PathFilter filter;
    private final ResourceLoader loader;

    FilteredResourceLoader(final PathFilter filter, final ResourceLoader loader) {
        this.filter = filter;
        this.loader = loader;
    }

    @Deprecated
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

    public void close() {
        loader.close();
    }

    public URI getLocation() {
        return loader.getLocation();
    }
}
