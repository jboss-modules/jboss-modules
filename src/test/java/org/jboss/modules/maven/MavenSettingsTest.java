package org.jboss.modules.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Tomaz Cerar (c) 2015 Red Hat Inc.
 */
public class MavenSettingsTest {

    void clearCachedSettings() throws Exception {
        Field mavenSettings = MavenSettings.class.getDeclaredField("mavenSettings");
        mavenSettings.setAccessible(true);
        mavenSettings.set(null, null);
    }

    @TempDir
    public Path tmpdir;

    @Test
    public void testLocalRepositoryOverriddenViaSystemProperty() throws Exception {
        URL settingsXmlUrl = MavenSettingsTest.class.getResource("settings-with-local-repo-defined.xml");
        System.setProperty("jboss.modules.settings.xml.url", settingsXmlUrl.toExternalForm());
        String temporaryLocalRepository = Files.createFile(tmpdir.resolve("repository")).toFile().getAbsolutePath();
        System.setProperty("maven.repo.local", temporaryLocalRepository);

        try {
            clearCachedSettings();
            MavenSettings settings = MavenSettings.getSettings();
            assertEquals(temporaryLocalRepository, settings.getLocalRepository().toString());
        } finally {
            System.clearProperty("maven.repo.local");
            System.clearProperty("jboss.modules.settings.xml.url");
        }
    }

    @Test
    public void testLocalRepositoryNotOverriddenViaSystemProperty() throws Exception {
        URL settingsXmlUrl = MavenSettingsTest.class.getResource("settings-with-local-repo-defined.xml");
        System.setProperty("jboss.modules.settings.xml.url", settingsXmlUrl.toExternalForm());

        try {
            clearCachedSettings();
            MavenSettings settings = MavenSettings.getSettings();
            assertEquals("/user/defined/path/in/settings/xml".replace('/', File.separatorChar), settings.getLocalRepository().toString());
        } finally {
            System.clearProperty("jboss.modules.settings.xml.url");
        }
    }

    @Test
    public void testWithPassedRepository() throws Exception {
        System.setProperty("maven.repo.local", Files.createFile(tmpdir.resolve("repository")).toFile().getAbsolutePath());
        System.setProperty("remote.maven.repo", "http://repository.jboss.org/nexus/content/groups/public/,https://maven-central.storage.googleapis.com/");

        try {
            clearCachedSettings();
            MavenSettings settings = MavenSettings.getSettings();
            List<String> remoteRepos = settings.getRemoteRepositories();
            assertTrue(remoteRepos.size() >= 3); //at least 3 must be present, other can come from settings.xml
            assertTrue(remoteRepos.contains("https://repo1.maven.org/maven2/"));
            assertTrue(remoteRepos.contains("http://repository.jboss.org/nexus/content/groups/public/"));
            assertTrue(remoteRepos.contains("https://maven-central.storage.googleapis.com/"));

        } finally {
            System.clearProperty("maven.repo.local");
            System.clearProperty("remote.repository");
        }
    }

    @Test
    public void testWithEmptyPassedRepository() throws Exception {
        Path userRepo = Files.createDirectories(tmpdir.resolve(".m2").resolve("repository"));
        String userHome = System.getProperty("user.home");
        System.setProperty("user.home", tmpdir.toFile().getAbsolutePath());
        System.setProperty("maven.repo.local", "");

        try {
            clearCachedSettings();
            MavenSettings settings = MavenSettings.getSettings();
            assertEquals(userRepo, settings.getLocalRepository());
        } finally {
            System.setProperty("user.home", userHome);
            System.clearProperty("maven.repo.local");
        }
    }

    @Test
    public void testEmptyLocalRepo() throws Exception {
        MavenSettings settings = new MavenSettings();

        MavenSettings.parseSettingsXml(Paths.get(MavenSettingsTest.class.getResource("settings-empty-local-repo.xml").toURI()), settings);
        assertNull(settings.getLocalRepository());//local repo shouldn't be set

    }

