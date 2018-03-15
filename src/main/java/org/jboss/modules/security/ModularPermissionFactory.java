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
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules._private.ModulesPrivateAccess;

/**
 * A permission factory which instantiates a permission from a module.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModularPermissionFactory implements PermissionFactory {
    private final ModuleLoader moduleLoader;
    private final String moduleName;
    private final String className;
    private final String targetName;
    private final String permissionActions;

    private volatile Permission instance = UninitializedPermission.INSTANCE;

    private static final ModulesPrivateAccess access = Module.getPrivateAccess();

    /**
     * Construct a new instance.
     *
     * @param moduleLoader the module loader from which the module is to be loaded
     * @param moduleIdentifier the module identifier from which the permission is to be loaded
     * @param className the name of the permission class
     * @param targetName the name to pass to the permission class constructor or {@code null} for none
     * @param permissionActions the action list to pass to the permission class constructor or {@code null} for none
     * @deprecated Use {@link #ModularPermissionFactory(ModuleLoader, String, String, String, String)} instead.
     */
    @Deprecated
    public ModularPermissionFactory(final ModuleLoader moduleLoader, final ModuleIdentifier moduleIdentifier, final String className, final String targetName, final String permissionActions) {
        this(moduleLoader, moduleIdentifier.toString(), className, targetName, permissionActions);
    }

    /**
     * Construct a new instance.
     *
     * @param moduleLoader the module loader from which the module is to be loaded
     * @param moduleName the module name from which the permission is to be loaded
     * @param className the name of the permission class
     * @param targetName the name to pass to the permission class constructor or {@code null} for none
     * @param permissionActions the action list to pass to the permission class constructor or {@code null} for none
     */
    public ModularPermissionFactory(final ModuleLoader moduleLoader, final String moduleName, final String className, final String targetName, final String permissionActions) {
        if (moduleLoader == null) {
            throw new IllegalArgumentException("moduleLoader is null");
        }
        if (moduleName == null) {
            throw new IllegalArgumentException("moduleName is null");
        }
        if (className == null) {
            throw new IllegalArgumentException("className is null");
        }
        this.moduleLoader = moduleLoader;
        this.moduleName = moduleName;
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
                final Module module = moduleLoader.loadModule(moduleName);
                final Class<? extends Permission> permissionClass = access.getClassLoaderOf(module).loadClass(className, true).asSubclass(Permission.class);
                return instance = PermissionFactory.constructFromClass(permissionClass, targetName, permissionActions);
            } catch (Throwable t) {
                instance = null;
                return null;
            }
        }
    }
}
