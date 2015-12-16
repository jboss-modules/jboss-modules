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

package org.jboss.modules.security;

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
                return instance = PermissionFactory.constructFromClass(permissionClass, targetName, permissionActions);
            } catch (Throwable t) {
                instance = null;
                return null;
            }
        }
    }
}
