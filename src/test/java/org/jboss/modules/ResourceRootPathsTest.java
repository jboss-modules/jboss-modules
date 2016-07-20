/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.modules;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test absolute and relative paths in resource-roots' path attribute (MODULES-218)
 * @author Martin Simka
 */
public class ResourceRootPathsTest extends AbstractModuleTestCase {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();
    private File repoRoot;
    private File testModuleRoot;
    private File fileResourceRoot;

    private static final String MODULE_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<module xmlns=\"urn:jboss:module:1.5\" name=\"test.test\">\n" +
            "    <resources>\n" +
            "        <resource-root name=\"relativejar\" path=\"relative.jar\" />\n" +
            "        <resource-root name=\"absolutejar\" path=\"@absolutejar@\" />\n" +
            "        <resource-root name=\"relativedir\" path=\"relativedir\" />\n" +
            "        <resource-root name=\"absolutedir\" path=\"@absolutedir@\" />\n" +
            "    </resources>\n" +
            "</module>";

    private ModuleLoader moduleLoader;

    @Before
    public void setupModuleLoader() throws Exception {
        repoRoot = tmpDir.newFolder("repo");
        testModuleRoot = new File(repoRoot, "test/test/main/");

        // Build a jar in module
        copyResource("org/jboss/modules/test/TestClass.class", "test/fileresourceloader", "org/jboss/modules/test");
        fileResourceRoot = getResource("test/fileresourceloader");
        final File jarInModule = new File(testModuleRoot, "relative.jar");
        jarInModule.getParentFile().mkdirs();
        JarResourceLoaderTest.buildJar(fileResourceRoot, jarInModule);

        // Build a jar outside of module
        final File jarOutsideModule = new File(repoRoot, "relative.jar");
        JarResourceLoaderTest.buildJar(fileResourceRoot, jarOutsideModule);

        // copy resource dir to module
        copyDir(fileResourceRoot, new File(testModuleRoot, "relativedir"));

        //create module.xml
        final File moduleXml = new File(testModuleRoot, "module.xml");
        String moduleXmlContent = MODULE_XML_TEMPLATE.replaceAll("@absolutejar@", jarOutsideModule.getCanonicalPath());
        moduleXmlContent = moduleXmlContent.replaceAll("@absolutedir@", fileResourceRoot.getCanonicalPath());
        Files.write(moduleXml.toPath(), moduleXmlContent.getBytes(), StandardOpenOption.CREATE_NEW);

        moduleLoader = new LocalModuleLoader(new File[] {repoRoot});
    }

    @Test
    public void testPaths() throws Exception {
        ConcreteModuleSpec moduleSpec = (ConcreteModuleSpec) moduleLoader.findModule(MODULE_ID);
        ResourceLoaderSpec[] resourceLoaders = moduleSpec.getResourceLoaders();
        int checkCount = 0;
        for (ResourceLoaderSpec r : resourceLoaders) {
            ResourceLoader resourceLoader = r.getResourceLoader();
            if (resourceLoader instanceof JarFileResourceLoader) {
                // validate jar with relative path
                if(resourceLoader.getRootName().equals("relativejar")) {
                    checkCount++;
                    final File relativeJar = getFileFromJarUri(resourceLoader.getLocation());
                    Assert.assertEquals(testModuleRoot, relativeJar.getParentFile());
                    continue;
                }

                // validate jar with absolute path
                if(resourceLoader.getRootName().equals("absolutejar")) {
                    checkCount++;
                    final File absoluteJar = getFileFromJarUri(resourceLoader.getLocation());
                    Assert.assertEquals(repoRoot, absoluteJar.getParentFile());
                    continue;
                }
            } else if (resourceLoader instanceof FileResourceLoader) {

                // validate dir with relative path
                if(resourceLoader.getRootName().equals("relativedir")) {
                    checkCount++;
                    final File relativeDir = new File(resourceLoader.getLocation());
                    Assert.assertEquals(testModuleRoot, relativeDir.getParentFile());
                    continue;
                }

                // validate dir with absolute path
                if(resourceLoader.getRootName().equals("absolutedir")) {
                    checkCount++;
                    final File absoluteDir = new File(resourceLoader.getLocation());
                    Assert.assertEquals(fileResourceRoot.getParentFile(), absoluteDir.getParentFile());
                    continue;
                }
            }
        }
        Assert.assertEquals("Test should have checked 4 ResourceLoaders", 4, checkCount);
    }

    private File getFileFromJarUri(URI uri) throws Exception {
        JarURLConnection connection = (JarURLConnection) uri.toURL().openConnection();
        File file = new File(connection.getJarFileURL().toURI());
        return file;
    }

    static void copyDir(File sourceDir, File targetDir) throws Exception {

        abstract class CopyFileVisitor implements FileVisitor<Path> {
            boolean isFirst = true;
            Path ptr;
        }

        CopyFileVisitor copyVisitor = new CopyFileVisitor() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!isFirst) {
                    Path target = ptr.resolve(dir.getName(dir.getNameCount() - 1));
                    ptr = target;
                }
                Files.copy(dir, ptr, StandardCopyOption.COPY_ATTRIBUTES);
                isFirst = false;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path target = ptr.resolve(file.getFileName());
                Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Path target = ptr.getParent();
                ptr = target;
                return FileVisitResult.CONTINUE;
            }
        };

        copyVisitor.ptr = targetDir.toPath();
        Files.walkFileTree(sourceDir.toPath(), copyVisitor);
    }
}
