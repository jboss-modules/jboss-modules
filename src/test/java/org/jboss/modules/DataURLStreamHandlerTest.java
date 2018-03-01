/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 */
public class DataURLStreamHandlerTest {
    public DataURLStreamHandlerTest() {
    }

    static URL makeUrl(String str) throws MalformedURLException {
        return new URL(null, str, DataURLStreamHandler.getInstance());
    }

    static byte[] readAllBytes(URLConnection uc) throws IOException {
        final byte[] bytes = new byte[uc.getContentLength()];
        final InputStream inputStream = uc.getInputStream();
        if (inputStream.read(bytes) < bytes.length) {
            throw new IOException("incomplete read");
        }
        return bytes;
    }

    static byte[] bytes(int... vals) {
        final byte[] bytes = new byte[vals.length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) vals[i];
        }
        return bytes;
    }

    @Test
    public void testEmpty() throws Exception {
        final URLConnection urlConnection = makeUrl("data:,").openConnection();
        assertEquals(0, urlConnection.getContentLength());
        assertEquals("text/plain", urlConnection.getContentType());
        assertNull(urlConnection.getContentEncoding());
        assertEquals(-1, urlConnection.getInputStream().read());
    }

    @Test
    public void testText() throws Exception {
        final URLConnection urlConnection = makeUrl("data:,some kinda text").openConnection();
        assertEquals(15, urlConnection.getContentLength());
        assertEquals("text/plain", urlConnection.getContentType());
        assertEquals("some kinda text", new String(readAllBytes(urlConnection), StandardCharsets.US_ASCII));
    }

    @Test
    public void testTextUtf8() throws Exception {
        final URLConnection urlConnection = makeUrl("data:;charset=utf-8,some kïndä tëxt").openConnection();
        assertEquals(18, urlConnection.getContentLength());
        assertEquals("text/plain;charset=utf-8", urlConnection.getContentType());
        assertEquals("some kïndä tëxt", new String(readAllBytes(urlConnection), StandardCharsets.UTF_8));
    }

    @Test
    public void testTextUtf8Ascii() throws Exception {
        final URLConnection urlConnection = makeUrl("data:,some kïndä tëxt").openConnection();
        assertEquals(15, urlConnection.getContentLength());
        assertEquals("text/plain", urlConnection.getContentType());
        assertEquals("some k?nd? t?xt", new String(readAllBytes(urlConnection), StandardCharsets.UTF_8));
    }

    @Test
    public void testPlainBinary() throws Exception {
        final URLConnection urlConnection = makeUrl("data:x-whatever/stuff;charset=utf-8,some kïndä tëxt").openConnection();
        assertEquals(15, urlConnection.getContentLength());
        assertEquals("x-whatever/stuff", urlConnection.getContentType());
        assertEquals("some k�nd� t�xt", new String(readAllBytes(urlConnection), StandardCharsets.UTF_8));
    }

    @Test
    public void testRawBinary() throws Exception {
        final URLConnection urlConnection = makeUrl("data:x-whatever/stuff,%01%02%03%04%05%F0%FF%00").openConnection();
        assertEquals(8, urlConnection.getContentLength());
        assertEquals("x-whatever/stuff", urlConnection.getContentType());
        assertArrayEquals(bytes(0x01, 0x02, 0x03, 0x04, 0x05, 0xf0, 0xff, 0x00), readAllBytes(urlConnection));
    }

    @Test
    public void testBase64Binary() throws Exception {
        final URLConnection urlConnection = makeUrl("data:x-whatever/stuff;base64,dGhpcyBpcyBhIHRlc3QhCg==").openConnection();
        assertEquals(16, urlConnection.getContentLength());
        assertEquals("x-whatever/stuff", urlConnection.getContentType());
        assertArrayEquals("this is a test!\n".getBytes(StandardCharsets.UTF_8), readAllBytes(urlConnection));
    }
}
