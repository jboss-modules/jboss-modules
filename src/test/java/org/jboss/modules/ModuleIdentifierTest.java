/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.junit.Test;

/**
 * @author John E. Bailey
 */
public class ModuleIdentifierTest {

    @Test
    public void testFromString() throws Exception {
        ModuleIdentifier identifier = ModuleIdentifier.fromString("test.module");
        assertEquals("test.module", identifier.getName());
        assertEquals("main", identifier.getSlot());

        identifier = ModuleIdentifier.fromString("test.module:old");
        assertEquals("test.module", identifier.getName());
        assertEquals("old", identifier.getSlot());
    }

    @Test
    public void testToString() {
        ModuleIdentifier identifier = ModuleIdentifier.fromString("test.module");
        assertEquals("module:test.module:main", identifier.toString());

        identifier = ModuleIdentifier.fromString("test.module:old");
        assertEquals("module:test.module:old", identifier.toString());
    }

    @Test
    public void testInvalidCharacters() throws Exception {
        try {
            ModuleIdentifier.fromString("test.module\\test");
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ModuleIdentifier.fromString("test/module/test");
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }

        try {
            ModuleIdentifier.fromString("test,module,test");
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }

    }
}
