package org.jboss.modules.maven;

import java.lang.reflect.Field;
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

    /**
     * testing is snapshot resolving works properly, as in case of snapshot version, we need to use different path than exact version.
     * @throws Exception
     */
    @Test
    public void testSnapshotResolving()throws Exception{
        ArtifactCoordinates coordinates = ArtifactCoordinates.fromString("org.wildfly.core:wildfly-version:2.0.5.Final-20151222.144931-1");
        String path = coordinates.relativeArtifactPath('/');
        Assert.assertEquals("org/wildfly/core/wildfly-version/2.0.5.Final-SNAPSHOT/wildfly-version-2.0.5.Final-20151222.144931-1", path);
    }
}
