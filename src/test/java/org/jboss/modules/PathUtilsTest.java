/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
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

import static org.jboss.modules.PathUtils.isChild;
import static org.jboss.modules.PathUtils.isDirectChild;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test to verify the PathUtils functionality.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class PathUtilsTest {

    @Test
    public void testIsChild() {
        assertTrue(isChild("app.war/", "app.war/W"));
        assertTrue(isDirectChild("app.war/", "app.war/W"));
        assertTrue(isChild("app.war/", "app.war/FOO/BAR"));
        assertFalse(isDirectChild("app.war/", "app.war/FOO/BAR"));
        assertTrue(isChild("app.war", "app.war/W"));
        assertTrue(isDirectChild("app.war", "app.war/W"));
        assertTrue(isChild("app.war", "app.war/FOO/BAR"));
        assertFalse(isDirectChild("app.war", "app.war/FOO/BAR"));
        assertFalse(isDirectChild("app.war", "app.war/"));
        assertFalse(isChild("app.war", "app.war/"));
    }

}
