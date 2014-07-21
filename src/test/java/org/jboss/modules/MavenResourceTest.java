/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
