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
 * A loader which implements the local part of a module.
 * <p>
 * <b>Thread safety warning!</b>  The loader must <b>never</b> call into a class loader (or any other object) which may
 * take locks and subsequently delegate to a module class loader.  This will cause deadlocks and other hard-to-debug
 * concurrency problems.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface LocalLoader {

    /**
     * Load a class which is locally defined by this loader.
     *
     * @param name the class name
     * @param resolve {@code true} to resolve the class
     * @return the class, or {@code null} if there is no local class with this name
     */
    Class<?> loadClassLocal(String name, boolean resolve);

    /**
     * Load a package which is locally defined by this loader.
     *
     * @param name the package name
     * @return the package, or {@code null} if there is no local package with this name
     */
    Package loadPackageLocal(String name);

    /**
     * Load a resource which is locally defined by this loader.  The given name is a path separated
     * by "{@code /}" characters.
     *
     * @param name the resource path
     * @return the resource or resources, or an empty list if there is no local resource with this name
     */
    List<Resource> loadResourceLocal(String name);
}
