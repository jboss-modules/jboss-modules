/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

import java.io.File;
import java.net.URL;
import java.util.List;

import org.jboss.modules.util.Util;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MavenResourceTest {

    protected static final ModuleIdentifier MODULE_ID = ModuleIdentifier.fromString("test.maven");
    protected static final ModuleIdentifier MODULE_ID2 = ModuleIdentifier.fromString("test.maven:non-main");

    @Rule
    public TemporaryFolder tmpdir = new TemporaryFolder();

    private ModuleLoader moduleLoader;

    @Before
    public void setupRepo() throws Exception {
        final File repoRoot = Util.getResourceFile(getClass(), "test/repo");
        moduleLoader = new LocalModuleLoader(new File[]{repoRoot});
    }

    @Test
    public void testWithPassedRepository() throws Exception {
        System.setProperty("maven.repo.local", tmpdir.newFolder("repository").getAbsolutePath());
        System.setProperty("remote.maven.repo", "http://repository.jboss.org/nexus/content/groups/public/,https://maven-central.storage.googleapis.com/");
        try {
            Module module = moduleLoader.loadModule(MODULE_ID);
            URL url = module.getResource("org/jboss/resteasy/plugins/providers/jackson/ResteasyJacksonProvider.class");
            System.out.println(url);
            Assert.assertNotNull(url);

            MavenSettings settings = MavenArtifactUtil.getSettings();
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
    public void testDefaultRepositories() throws Exception {
        Module module = moduleLoader.loadModule(MODULE_ID2);
        URL url = module.getResource("org/jboss/resteasy/plugins/providers/jackson/ResteasyJacksonProvider.class");
        System.out.println(url);
        Assert.assertNotNull(url);

    }

    /**
     * we test if it uses repostiory user has configured in user.home/.m2/settings.xml or M2_HOME/conf/settings.xml
     *
     * @throws Exception
     */
    @Test
    @Ignore("Test artifact must exists in local repo but nowhere else, mostly meant for manual testing")
    public void testCustomRepository() throws Exception {
        MavenArtifactUtil.resolveJarArtifact("org.wildfly:wildfly-clustering-infinispan:9.0.0.Alpha1-SNAPSHOT");

    }
}
