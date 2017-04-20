package org.jboss.modules.maven;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by bob on 4/21/17.
 */
public class NonProxyHostTest {

    @Test
    public void testExactMatch() throws MalformedURLException {
        MavenSettings.NonProxyHost nph = new MavenSettings.NonProxyHost("www.google.com");
        Assert.assertTrue(nph.matches(new URL("http://www.google.com/")));
        Assert.assertFalse(nph.matches(new URL("http://google.com/")));
        Assert.assertFalse(nph.matches(new URL("http://www.apple.com/")));
    }

    @Test
    public void testWildcardMatch() throws MalformedURLException {
        MavenSettings.NonProxyHost nph = new MavenSettings.NonProxyHost("*.google.com");
        Assert.assertTrue(nph.matches(new URL("http://www.google.com/")));
        Assert.assertTrue(nph.matches(new URL("http://wave.google.com/")));
        Assert.assertFalse(nph.matches(new URL("http://google.com/")));
        Assert.assertFalse(nph.matches(new URL("http://www.apple.com/")));
    }
}
