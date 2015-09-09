/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
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
import java.util.Collection;
import java.util.Iterator;

/**
 * IterableResourceLoader that delegates all method calls to its delegate.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class DelegatingIterableResourceLoader implements IterableResourceLoader {

    private final IterableResourceLoader delegate;

    DelegatingIterableResourceLoader(final IterableResourceLoader delegate) {
        this.delegate = delegate;
    }

    final IterableResourceLoader getDelegate() {
        return delegate;
    }

    @Override
    public Iterator<Resource> iterateResources(final String startPath, final boolean recursive) {
        return getDelegate().iterateResources(startPath, recursive);
    }

    @Override
    public String getRootName() {
        return getDelegate().getRootName();
    }

    @Override
    public ClassSpec getClassSpec(final String fileName) throws IOException {
        return getDelegate().getClassSpec(fileName);
    }

    @Override
    public PackageSpec getPackageSpec(final String name) throws IOException {
        return getDelegate().getPackageSpec(name);
    }

    @Override
    public Resource getResource(final String name) {
        return getDelegate().getResource(name);
    }

    @Override
    public String getLibrary(final String name) {
        return getDelegate().getLibrary(name);
    }

    @Override
    public Collection<String> getPaths() {
        return getDelegate().getPaths();
    }

    @Override
    public void close() throws IOException {
        getDelegate().close();
    }
}
