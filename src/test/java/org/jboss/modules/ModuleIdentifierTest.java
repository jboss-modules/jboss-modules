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
        assertEquals("test.module", identifier.toString());

        identifier = ModuleIdentifier.fromString("test.module:old");
        assertEquals("test.module:old", identifier.toString());
    }

    @Test
    public void testInvalidCharacters() throws Exception {
        try {
            ModuleIdentifier.fromString("test.module\\test");
        } catch (IllegalArgumentException unexpected) {
            fail("Should not have thrown IllegalArgumentException");
        }
        try {
            ModuleIdentifier.fromString("test/module/test");
        } catch (IllegalArgumentException unexpected) {
            fail("Should have thrown IllegalArgumentException");
        }

        try {
            ModuleIdentifier.fromString("test,module,test");
        } catch (IllegalArgumentException unexpected) {
            fail("Should have thrown IllegalArgumentException");
        }

    }
}
