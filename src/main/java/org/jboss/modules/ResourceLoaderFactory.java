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

import java.net.URL;

/**
 * <p>
 * A factory for {@link ResourceLoader <code>ResourceLoader</code>} instances.
 * </p>
 * <p>
 * Which concrete <code>ResourceLoader</code> implementation is capable of handling a given <i>resource root</i> is usually
 * determined by a <i>module repository</i>'s type, i.e. <i>filesystem-based</i>, <i>classpath-based</i> and so forth.
 * Therefore, <code>ResourceLoaderFactories</code> are usually per module repository. This translates into {@link ModuleLoader
 * <code>ModuleLoaders</code>} holding a reference to a <code>ResourceLoaderFactory</code>.
 * </p>
 *
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 */
interface ResourceLoaderFactory {

    /**
     * <p>
     * Create and return a {@link ResourceLoader <code>ResourceLoader</code>} instance appropriate for the parameters passed in.
     * If this <code>ResourceLoaderFactory</code> does not know how to create an appropriate instance, it will throw a
     * {@link NoResourceLoaderForResourceRootException <code>NoResourceLoaderForResourceRootException</code>}.
     * </p>
     *
     * @param moduleRoot       The {@link URL <code>URL</code>} of the <i>module root</i>
     * @param resourceRootName The <i>resource root's</i> name
     * @param resourceRootPath The <i>resource root's</i> path, relative to the <code>moduleRoot</code>
     * @return A {@link ResourceLoader <code>ResourceLoader</code>} instance appropriate for the parameters passed in, never
     *         <code>null</code>
     * @throws IllegalArgumentException If any of the parameters is <code>null</code>
     * @throws NoResourceLoaderForResourceRootException
     *                                  If this <code>ResourceLoaderFactory</code> fails to create a
     *                                  <code>ResourceLoader</code> capable of handling the given <code>resourceRootPath</code>
     */
    ResourceLoader create(final URL moduleRoot, final String resourceRootName, final String resourceRootPath) throws IllegalArgumentException,
            NoResourceLoaderForResourceRootException;
}
