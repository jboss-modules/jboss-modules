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

import static org.jboss.modules.PathUtils.*;

import org.junit.Test;

import static org.junit.Assert.*;

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

    @Test
    public void testBasicNames() {
        assertEquals("basic/main", basicModuleNameToPath("basic"));
        assertEquals("a/b/main", basicModuleNameToPath("a.b"));
        assertEquals("a/b/c", basicModuleNameToPath("a.b:c"));
        assertEquals("a/b/c.d", basicModuleNameToPath("a.b:c.d"));
        assertEquals("a/b/c:d", basicModuleNameToPath("a.b:c:d"));
        assertEquals("a/b/c.d", basicModuleNameToPath("a.b:c/d"));
        assertEquals("a.b/c.d", basicModuleNameToPath("a/b:c.d"));
        assertEquals("a:b/c.d", basicModuleNameToPath("a\\:b:c.d"));

        assertNull(basicModuleNameToPath("."));
        assertNull(basicModuleNameToPath(".."));
        assertNull(basicModuleNameToPath("./"));
        assertNull(basicModuleNameToPath("/foo"));
        assertNull(basicModuleNameToPath(".foo"));
        assertNull(basicModuleNameToPath("foo/."));
        assertNull(basicModuleNameToPath("foo/"));
        assertNull(basicModuleNameToPath("foo:"));
        assertNull(basicModuleNameToPath("foo//bar"));
        assertNull(basicModuleNameToPath("foo..bar"));
    }

    @Test
    public void testBufferSize() {
        assertEquals(MIN_LENGTH, getBuffer(0).length);
        assertEquals(MIN_LENGTH, getBuffer(1).length);
        assertEquals(MIN_LENGTH, getBuffer(MIN_LENGTH - 1).length);
        assertEquals(MIN_LENGTH, getBuffer(MIN_LENGTH).length);
        assertEquals(MIN_LENGTH * 2, getBuffer(MIN_LENGTH + 1).length);
        assertEquals(MIN_LENGTH * 4, getBuffer(MIN_LENGTH * 3 + 1).length);
    }

    // ensure that buffer sharing doesn't cause issues for different path lengths by
    // running a test that recycles the same buffer in the same thread
    @Test
    public void testReusedBuffer() {
        for(int i = 0; i < 100; i++) {
            assertEquals("/foo/bar", PathUtils.canonicalize("/foo/bar"));
            assertEquals("/bar", PathUtils.canonicalize("/foo/../bar"));
            assertEquals("/", PathUtils.canonicalize("/bar/.."));
            assertEquals("/baz/", PathUtils.canonicalize("/baz/./"));
            assertEquals("/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz", PathUtils.canonicalize("/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz"));
        }
    }
}
