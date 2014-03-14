/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.security.AllPermission;
import java.security.Permissions;

import org.jboss.modules.security.ModularPermissionFactory;
import org.junit.Test;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class InstantiatePrivateAccessTest {

    @Test
    public void ensureFailure() {
        try {
            Module.getPrivateAccess();
            fail("Expected security exception");
        } catch (SecurityException ok) {}
    }

    @Test
    public void ensureModularPermissionFactory() {
        final ModuleIdentifier test = ModuleIdentifier.create("test");
        final ModuleLoader moduleLoader = new ModuleLoader(new ModuleFinder[]{
            new ModuleFinder() {
                public ModuleSpec findModule(final ModuleIdentifier identifier, final ModuleLoader delegateLoader) throws ModuleLoadException {
                    if (identifier.equals(test)) {
                        final Permissions perms = new Permissions();
                        perms.add(new AllPermission());
                        return ModuleSpec.build(test).setPermissionCollection(perms).create();
                    } else {
                        return null;
                    }
                }
            }
        });
        final ModularPermissionFactory factory = new ModularPermissionFactory(moduleLoader, test, RuntimePermission.class.getName(), "foo", "*");
        assertEquals(new RuntimePermission("foo", "*"), factory.construct());
    }
}
