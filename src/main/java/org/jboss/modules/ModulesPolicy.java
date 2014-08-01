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

import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Provider;

final class ModulesPolicy extends Policy {

    private static final AllPermission ALL_PERMISSION = new AllPermission();

    static final Permissions DEFAULT_PERMISSION_COLLECTION = getAllPermission();

    private static final CodeSource ourCodeSource = ModulesPolicy.class.getProtectionDomain().getCodeSource();

    private final Policy policy;

    private static Permissions getAllPermission() {
        final Permissions permissions = new Permissions();
        permissions.add(ALL_PERMISSION);
        return permissions;
    }

    public ModulesPolicy(final Policy policy) {
        this.policy = policy;
    }

    public Provider getProvider() {
        return policy.getProvider();
    }

    public String getType() {
        return policy.getType();
    }

    public Parameters getParameters() {
        return policy.getParameters();
    }

    public PermissionCollection getPermissions(final CodeSource codesource) {
        return codesource.equals(ourCodeSource) ? getAllPermission() : policy.getPermissions(codesource);
    }

    public PermissionCollection getPermissions(final ProtectionDomain domain) {
        final CodeSource codeSource = domain.getCodeSource();
        return codeSource != null && codeSource.equals(ourCodeSource) ? getAllPermission() : policy.getPermissions(domain);
    }

    public boolean implies(final ProtectionDomain domain, final Permission permission) {
        final CodeSource codeSource = domain.getCodeSource();
        return codeSource != null && codeSource.equals(ourCodeSource) || policy.implies(domain, permission);
    }

    public void refresh() {
        policy.refresh();
    }
}
