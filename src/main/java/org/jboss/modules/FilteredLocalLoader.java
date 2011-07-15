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
        return resourcePathFilter.accept(name.replace('.', '/')) ? originalLoader.loadPackageLocal(name) : null;
    }

    public List<Resource> loadResourceLocal(final String name) {
        return resourcePathFilter.accept(name) ? originalLoader.loadResourceLocal(name) : Collections.<Resource>emptyList();
    }
}
