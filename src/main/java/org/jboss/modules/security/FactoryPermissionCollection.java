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
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;

/**
 * A permission collection which lazily instantiates the permission objects on first use.  Any
 * unavailable permission objects will not be granted to the resultant collection.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class FactoryPermissionCollection extends PermissionCollection {

    private static final long serialVersionUID = -2524371701490830970L;

    private final PermissionFactory[] factories;
    private volatile Permissions assembled;

    /**
     * Construct a new instance.
     *
     * @param factories the factories to use to construct the permission collection
     */
    public FactoryPermissionCollection(final PermissionFactory... factories) {
        this.factories = factories.clone();
    }

    public void add(final Permission permission) {
        throw new SecurityException("Read-only permission collection");
    }

    Permissions getAssembled() {
        if (assembled == null) {
            synchronized (this) {
                if (assembled == null) {
                    final Permissions assembled = new Permissions();
                    for (PermissionFactory factory : factories) {
                        if (factory != null) {
                            final Permission permission = factory.construct();
                            if (permission != null) {
                                assembled.add(permission);
                            }
                        }
                    }
                    assembled.setReadOnly();
                    this.assembled = assembled;
                }
            }
        }
        return assembled;
    }

    public boolean implies(final Permission permission) {
        return getAssembled().implies(permission);
    }

    public Enumeration<Permission> elements() {
        return getAssembled().elements();
    }

    Object writeReplace() {
        return getAssembled();
    }
}
