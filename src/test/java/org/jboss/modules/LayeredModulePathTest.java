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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests of {@link LocalModuleLoader} when "layers" and "add-ons" are configured.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class LayeredModulePathTest extends AbstractModuleTestCase {

    private static final String PATH = "test/layeredmodulepath/";

    private static final ModuleIdentifier SHARED = ModuleIdentifier.create("test.shared");

    private String originalModulePath;

    private File reposRoot;
    private File repoA;
    private File repoB;

    @Before
    public void setUp() throws Exception {

        originalModulePath = System.getProperty("module.path");

        reposRoot = new File(getResource(PATH), "repos");
        if (!reposRoot.mkdirs() && !reposRoot.isDirectory()) {
            throw new IllegalStateException("Cannot create reposRoot");
        }
        repoA = new File(reposRoot, "root-a");
        if (!repoA.mkdirs() && !repoA.isDirectory()) {
            throw new IllegalStateException("Cannot create reposA");
        }
        repoB = new File(reposRoot, "root-b");
        if (!repoB.mkdirs() && !repoB.isDirectory()) {
            throw new IllegalStateException("Cannot create reposB");
        }
    }

    @After
    public void tearDown() throws Exception {
        if (reposRoot != null) {
            cleanFile(reposRoot);
        }

        if (originalModulePath != null) {
            System.setProperty("module.path", originalModulePath);
        } else {
            System.clearProperty("module.path");
        }
    }

    private void cleanFile(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                cleanFile(child);
            }
        }
        if (!file.delete() && file.exists()) {
            file.deleteOnExit();
        }
    }

    @Test
    public void testBaseLayer() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"));

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false, "base");

        validateModuleLoading(standardPath, false, false, false, "base");
    }

    @Test
    public void testSpecifiedBaseLayer() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", false, false, "base");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false, "base");

        validateModuleLoading(standardPath, false, false, false, "base");
    }

    @Test
    public void testSimpleOverlay() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"), "top");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false, "top", "base");

        validateModuleLoading(standardPath, false, false, false, "top", "base");
    }

    @Test
    public void testSpecifiedBaseLayerWithSimpleOverlay() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", false, false, "top", "base");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false, "top", "base");

        validateModuleLoading(standardPath, false, false, false, "top", "base");
    }

    @Test
    public void testMultipleOverlays() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"), "top", "mid");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false, "top", "mid", "base");

        validateModuleLoading(standardPath, false, false, false, "top", "mid", "base");
    }

    @Test
    public void testSpecifiedBaseLayerWithMultipleOverlays() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", false, false, "top", "mid", "base");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false, "top", "mid", "base");

        validateModuleLoading(standardPath, false, false, false, "top", "mid", "base");
    }

    @Test
    public void testBasePlusAddOns() throws Exception {
        createRepo("root-a", true, false, Collections.singletonList("base"));

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, true, "base");

        validateModuleLoading(standardPath, true, false, false, "base");
    }

    @Test
    public void testSpecifiedBasePlusAddOns() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", true, false, "base");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, true, "base");

        validateModuleLoading(standardPath, true, false, false, "base");
    }

    @Test
    public void testLayersAndAddOns() throws Exception {
        createRepo("root-a", true, false, Collections.singletonList("base"), "top", "mid");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, true, "top", "mid", "base");

        validateModuleLoading(standardPath, true, false, false, "top", "mid", "base");
    }

    @Test
    public void testSpecifiedBaseLayersAndAddOns() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", true, false, "top", "mid", "base");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, true, "top", "mid", "base");

        validateModuleLoading(standardPath, true, false, false, "top", "mid", "base");
    }

    @Test
    public void testBaseLayerAndUser() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"));

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false, "base");

        validateModuleLoading(standardPath, false, false, false, "base");
    }

    @Test
    public void testSpecifiedBaseLayerAndUser() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", false, true, "base");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false, "base");

        validateModuleLoading(standardPath, false, true, true, "base");
    }

    @Test
    public void testSingleRootComplete() throws Exception {
        createRepo("root-a", true, false, Collections.singletonList("base"), "top", "mid");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, true, "top", "mid", "base");

        validateModuleLoading(standardPath, true, false, false, "top", "mid", "base");
    }

    @Test
    public void testSpecifiedBaseSingleRootComplete() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", true, true, "top", "mid", "base");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, true, "top", "mid", "base");

        validateModuleLoading(standardPath, true, true, true, "top", "mid", "base");
    }

    @Test
    public void testSecondRepoHigherPrecedence() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"));
        createRepo("root-b", false, true);

        File[] standardPath = { repoB, repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 1, 0, false, "base");

        validateModuleLoading(standardPath, false, true, true, "base");
    }

    @Test
    public void testSecondRepoLowerPrecedence() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"));
        createRepo("root-b", false, true);

        File[] standardPath = { repoA, repoB };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, 2, false, "base");

        validateModuleLoading(standardPath, false, true, false, "base");
    }

    @Test
    public void testExtraneousOverlay() throws Exception {
        createRepo("root-a", false, false, Arrays.asList("base", "mid"), "top");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false, "top", "base");

        validateModuleLoading(standardPath, false, false, false, "top", "base");
    }

    @Test
    public void testSpecifiedBaseLayerWithExtraneousOverlay() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", false, false, Arrays.asList("mid"), "top", "base");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false, "top", "base");

        validateModuleLoading(standardPath, false, false, false, "top", "base");
    }

    /** Tests that setting up add-ons has no effect without the layers structure */
    @Test
    public void testLayersRequiredForAddOns() throws Exception {
        createRepo("root-a", true, false);

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false);

        validateModuleLoading(standardPath, false, false, false);

        // Now add the layers/base dir
        new File(repoA, "system/layers/base").mkdirs();

        modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, true, "base");

        validateModuleLoading(standardPath, true, false, false);
    }

    @Test
    public void testRejectConfWithNoStructure() throws Exception {
        createRepo("root-a", false, false);
        writeLayersConf("root-a", "top");

        File[] standardPath = { repoA };
        try {
            LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
            Assert.fail("layers.conf with no layers should fail");
        } catch (Exception good) {
            // good
        }
    }

    @Test
    public void testRejectConfWithMissingLayer() throws Exception {
        createRepo("root-a", false, false, Arrays.asList("top", "base"));
        writeLayersConf("root-a", "top", "mid");

        File[] standardPath = { repoA };
        try {
            LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
            Assert.fail("layers.conf with no layers should fail");
        } catch (Exception good) {
            // good
        }
    }

    @Test
    public void testEmptyLayersConf() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"));
        writeLayersConf("root-a");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);
        validateModulePath(modulePath, repoA, 0, -1, false, "base");

        validateModuleLoading(standardPath, false, false, false, "base");
    }

    @Test
    public void testLayersOverlayModulePath() throws Exception {
        createRepo("root-a", false, false, Arrays.asList("top", "base"));
        writeLayersConf("root-a", "top", "base");

        createOverlays(repoA, "top", false, "top1", "top2");
        createOverlays(repoA, "base", false, "base1", "base2");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);

        Assert.assertEquals(7, modulePath.length);
        Assert.assertEquals(repoA, modulePath[0]);
        Assert.assertEquals(new File(repoA, "system/layers/top/.overlays/top1"), modulePath[1]);
        Assert.assertEquals(new File(repoA, "system/layers/top/.overlays/top2"), modulePath[2]);
        Assert.assertEquals(new File(repoA, "system/layers/top"), modulePath[3]);
        Assert.assertEquals(new File(repoA, "system/layers/base/.overlays/base1"), modulePath[4]);
        Assert.assertEquals(new File(repoA, "system/layers/base/.overlays/base2"), modulePath[5]);
        Assert.assertEquals(new File(repoA, "system/layers/base"), modulePath[6]);

    }

    @Test
    public void testAddOnsOverlayModulePath() throws Exception {
        createRepo("root-a", true, false, Arrays.asList("base"));
        writeLayersConf("root-a", "base");

        createOverlays(repoA, "a", true, "a");
        createOverlays(repoA, "b", true, "b");

        File[] standardPath = { repoA };
        File[] modulePath = LayeredModulePathFactory.resolveLayeredModulePath(standardPath);

        Assert.assertEquals(6, modulePath.length);
        Assert.assertEquals(repoA, modulePath[0]);
        Assert.assertEquals(new File(repoA, "system/layers/base"), modulePath[1]);
        // The order of the add-ons is non deterministic
        Assert.assertEquals(".overlays", modulePath[2].getParentFile().getName());
        final String firstOverlay = modulePath[2].getName();
        Assert.assertEquals(new File(repoA, "system/add-ons/" + firstOverlay), modulePath[3]);
        Assert.assertEquals(".overlays", modulePath[4].getParentFile().getName());
        final String secondOverlays = modulePath[4].getName();
        Assert.assertEquals(new File(repoA, "system/add-ons/" + secondOverlays), modulePath[5]);

    }

    private void writeLayersConf(String rootName, String... layers) throws IOException {
        if (layers != null && layers.length > 0) {

            StringBuilder sb = new StringBuilder("layers=");
            for (int i = 0; i < layers.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(layers[i]);
            }

            File repo = "root-a".equals(rootName) ? repoA : repoB;
            File layersConf = new File(repo, "layers.conf");
            layersConf.createNewFile();
            FileWriter fw = new FileWriter(layersConf);
            try {
                PrintWriter pw = new PrintWriter(fw);
                pw.println(sb.toString());
                pw.close();
            } finally {
                try {
                    fw.close();
                } catch (Exception e) {
                    // meh
                }
            }
        }
    }

    private void createRepo(String rootName, boolean includeAddons, boolean includeUser, String... layers) throws Exception {
        List<String> empty = Collections.emptyList();
        createRepo(rootName, includeAddons, includeUser, empty, layers);
    }

    private void createRepo(String rootName, boolean includeAddons, boolean includeUser, List<String> extraLayers, String... layers) throws Exception {
        if (layers != null && layers.length > 0) {
            writeLayersConf(rootName, layers);
            for (String layer : layers) {
                createLayer(rootName, layer);
            }
        }
        if (extraLayers != null) {
            for (String extraLayer : extraLayers) {
                createLayer(rootName, extraLayer);
            }
        }

        if (includeAddons) {
            createAddOn(rootName, "a");
            createAddOn(rootName, "b");
        }

        if (includeUser) {
            createUserModules(rootName);
        }
    }

    private void createLayer(String rootName, String layerName) throws Exception {
        createModules("layers/" + layerName, rootName + "/system/layers/" + layerName, layerName);
    }

    private void createAddOn(String rootName, String addOnName) throws Exception {
        createModules("add-ons/" + addOnName, rootName + "/system/add-ons/" + addOnName, addOnName);
    }

    private void createUserModules(String rootName) throws Exception {
        createModules("user", rootName, "user");
    }

    private void createModules(String sourcePath, String relativeRepoPath, String uniqueName) throws Exception {
        copyResource(PATH + sourcePath + "/shared/module.xml", PATH, "repos/" + relativeRepoPath + "/test/shared/main");
        copyResource(PATH + sourcePath + "/unique/module.xml", PATH, "repos/" + relativeRepoPath + "/test/" + uniqueName + "/main");
    }

    private void validateModulePath(File[] modulePath, File repoRoot, int expectedStartPos,
                                    int expectedOtherRootPos, boolean expectAddons, String... layers) {
        int expectedLength = 1 + layers.length + (expectAddons ? 2 : 0);

        // Validate positional parameters -- check for bad test writers ;)
        if (expectedOtherRootPos < 0) {
            Assert.assertEquals(0, expectedStartPos); //
        } else if (expectedStartPos == 0) {
            Assert.assertEquals(expectedLength, expectedOtherRootPos);
        }

        if (expectedOtherRootPos < 1) {
            Assert.assertEquals("Correct module path length", expectedStartPos + expectedLength, modulePath.length);
        } else {
            Assert.assertTrue("Correct module path length", modulePath.length > expectedStartPos + expectedLength);
        }

        Assert.assertEquals(repoRoot, modulePath[expectedStartPos]);
        for (int i = 0; i < layers.length; i++) {
            File layer = new File(repoRoot, "system/layers/" + layers[i]);
            Assert.assertEquals(layer, modulePath[expectedStartPos + i + 1]);
        }
        if (expectAddons) {
            File addOnBase = new File(repoRoot, "system/add-ons");
            Set<String> valid = new HashSet<String>(Arrays.asList("a", "b"));
            for (int i = 0; i < 2; i++) {
                File addOn = modulePath[expectedStartPos + layers.length + i + 1];
                Assert.assertEquals(addOnBase, addOn.getParentFile());
                String addOnName = addOn.getName();
                Assert.assertTrue(addOnName, valid.remove(addOnName));
            }

        }

        if (expectedOtherRootPos == 0) {
            for (int i = 0; i < expectedStartPos; i++) {
                validateNotChild(modulePath[i], repoRoot);
            }
        } else if (expectedOtherRootPos > 0) {
            for (int i = expectedOtherRootPos; i < modulePath.length; i++) {
                validateNotChild(modulePath[i], repoRoot);
            }
        }

    }

    private void validateNotChild(File file, File repoRoot) {
        File stop = repoRoot.getParentFile();
        File testee = file;
        while (testee != null && !testee.equals(stop)) {
            Assert.assertFalse(testee.equals(repoRoot));
            testee = testee.getParentFile();
        }
    }

    private void validateModuleLoading(File[] standardPath, boolean expectAddOns, boolean expectUser,
                                       boolean expectUserPrecedence, String... layers) throws ModuleLoadException {
        // This is nasty, but the alternative is exposing the layers config stuff in the LocalModuleLoader API
        setUpModulePathProperty(standardPath);
        ModuleLoader moduleLoader = new LocalModuleLoader();

        // Validate the expected path produced the shared module
        if (expectUser || layers.length > 0 || expectAddOns) {
            Module shared = moduleLoader.loadModule(SHARED);
            String sharedProp = shared.getProperty("test.prop");
            if (expectUserPrecedence) {
                Assert.assertEquals("user", sharedProp);
            } else if (layers.length > 0) {
                Assert.assertEquals(layers[0], sharedProp);
            } else if (expectAddOns) {
                Assert.assertTrue("a".equals(sharedProp) || "b".equals(sharedProp));
            }
        }

        // Validate the expected unique modules are present
        Set<String> layersSet = new HashSet<String>(Arrays.asList(layers));
        loadModule(moduleLoader, "user", expectUser);
        loadModule(moduleLoader, "top", layersSet.contains("top"));
        loadModule(moduleLoader, "mid", layersSet.contains("mid"));
        loadModule(moduleLoader, "base", layersSet.contains("base"));
        loadModule(moduleLoader, "a", expectAddOns);
        loadModule(moduleLoader, "b", expectAddOns);
    }

    private void setUpModulePathProperty(File[] standardPath) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < standardPath.length; i++) {
            if (i > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(standardPath[i].getAbsolutePath());
        }
        System.setProperty("module.path", sb.toString());
    }

    private void loadModule(ModuleLoader moduleLoader, String moduleName, boolean expectAvailable) {
        ModuleIdentifier id = ModuleIdentifier.create("test." + moduleName);
        try {
            Module module = moduleLoader.loadModule(id);
            if (!expectAvailable) {
                Assert.fail("test." + moduleName + " should not be loadable");
            }
            String prop = module.getProperty("test.prop");
            Assert.assertEquals(moduleName, prop);
        } catch (ModuleLoadException e) {
            if (expectAvailable) {
                Assert.fail(e.getMessage());
            }
        }
    }

    private void createOverlays(final File root, final String name, boolean addOn, String... overlays) throws IOException {
        final File system = new File(root, "system");
        final File layers = addOn ? new File(system, "add-ons") : new File(system, "layers");
        final File repo = new File(layers, name);
        final File overlaysRoot = new File(repo, ".overlays");
        overlaysRoot.mkdir();
        final File overlaysConfig = new File(overlaysRoot, ".overlays");
        final OutputStream os = new FileOutputStream(overlaysConfig);
        try {
            for (final String overlay : overlays) {
                os.write(overlay.getBytes());
                os.write('\n');
            }
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
