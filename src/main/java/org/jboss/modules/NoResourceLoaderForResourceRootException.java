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

/**
 * <p>
 * A specialized {@link ModuleLoadException <code>ModuleLoadException</code>} indicating that a {@link ResourceLoaderFactory
 * <code>ResourceLoaderFactory</code>} failed to create a {@link ResourceLoader <code>ResourceLoader</code>} appropriate for
 * handling a given <i>resource root</i>.
 * </p>
 *
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 */
public class NoResourceLoaderForResourceRootException extends ModuleLoadException {

    private static final long serialVersionUID = 2726462006116848338L;

    private final String resourceRootName;

    private final String resourceRootPath;

    /**
     *
     */
    public NoResourceLoaderForResourceRootException(final String resourceRootName,
                                                    final String resourceRootPath) {
        super();
        this.resourceRootName = resourceRootName;
        this.resourceRootPath = resourceRootPath;
    }

    /**
     * @param msg
     */
    public NoResourceLoaderForResourceRootException(final String resourceRootName,
                                                    final String resourceRootPath, final String msg) {
        super(msg);
        this.resourceRootName = resourceRootName;
        this.resourceRootPath = resourceRootPath;
    }

    /**
     * @param cause
     */
    public NoResourceLoaderForResourceRootException(final String resourceRootName,
                                                    final String resourceRootPath, final Throwable cause) {
        super(cause);
        this.resourceRootName = resourceRootName;
        this.resourceRootPath = resourceRootPath;
    }

    /**
     * @param msg
     * @param cause
     */
    public NoResourceLoaderForResourceRootException(final String resourceRootName,
                                                    final String resourceRootPath, final String msg, final Throwable cause) {
        super(msg, cause);
        this.resourceRootName = resourceRootName;
        this.resourceRootPath = resourceRootPath;
    }

    /**
     * @return the resourceRootName
     */
    public final String getResourceRootName() {
        return resourceRootName;
    }

    /**
     * @return the resourceRootPath
     */
    public final String getResourceRootPath() {
        return resourceRootPath;
    }
}