/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.io.File;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test to verify the functionality of module properties.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ModulePropertyTest extends AbstractModuleTestCase {
    private ModuleLoader moduleLoader;

    @Before
    public void setupModuleLoader() throws Exception {
        final File repoRoot = getResource("test/repo");
        moduleLoader = new LocalModuleLoader(new File[] {repoRoot});
    }

    @Test
    public void testBasic() throws Exception {
        Module module = moduleLoader.loadModule(MODULE_ID);
        assertNull(module.getProperty("non-existent"));
        assertEquals("blah", module.getProperty("non-existent", "blah"));
        assertEquals("true", module.getProperty("test.prop.1"));
        assertEquals("propertyValue", module.getProperty("test.prop.2"));
    }
}
