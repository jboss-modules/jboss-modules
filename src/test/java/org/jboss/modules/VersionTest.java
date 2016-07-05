/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

import static org.jboss.modules.Version.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class VersionTest {

    private static void parseFail(String str) {
        try {
            parse(str);
            fail("Expected parsing to fail for " + str);
        } catch (IllegalArgumentException ok) {
        }
    }

    @Test
    public void testParsing() {
        parse("1");
        parse("a");
        parseFail(".");
        parse("1.1");
        parse("1.a");
        parse("1a");
        parse("a1");
        parse("1+1");
        parse("1-1");
        parse("1_1");
        parse("1_1a.1993-12-31");
        parseFail("1.");
        parseFail("1..");
        parseFail(".1");
    }

    private boolean testCompare(Version v1, Version v2) {
        final boolean res = v1.equals(v2);
        if (res) {
            assertTrue(v1.hashCode() == v2.hashCode());
        }
        return res;
    }

    @Test
    public void testEquals() {
        assertTrue(testCompare(parse("1.0"), parse("1.0")));
        assertTrue(testCompare(parse("a1"), parse("a1")));
        assertFalse(testCompare(parse("1.1"), parse("1.0")));
        assertFalse(testCompare(parse("1.1"), parse("1.01")));
        assertFalse(testCompare(parse("1.1"), parse("1.10")));
        assertFalse(testCompare(parse("1.1"), parse("1.100")));
    }

    @Test
    public void testCompareTo() {
        assertEquals(0, parse("1.0").compareTo(parse("1.0")));
        assertEquals(-1, parse("1.0").compareTo(parse("1.0.0")));
        assertEquals(1, parse("1.0.0.0").compareTo(parse("1.0.0")));
        assertEquals(1, parse("5u1").compareTo(parse("5")));
        assertEquals(-1, parse("5u1").compareTo(parse("5.1")));
    }

    @Test
    public void testIterate() {
        Version.Iterator i = parse("1.0.0u2").iterator();
        assertTrue(i.hasNext());
        i.next();
        assertTrue(i.isPart());
        assertTrue(i.isNumberPart());
        assertFalse(i.isAlphaPart());
        assertFalse(i.isSeparator());
        assertFalse(i.isEmptySeparator());
        assertFalse(i.isNonEmptySeparator());
        assertEquals(1, i.getNumberPartAsInt());
        assertEquals(1, i.getNumberPartAsLong());
        assertEquals("1", i.getNumberPartAsString());
        assertTrue(i.hasNext());
        i.next();
        assertFalse(i.isPart());
        assertFalse(i.isNumberPart());
        assertFalse(i.isAlphaPart());
        assertTrue(i.isSeparator());
        assertFalse(i.isEmptySeparator());
        assertTrue(i.isNonEmptySeparator());
        assertEquals('.', i.getSeparatorCodePoint());
        assertTrue(i.hasNext());
        i.next();
        assertTrue(i.isPart());
        assertTrue(i.isNumberPart());
        assertFalse(i.isAlphaPart());
        assertFalse(i.isSeparator());
        assertFalse(i.isEmptySeparator());
        assertFalse(i.isNonEmptySeparator());
        assertEquals(0, i.getNumberPartAsInt());
        assertEquals(0, i.getNumberPartAsLong());
        assertEquals("0", i.getNumberPartAsString());
        assertTrue(i.hasNext());
        i.next();
        assertFalse(i.isPart());
        assertFalse(i.isNumberPart());
        assertFalse(i.isAlphaPart());
        assertTrue(i.isSeparator());
        assertFalse(i.isEmptySeparator());
        assertTrue(i.isNonEmptySeparator());
        assertEquals('.', i.getSeparatorCodePoint());
        assertTrue(i.hasNext());
        i.next();
        assertTrue(i.isPart());
        assertTrue(i.isNumberPart());
        assertFalse(i.isAlphaPart());
        assertFalse(i.isSeparator());
        assertFalse(i.isEmptySeparator());
        assertFalse(i.isNonEmptySeparator());
        assertEquals(0, i.getNumberPartAsInt());
        assertEquals(0, i.getNumberPartAsLong());
        assertEquals("0", i.getNumberPartAsString());
        assertTrue(i.hasNext());
        i.next();
        assertFalse(i.isPart());
        assertFalse(i.isNumberPart());
        assertFalse(i.isAlphaPart());
        assertTrue(i.isSeparator());
        assertTrue(i.isEmptySeparator());
        assertFalse(i.isNonEmptySeparator());
        assertTrue(i.hasNext());
        i.next();
        assertTrue(i.isPart());
        assertFalse(i.isNumberPart());
        assertTrue(i.isAlphaPart());
        assertFalse(i.isSeparator());
        assertFalse(i.isEmptySeparator());
        assertFalse(i.isNonEmptySeparator());
        assertEquals("u", i.getAlphaPart());
        assertTrue(i.hasNext());
        i.next();
        assertFalse(i.isPart());
        assertFalse(i.isNumberPart());
        assertFalse(i.isAlphaPart());
        assertTrue(i.isSeparator());
        assertTrue(i.isEmptySeparator());
        assertFalse(i.isNonEmptySeparator());
        assertTrue(i.hasNext());
        i.next();
        assertTrue(i.isPart());
        assertTrue(i.isNumberPart());
        assertFalse(i.isAlphaPart());
        assertFalse(i.isSeparator());
        assertFalse(i.isEmptySeparator());
        assertFalse(i.isNonEmptySeparator());
        assertEquals(2, i.getNumberPartAsInt());
        assertEquals(2, i.getNumberPartAsLong());
        assertEquals("2", i.getNumberPartAsString());
        assertFalse(i.hasNext());
    }
}
