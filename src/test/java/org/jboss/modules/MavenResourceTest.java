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

import org.jboss.modules.maven.ArtifactCoordinates;
import org.jboss.modules.maven.MavenArtifactUtil;
import org.jboss.modules.util.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MavenResourceTest {

    protected static final String MODULE_ID = "test.maven";

    private ModuleLoader moduleLoader;

    @BeforeEach
    public void setupRepo() throws Exception {
        final File repoRoot = Util.getResourceFile(getClass(), "test/repo");
        moduleLoader = new LocalModuleLoader(new File[]{repoRoot});
    }

    @Test
    public void testWithPassedRepository() throws Exception {
        File repoDir = new File(System.getProperty("java.io.tmpdir"), "MavenResourceTest_repository" + System.currentTimeMillis());
        repoDir.mkdirs();
        System.setProperty("maven.repo.local", repoDir.getAbsolutePath());
        System.setProperty("remote.maven.repo", "https://repository.jboss.org/nexus/content/groups/public/,https://maven-central.storage.googleapis.com/");
        try {
            Module module = moduleLoader.loadModule(MODULE_ID);
            URL url = module.getResource("org/jboss/resteasy/plugins/providers/jackson/ResteasyJacksonProvider.class");
            System.out.println(url);
            assertNotNull(url);
        } finally {
            System.clearProperty("maven.repo.local");
            System.clearProperty("remote.maven.repo");
        }
    }

    /**
     * we test if it uses repostiory user has configured in user.home/.m2/settings.xml or M2_HOME/conf/settings.xml
     *
     * @throws Exception
     */
    @Test
    @Disabled("Test is mostly meant for manual testing, as snapshot are not parmanent")
    public void testCustomRepository() throws Exception {
        File f = MavenArtifactUtil.resolveJarArtifact(ArtifactCoordinates.fromString("org.wildfly.core:wildfly-version:2.0.5.Final-20151222.144931-1"));
        assertNotNull(f);
        System.out.println("f = " + f);
    }
}
