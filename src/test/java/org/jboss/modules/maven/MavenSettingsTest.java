package org.jboss.modules.maven;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Tomaz Cerar (c) 2015 Red Hat Inc.
 */
public class MavenSettingsTest {

    void clearCachedSettings() throws Exception {
        Field mavenSettings = MavenSettings.class.getDeclaredField("mavenSettings");
        mavenSettings.setAccessible(true);
        mavenSettings.set(null, null);
    }

    @Rule
    public TemporaryFolder tmpdir = new TemporaryFolder();

    @Test
    public void testWithPassedRepository() throws Exception {
        System.setProperty("maven.repo.local", tmpdir.newFolder("repository").getAbsolutePath());
        System.setProperty("remote.maven.repo", "http://repository.jboss.org/nexus/content/groups/public/,https://maven-central.storage.googleapis.com/");

        try {
            clearCachedSettings();
            MavenSettings settings = MavenSettings.getSettings();
            List<String> remoteRepos = settings.getRemoteRepositories();
            Assert.assertTrue(remoteRepos.size() >= 3); //at least 3 must be present, other can come from settings.xml
            Assert.assertTrue(remoteRepos.contains("https://repo1.maven.org/maven2/"));
            Assert.assertTrue(remoteRepos.contains("http://repository.jboss.org/nexus/content/groups/public/"));
            Assert.assertTrue(remoteRepos.contains("https://maven-central.storage.googleapis.com/"));

        } finally {
            System.clearProperty("maven.repo.local");
            System.clearProperty("remote.repository");
        }
    }

    @Test
    public void testWithEmptyPassedRepository() throws Exception {
        Path userRepo = tmpdir.newFolder(".m2", "repository").toPath();
        String userHome = System.getProperty("user.home");
        System.setProperty("user.home", tmpdir.getRoot().getAbsolutePath());
        System.setProperty("maven.repo.local", "");

        try {
            clearCachedSettings();
            MavenSettings settings = MavenSettings.getSettings();
            Assert.assertEquals(userRepo, settings.getLocalRepository());
        } finally {
            System.setProperty("user.home", userHome);
            System.clearProperty("maven.repo.local");
        }
    }

    @Test
    public void testEmptyLocalRepo() throws Exception {
        MavenSettings settings = new MavenSettings();

        MavenSettings.parseSettingsXml(Paths.get(MavenSettingsTest.class.getResource("settings-empty-local-repo.xml").toURI()), settings);
        Assert.assertNull(settings.getLocalRepository());//local repo shouldn't be set

    }

    @Test
    public void testInterpolatedLocalRepo() throws Exception {
        Path userRepo = tmpdir.newFolder(".m2", "repository").toPath();
        String userHome = System.getProperty("user.home");
        System.setProperty("user.home", tmpdir.getRoot().getAbsolutePath());

        try {
            clearCachedSettings();
            MavenSettings settings = new MavenSettings();

            MavenSettings.parseSettingsXml(Paths.get(MavenSettingsTest.class.getResource("settings-interpolated-local-repo.xml").toURI()), settings);
            Assert.assertEquals(Paths.get(tmpdir.getRoot().getAbsolutePath() + "/.mvnrepository"), settings.getLocalRepository());
        } finally {
            System.setProperty("user.home", userHome);
        }
    }


    @Test
    public void testProxies() throws Exception {
        MavenSettings settings = new MavenSettings();

        MavenSettings.parseSettingsXml(Paths.get(MavenSettingsTest.class.getResource("settings-empty-local-repo.xml").toURI()), settings);
        List<MavenSettings.Proxy> proxies = settings.getProxies();
        Assert.assertEquals(1, proxies.size());

        MavenSettings.Proxy proxy = proxies.get(0);

        Assert.assertEquals("my-proxy", proxy.getId());
        Assert.assertEquals("myproxy.corp.com", proxy.getHost());
        Assert.assertEquals(8080, proxy.getPort());
        Assert.assertEquals("http", proxy.getProtocol());
        Assert.assertEquals("bob", proxy.getUsername());
        Assert.assertEquals("hunter2", proxy.getPassword());

        Assert.assertTrue(proxy.canProxyFor(new URL("http://www.redhat.com/")));
        Assert.assertFalse(proxy.canProxyFor(new URL("http://genius.apple.com/")));

        Proxy netProxy = proxy.getProxy();

        Assert.assertNotNull(netProxy);

        Assert.assertEquals("myproxy.corp.com", ((InetSocketAddress) netProxy.address()).getHostName());
        Assert.assertEquals(8080, ((InetSocketAddress) netProxy.address()).getPort());
    }

    @Test
    public void testProxySelection() throws Exception {
        MavenSettings settings = new MavenSettings();

        MavenSettings.parseSettingsXml(Paths.get(MavenSettingsTest.class.getResource("settings-empty-local-repo.xml").toURI()), settings);
        List<MavenSettings.Proxy> proxies = settings.getProxies();
        Assert.assertEquals(1, proxies.size());

        MavenSettings.Proxy proxy = settings.getProxyFor(new URL("http://genius.apple.com/foo/bar/baz"));
        Assert.assertNull(proxy);

        proxy = settings.getProxyFor(new URL("http://repository.jboss.org/foo/bar/baz"));
        Assert.assertNotNull(proxy);

        Assert.assertEquals("myproxy.corp.com", ((InetSocketAddress) proxy.getProxy().address()).getHostName());
        Assert.assertEquals(8080, ((InetSocketAddress) proxy.getProxy().address()).getPort());
    }

    /**
     * testing is snapshot resolving works properly, as in case of snapshot version, we need to use different path than exact version.
     *
     * @throws Exception
     */
    @Test
    public void testSnapshotResolving() throws Exception {
        ArtifactCoordinates coordinates = ArtifactCoordinates.fromString("org.wildfly.core:wildfly-version:2.0.5.Final-20151222.144931-1");
        String path = coordinates.relativeArtifactPath('/');
        Assert.assertEquals("org/wildfly/core/wildfly-version/2.0.5.Final-SNAPSHOT/wildfly-version-2.0.5.Final-20151222.144931-1", path);
    }

    @Test
    public void testInterpolateVariablesOneVariable() throws Exception {
        try {
            System.setProperty( "test.user.home", "/home/bob" );
            Assert.assertEquals("/home/bob/.m2/repository", MavenSettings.interpolateVariables("${test.user.home}/.m2/repository"));
        } finally {
            System.clearProperty("test.user.home");
        }
    }

    @Test
    public void testInterpolateVariablesTwoVariables() throws Exception {
        try {
            System.setProperty( "test.user.home", "/home/bob" );
            System.setProperty( "test.repo.dir", "repository" );
            Assert.assertEquals("/home/bob/.m2/repository", MavenSettings.interpolateVariables("${test.user.home}/.m2/${test.repo.dir}"));
        } finally {
            System.clearProperty("test.user.home");
        }
    }

    @Test
    public void testInterpolateVariablesInvalidExpression() throws Exception {
        try {
            System.setProperty( "test.user.home", "/home/bob" );
            Assert.assertEquals("${test.user.home/.m2/repository", MavenSettings.interpolateVariables("${test.user.home/.m2/repository"));
        } finally {
            System.clearProperty("test.user.home");
        }
    }

    @Test
    public void testInterpolateVariablesInvalidExpression2() throws Exception {
        try {
            System.setProperty( "test.user.home", "/home/bob" );
            Assert.assertEquals("/home/bob/.m2/${repoName", MavenSettings.interpolateVariables("${test.user.home}/.m2/${repoName"));
        } finally {
            System.clearProperty("test.user.home");
        }
    }
}
