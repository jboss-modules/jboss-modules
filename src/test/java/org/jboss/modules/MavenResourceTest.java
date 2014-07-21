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

import org.junit.Assert;
import org.jboss.modules.util.Util;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MavenResourceTest {

    protected static final ModuleIdentifier MODULE_ID = ModuleIdentifier.fromString("test.maven");

    @Rule
    public TemporaryFolder tmpdir = new TemporaryFolder();

    private ModuleLoader moduleLoader;

    @Before
    public void setupRepo() throws Exception {
        final File repoRoot = Util.getResourceFile(getClass(), "test/repo");
        moduleLoader = new LocalModuleLoader(new File[] { repoRoot });
    }

    @Test
    public void testWithPassedRepository() throws Exception {
        System.setProperty("local.maven.repo.path", tmpdir.newFolder("repository").getAbsolutePath());
        System.setProperty("remote.maven.repo", "http://repository.jboss.org/nexus/content/groups/public/");
        try {
            Module module = moduleLoader.loadModule(MODULE_ID);
            URL url = module.getResource("org/jboss/resteasy/plugins/providers/jackson/ResteasyJacksonProvider.class");
            System.out.println(url);
            Assert.assertNotNull(url);
        } finally {
            System.clearProperty("local.repository.path");
            System.clearProperty("remote.repository");
        }
    }

    /**
     * we test if it uses repostiory user has configured in user.home/.m2/settings.xml
     * @throws Exception
     */
    @Test
    public void testCustomRepository() throws Exception{
        //MavenArtifactUtil.resolveJarArtifact("org.wildfly.core:wildfly-controller:1.0.0.Alpha2");
        MavenArtifactUtil.resolveJarArtifact("org.wildfly:wildfly-clustering-infinispan:9.0.0.Alpha1-SNAPSHOT");
        //moduleLoader.loadModule(ModuleIdentifier.fromString("${org.wildfly.core:wildfly-controller}"));

    }
}
