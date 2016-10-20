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
        final ModuleLoader moduleLoader = new ModuleLoader(new ModuleFinder[]{
            new ModuleFinder() {
                public ModuleSpec findModule(final String name, final ModuleLoader delegateLoader) throws ModuleLoadException {
                    if (name.equals("test")) {
                        final Permissions perms = new Permissions();
                        perms.add(new AllPermission());
                        return ModuleSpec.build("test").setPermissionCollection(perms).create();
                    } else {
                        return null;
                    }
                }
            }
        });
        final ModularPermissionFactory factory = new ModularPermissionFactory(moduleLoader, "test", RuntimePermission.class.getName(), "foo", "*");
        assertEquals(new RuntimePermission("foo", "*"), factory.construct());
    }
}
