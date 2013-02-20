/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.modules.security;

import java.lang.reflect.Constructor;
import java.security.Permission;

/**
 * A permission factory which instantiates a permission with the given class name.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LoadedPermissionFactory implements PermissionFactory {
    private final ClassLoader classLoader;
    private final String className;
    private final String targetName;
    private final String permissionActions;

    private volatile Permission instance = UninitializedPermission.INSTANCE;

    /**
     * Construct a new instance.
     *
     * @param classLoader the class loader from which the permission should be loaded
     * @param className the name of the permission class
     * @param targetName the name to pass to the permission class constructor or {@code null} for none
     * @param permissionActions the action list to pass to the permission class constructor or {@code null} for none
     */
    public LoadedPermissionFactory(final ClassLoader classLoader, final String className, final String targetName, final String permissionActions) {
        if (className == null) {
            throw new IllegalArgumentException("className is null");
        }
        this.classLoader = classLoader;
        this.className = className;
        this.targetName = targetName;
        this.permissionActions = permissionActions;
    }

    public Permission construct() {
        if (instance != UninitializedPermission.INSTANCE) {
            return instance;
        }
        synchronized (this) {
            if (instance != UninitializedPermission.INSTANCE) {
                return instance;
            }
            try {
                final Class<? extends Permission> permissionClass = classLoader.loadClass(className).asSubclass(Permission.class);
                final Constructor<? extends Permission> constructor = permissionClass.getConstructor(String.class, String.class);
                return instance = constructor.newInstance(targetName, permissionActions);
            } catch (Throwable t) {
                instance = null;
                return null;
            }
        }
    }
}
