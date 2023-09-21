/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

import org.jboss.modules.filter.PathFilter;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

/**
 * Test the functionality of the PathResourceLoader
 *
 * @author Sergei Egorov
 */
@RunWith(Parameterized.class)
public class PathResourceLoaderTest extends AbstractResourceLoaderTestCase {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TestMode.FOLDER},
                {TestMode.JAR}
        });
    }

    @Parameterized.Parameter
    public TestMode testMode;

    private Path resourceRoot;

    @Override
    protected ResourceLoader createLoader(PathFilter exportFilter) throws Exception {
        resourceRoot = testMode.getResourceRoot(this);

        return ResourceLoaders.createPathResourceLoader(resourceRoot);
    }

    @Override
    protected void assertResource(Resource resource, String fileName) throws IOException {
        final Path resourceFile = resourceRoot.resolve(fileName);

        Assert.assertEquals(Files.size(resourceFile), resource.getSize());
        Assert.assertEquals(resourceFile.toUri().toURL(), resource.getURL());
    }

    private enum TestMode {
        FOLDER,
        JAR {
            @Override
            Path getResourceRoot(PathResourceLoaderTest test) throws Exception {
                // Build a jar to match the fileresource loader
                final File outputFile = new File(test.getResource("test"), "jarresourceloader/test.jar");
                outputFile.getParentFile().mkdirs();
                JarResourceLoaderTest.buildJar(super.getResourceRoot(test).toFile(), outputFile);

                FileSystem fileSystem = FileSystems.newFileSystem(outputFile.toPath(), (ClassLoader) null);
                return fileSystem.getRootDirectories().iterator().next();
            }
        };

        Path getResourceRoot(PathResourceLoaderTest test) throws Exception {
            Path resourceRoot = test.getResource("test/fileresourceloader").toPath().toAbsolutePath();
            // Copy the classfile over
            test.copyResource("org/jboss/modules/test/TestClass.class", "test/fileresourceloader", "org/jboss/modules/test");

            return resourceRoot;
        }
    }
}
