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

import java.io.File;

import static org.junit.Assert.*;

/**
 * Test to verify the PathUtils functionality.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class PathUtilsTest {

    /**
     * Canonicalize the given path.  Removes all {@code .} and {@code ..} segments from the path.
     *
     * This is the original code, preserved here to test for correctness.
     *
     * @param path the relative or absolute possibly non-canonical path
     * @return the canonical path
     */
    static String originalCanonicalize(String path) {
        final int length = path.length();
        // 0 - start
        // 1 - got one .
        // 2 - got two .
        // 3 - got /
        int state = 0;
        if (length == 0) {
            return path;
        }
        final char[] targetBuf = new char[length];
        // string segment end exclusive
        int e = length;
        // string cursor position
        int i = length;
        // buffer cursor position
        int a = length - 1;
        // number of segments to skip
        int skip = 0;
        loop: while (--i >= 0) {
            char c = path.charAt(i);
            outer: switch (c) {
                case '/': {
                    inner: switch (state) {
                        case 0: state = 3; e = i; break outer;
                        case 1: state = 3; e = i; break outer;
                        case 2: state = 3; e = i; skip ++; break outer;
                        case 3: e = i; break outer;
                        default: throw new IllegalStateException();
                    }
                    // not reached!
                }
                case '.': {
                    inner: switch (state) {
                        case 0: state = 1; break outer;
                        case 1: state = 2; break outer;
                        case 2: break inner; // emit!
                        case 3: state = 1; break outer;
                        default: throw new IllegalStateException();
                    }
                    // fall thru
                }
                default: {
                    if (File.separatorChar != '/' && c == File.separatorChar) {
                        switch (state) {
                            case 0: state = 3; e = i; break outer;
                            case 1: state = 3; e = i; break outer;
                            case 2: state = 3; e = i; skip ++; break outer;
                            case 3: e = i; break outer;
                            default: throw new IllegalStateException();
                        }
                        // not reached!
                    }
                    final int newE = e > 0 ? path.lastIndexOf('/', e - 1) : -1;
                    final int segmentLength = e - newE - 1;
                    if (skip > 0) {
                        skip--;
                    } else {
                        if (state == 3) {
                            targetBuf[a--] = '/';
                        }
                        path.getChars(newE + 1, e, targetBuf, (a -= segmentLength) + 1);
                    }
                    state = 0;
                    i = newE + 1;
                    e = newE;
                    break;
                }
            }
        }
        if (state == 3) {
            targetBuf[a--] = '/';
        }
        return new String(targetBuf, a + 1, length - a - 1);
    }

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
        // remove the current buffer size or this math won't work since
        // the buffer would be already initialized
        CHAR_BUFFER_CACHE.remove();

        assertEquals(MIN_LENGTH, getBuffer(0).length);
        assertEquals(MIN_LENGTH, getBuffer(1).length);
        assertEquals(MIN_LENGTH, getBuffer(MIN_LENGTH - 1).length);
        assertEquals(MIN_LENGTH, getBuffer(MIN_LENGTH).length);
        assertEquals(MIN_LENGTH * 2, getBuffer(MIN_LENGTH + 1).length);
        assertEquals(MIN_LENGTH * 4, getBuffer(MIN_LENGTH * 3 + 1).length);
    }

    // ensure that buffer sharing doesn't cause issues for different path lengths by
    // running a test that recycles the same buffer in the same thread. this test uses
    // increasing and decreasing string lengths to ensure that no unintended data makes it
    // in the string
    @Test
    public void testCorrectAgainstOriginal() {
        assertEquals(originalCanonicalize(".."), PathUtils.canonicalize(".."));
        assertEquals(originalCanonicalize("."), PathUtils.canonicalize("."));
        assertEquals(originalCanonicalize("/foo/bar"), PathUtils.canonicalize("/foo/bar"));
        assertEquals(originalCanonicalize("/foo/../bar"), PathUtils.canonicalize("/foo/../bar"));
        assertEquals(originalCanonicalize("/bar/.."), PathUtils.canonicalize("/bar/.."));
        assertEquals(originalCanonicalize("META-INF/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/."), PathUtils.canonicalize("META-INF/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/."));
        assertEquals(originalCanonicalize("/baz/./"), PathUtils.canonicalize("/baz/./"));
        assertEquals(originalCanonicalize("/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz"), PathUtils.canonicalize("/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz/baz"));
        assertEquals(originalCanonicalize("/baz/hidden.properties/.."), PathUtils.canonicalize("/baz/hidden.properties/.."));
        assertEquals(originalCanonicalize("/baz/./hidden.properties/../../../check..thing/./"), PathUtils.canonicalize("/baz/./hidden.properties/../../../check..thing/./"));
    }


    // run the correctness test over and over to ensure that the buffer reuse doesn't cause issues
    @Test
    public void testReusedBuffer() {
        for(int i = 0; i < 100; i++) {
            this.testCorrectAgainstOriginal();
        }
    }
}
