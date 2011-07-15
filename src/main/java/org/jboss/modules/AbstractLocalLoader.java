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

import java.util.List;

/**
 * An abstract local loader implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractLocalLoader implements LocalLoader {

    /**
     * Load a class which is locally defined by this loader.  Returns {@code null} by default.
     *
     * @param name the class name
     * @param resolve {@code true} to resolve the class
     *
     * @return the class, or {@code null} if there is no local class with this name
     */
    public Class<?> loadClassLocal(final String name, final boolean resolve) {
        return null;
    }

    /**
     * Load a package which is locally defined by this loader.  Returns {@code null} by default.
     *
     * @param name the package name
     *
     * @return the package, or {@code null} if there is no local package with this name
     */
    public Package loadPackageLocal(final String name) {
        return null;
    }

    /**
     * Load a resource which is locally defined by this loader.  The given name is a path separated by "{@code /}"
     * characters.  Returns {@code null} by default.
     *
     * @param name the resource path
     *
     * @return the resource or resources, or an empty list if there is no local resource with this name
     */
    public List<Resource> loadResourceLocal(final String name) {
        return null;
    }
}
