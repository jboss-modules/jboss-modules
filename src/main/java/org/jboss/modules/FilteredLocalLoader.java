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

import java.util.Collections;
import java.util.List;
import org.jboss.modules.filter.ClassFilter;
import org.jboss.modules.filter.PathFilter;

/**
* @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
*/
class FilteredLocalLoader implements LocalLoader {

    private final ClassFilter classFilter;
    private final LocalLoader originalLoader;
    private final PathFilter resourcePathFilter;

    FilteredLocalLoader(final ClassFilter classFilter, final PathFilter resourcePathFilter, final LocalLoader originalLoader) {
        this.classFilter = classFilter;
        this.originalLoader = originalLoader;
        this.resourcePathFilter = resourcePathFilter;
    }

    public Class<?> loadClassLocal(final String name, final boolean resolve) {
        return classFilter.accept(name) ? originalLoader.loadClassLocal(name, resolve) : null;
    }

    public Package loadPackageLocal(final String name) {
        return originalLoader.loadPackageLocal(name);
    }

    public List<Resource> loadResourceLocal(final String name) {
        return resourcePathFilter.accept(name) ? originalLoader.loadResourceLocal(name) : Collections.<Resource>emptyList();
    }
}
