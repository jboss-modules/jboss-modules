package org.jboss.modules.maven;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by bob on 4/21/17.
 */
public class NonProxyHostTest {

    @Test
    public void testExactMatch() throws MalformedURLException {
        MavenSettings.NonProxyHost nph = new MavenSettings.NonProxyHost("www.google.com");
        assertTrue(nph.matches(new URL("http://www.google.com/")));
        assertFalse(nph.matches(new URL("http://google.com/")));
        assertFalse(nph.matches(new URL("http://www.apple.com/")));
    }

    @Test
    public void testWildcardMatch() throws MalformedURLException {
        MavenSettings.NonProxyHost nph = new MavenSettings.NonProxyHost("*.google.com");
        assertTrue(nph.matches(new URL("http://www.google.com/")));
        assertTrue(nph.matches(new URL("http://wave.google.com/")));
        assertFalse(nph.matches(new URL("http://google.com/")));
        assertFalse(nph.matches(new URL("http://www.apple.com/")));
    }
}
