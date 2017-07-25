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
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

/**
 * A local filesystem-backed module loader.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LocalModuleLoader extends ModuleLoader implements AutoCloseable {

    /**
     * Construct a new instance.
     *
     * @param repoRoots the array of repository roots to look for modules
     */
    public LocalModuleLoader(final File[] repoRoots) {
        this(repoRoots, PathFilters.acceptAll());
    }

    /**
     * Construct a new instance.
     *
     * @param repoRoots the array of repository roots to look for modules
     * @param pathFilter the path filter to apply to roots
     */
    public LocalModuleLoader(final File[] repoRoots, final PathFilter pathFilter) {
        super(new ModuleFinder[] { new LocalModuleFinder(repoRoots, pathFilter)});
    }

    /**
     * Construct a new instance, using the {@code module.path} system property or the {@code JAVA_MODULEPATH} environment variable
     * to get the list of module repository roots.
     */
    public LocalModuleLoader() {
        super(new ModuleFinder[] { new LocalModuleFinder() });
    }

    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("local module loader @").append(Integer.toHexString(hashCode())).append(" (finder: ").append(getFinders()[0]).append(')');
        return b.toString();
    }

    /**
     * Close this module loader and release all backing files.  Note that subsequent load attempts will fail with an
     * error after this method is called.
     */
    public void close() {
        final ModuleFinder[] finders = getFinders();
        assert finders.length == 1 && finders[0] instanceof LocalModuleFinder;
        ((LocalModuleFinder)finders[0]).close();
    }
}
