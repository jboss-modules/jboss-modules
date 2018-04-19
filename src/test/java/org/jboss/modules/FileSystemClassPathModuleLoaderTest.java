/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

import org.jboss.modules.util.Util;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class FileSystemClassPathModuleLoaderTest {
    private FileSystemClassPathModuleFinder moduleFinder;
    private ModuleLoader moduleLoader;

    @Before
    public void setup() throws Exception {
        final File repoRoot = Util.getResourceFile(getClass(), "test/repo");
        ModuleLoader localModuleLoader = new LocalModuleLoader(new File[] {repoRoot});
        moduleFinder = new FileSystemClassPathModuleFinder(localModuleLoader);
        moduleLoader = new ModuleLoader(moduleFinder);
    }

    @Test
    public void testModuleLoadFromFileSystemModule() throws Exception {
        final File resourceFile = Util.getResourceFile(getClass(), "test/filesystem-module-1");
        final Module module = moduleLoader.loadModule(resourceFile.getAbsoluteFile().getCanonicalFile().toString());
        final URL exportedResource = module.getExportedResource("META-INF/services/javax.ws.rs.ext.Providers");
        Assert.assertNotNull(exportedResource);
    }
}