    @Test
    public void testInterpolatedLocalRepo() throws Exception {
        Path userRepo = Files.createDirectories(tmpdir.resolve(".m2").resolve("repository"));
        String userHome = System.getProperty("user.home");
        System.setProperty("user.home", tmpdir.getRoot().toFile().getAbsolutePath());

        try {
            clearCachedSettings();
            MavenSettings settings = new MavenSettings();

            MavenSettings.parseSettingsXml(Paths.get(MavenSettingsTest.class.getResource("settings-interpolated-local-repo.xml").toURI()), settings);
            assertEquals(Paths.get(tmpdir.getRoot().toFile().getAbsolutePath() + "/.mvnrepository"), settings.getLocalRepository());
        } finally {
            System.setProperty("user.home", userHome);
        }
    }


    @Test
    public void testProxies() throws Exception {
        MavenSettings settings = new MavenSettings();

        MavenSettings.parseSettingsXml(Paths.get(MavenSettingsTest.class.getResource("settings-empty-local-repo.xml").toURI()), settings);
        List<MavenSettings.Proxy> proxies = settings.getProxies();
        assertEquals(1, proxies.size());

        MavenSettings.Proxy proxy = proxies.get(0);

        assertEquals("my-proxy", proxy.getId());
        assertEquals("myproxy.corp.com", proxy.getHost());
        assertEquals(8080, proxy.getPort());
        assertEquals("http", proxy.getProtocol());
        assertEquals("bob", proxy.getUsername());
        assertEquals("hunter2", proxy.getPassword());

        assertTrue(proxy.canProxyFor(new URL("http://www.redhat.com/")));
        assertFalse(proxy.canProxyFor(new URL("http://genius.apple.com/")));

        Proxy netProxy = proxy.getProxy();

        assertNotNull(netProxy);

        assertEquals("myproxy.corp.com", ((InetSocketAddress) netProxy.address()).getHostName());
        assertEquals(8080, ((InetSocketAddress) netProxy.address()).getPort());
    }

    @Test
    public void testProxySelection() throws Exception {
        MavenSettings settings = new MavenSettings();

        MavenSettings.parseSettingsXml(Paths.get(MavenSettingsTest.class.getResource("settings-empty-local-repo.xml").toURI()), settings);
        List<MavenSettings.Proxy> proxies = settings.getProxies();
        assertEquals(1, proxies.size());

        MavenSettings.Proxy proxy = settings.getProxyFor(new URL("http://genius.apple.com/foo/bar/baz"));
        assertNull(proxy);

        proxy = settings.getProxyFor(new URL("http://repository.jboss.org/foo/bar/baz"));
        assertNotNull(proxy);

        assertEquals("myproxy.corp.com", ((InetSocketAddress) proxy.getProxy().address()).getHostName());
        assertEquals(8080, ((InetSocketAddress) proxy.getProxy().address()).getPort());
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
        assertEquals("org/wildfly/core/wildfly-version/2.0.5.Final-SNAPSHOT/wildfly-version-2.0.5.Final-20151222.144931-1", path);
    }

    @Test
    public void testInterpolateVariablesOneVariable() throws Exception {
        try {
            System.setProperty( "test.user.home", "/home/bob" );
            assertEquals("/home/bob/.m2/repository", MavenSettings.interpolateVariables("${test.user.home}/.m2/repository"));
        } finally {
            System.clearProperty("test.user.home");
        }
    }

    @Test
    public void testInterpolateVariablesTwoVariables() throws Exception {
        try {
            System.setProperty( "test.user.home", "/home/bob" );
            System.setProperty( "test.repo.dir", "repository" );
            assertEquals("/home/bob/.m2/repository", MavenSettings.interpolateVariables("${test.user.home}/.m2/${test.repo.dir}"));
        } finally {
            System.clearProperty("test.user.home");
        }
    }

    @Test
    public void testInterpolateVariablesInvalidExpression() throws Exception {
        try {
            System.setProperty( "test.user.home", "/home/bob" );
            assertEquals("${test.user.home/.m2/repository", MavenSettings.interpolateVariables("${test.user.home/.m2/repository"));
        } finally {
            System.clearProperty("test.user.home");
        }
    }

    @Test
    public void testInterpolateVariablesInvalidExpression2() throws Exception {
        try {
            System.setProperty( "test.user.home", "/home/bob" );
            assertEquals("/home/bob/.m2/${repoName", MavenSettings.interpolateVariables("${test.user.home}/.m2/${repoName"));
        } finally {
            System.clearProperty("test.user.home");
        }
    }
}
