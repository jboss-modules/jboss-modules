/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;

import org.jboss.modules.ModuleClassLoader;

/**
 * A protection domain which has a (modular) class loader but whose dynamicity is selectable.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ModularProtectionDomain extends ProtectionDomain {

    private final boolean dynamic;

    /**
     * Construct a new instance.  The {@code dynamic} parameter determines whether the policy is consulted for permission
     * checks; if {@code false}, then only the specified permission collection is consulted.
     *
     * @param codeSource the code source of the protection domain (must not be {@code null})
     * @param permissions the permission collection (must not be {@code null})
     * @param moduleClassLoader the module class loader (must not be {@code null})
     * @param dynamic {@code true} if the protection domain should be dynamic, {@code false} otherwise
     */
    public ModularProtectionDomain(final CodeSource codeSource, final PermissionCollection permissions, final ModuleClassLoader moduleClassLoader, boolean dynamic) {
        super(codeSource, permissions, moduleClassLoader, null);
        this.dynamic = dynamic;
    }

    /**
     * Construct a new, static instance.  Only the specified permission collection is consulted for permission checks.
     *
     * @param codeSource the code source of the protection domain (must not be {@code null})
     * @param permissions the permission collection (must not be {@code null})
     * @param moduleClassLoader the module class loader (must not be {@code null})
     */
    public ModularProtectionDomain(final CodeSource codeSource, final PermissionCollection permissions, final ModuleClassLoader moduleClassLoader) {
        this(codeSource, permissions, moduleClassLoader, false);
    }

    /**
     * Determine if the permission collection of this protection domain implies the given permission.  This is
     * just a shortcut for calling {@code getPermissions().implies(permission)}.
     *
     * @param permission the permission to check (must not be {@code null})
     * @return {@code true} if the permission is implied, {@code false} otherwise.
     */
    public boolean implies(final Permission permission) {
        return dynamic ? super.implies(permission) : getPermissions().implies(permission);
    }

    /**
     * Get a string representation of this protection domain.
     *
     * @return the string representation (not {@code null})
     */
    public String toString() {
        return String.format("ProtectionDomain (%s) %s%n%s%n%s%n%s", dynamic ? "dynamic" : "static", getCodeSource(), getClassLoader(), getPermissions());
    }
}
