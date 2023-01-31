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

package org.jboss.modules.xml;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FilePermission;
import java.security.Permission;
import java.util.Enumeration;

import org.jboss.modules.AbstractModuleTestCase;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.junit.Before;
import org.junit.Test;

/**
 * Test to verify the functionality of module permissions
 *
 * @author <a href="mailto:jshepherd@redhat.com">Jason Shepherd</a>
 */
public class PermissionsTest extends AbstractModuleTestCase {
    private static final String JBOSS_HOME_DIR_VALUE = "someDir/someOtherDir";
    protected static final String MODULE_WITH_INVALID_EXPANSION = "test.permissions";
    private ModuleLoader moduleLoader;

    @Before
    public void setupModuleLoader() throws Exception {
        System.setProperty("jboss.home.dir", JBOSS_HOME_DIR_VALUE);
        System.setProperty("foo.bar", "substituted_value");
        final File repoRoot = getResource("test/repo");
        moduleLoader = new LocalModuleLoader(new File[] {repoRoot});
    }

    @Test
    public void testExpansion() throws Exception {
        Module module = moduleLoader.loadModule(MODULE_WITH_INVALID_EXPANSION);
        assertTrue(module.getExportedResource("active.txt").toString().contains("substituted_value"));
        Enumeration<Permission> permissions = module.getPermissionCollection().elements();
        assertTrue(permissions.hasMoreElements());
        Permission firstPermission = permissions.nextElement();
        assertEquals(FilePermission.class.getName(), firstPermission.getClass().getName());
        assertFalse(permissions.hasMoreElements());
    }
}
