/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
     * @param resolve {@code true} to initialize the class
     * @return the class, or {@code null} if there is no local class with this name
     */
    Class<?> loadClassLocal(String name, boolean resolve);

    /**
     * Load a resource which is locally defined by this loader.
     *
     * @param name the resource path
     * @return the resource or resources, or an empty list if there is no local resource with this name
     */
    List<Resource> loadResourceLocal(String name);

    /**
     * Load a resource which is locally defined by a specific root on this loader.
     *
     * @param name the resource path
     * @return the resource, or {@code null} if there is no local resource with this name
     */
    Resource loadResourceLocal(String root, String name);
}
