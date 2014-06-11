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
