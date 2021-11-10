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

import org.jboss.modules.maven.MavenSettingsTest;
import org.jboss.modules.util.Util;
import org.junit.Assert;
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
public class MavenResource2Test {

    protected static final ModuleIdentifier MODULE_ID2 = ModuleIdentifier.fromString("test.maven:non-main");

    private ModuleLoader moduleLoader;

    @Before
    public void setupRepo() throws Exception {
        final File repoRoot = Util.getResourceFile(getClass(), "test/repo");
        moduleLoader = new LocalModuleLoader(new File[]{repoRoot});
    }

    @Test
    public void testDefaultRepositories() throws Exception {
        try {
            URL settingsXmlUrl = MavenSettingsTest.class.getResource("jboss-settings.xml");
            System.setProperty("jboss.modules.settings.xml.url", settingsXmlUrl.toExternalForm());
            Module module = moduleLoader.loadModule(MODULE_ID2);
            URL url = module.getResource("org/jboss/resteasy/plugins/providers/jackson/ResteasyJacksonProvider.class");
            System.out.println(url);
            Assert.assertNotNull(url);
        } finally {
            System.clearProperty("jboss.modules.settings.xml.url");
        }
    }
}
