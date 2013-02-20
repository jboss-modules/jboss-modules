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
