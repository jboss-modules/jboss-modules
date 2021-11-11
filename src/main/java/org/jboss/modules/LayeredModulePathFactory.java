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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Provides a module path that includes entries for any "layer" and "add-on" directory structures found
 * within the regular items in the provided module path.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class LayeredModulePathFactory {

    /**
     * Inspects each element in the given {@code modulePath} to see if it includes a {@code layers.conf} file
     * and/or a standard directory structure with child directories {@code system/layers} and, optionally,
     * {@code system/add-ons}. If so, the layers identified in {@code layers.conf} are added to the module path
     *
     * @param modulePath the filesystem locations that make up the standard module path, each of which is to be
     *                   checked for the presence of layers and add-ons
     *
     * @return a new module path, including any layers and add-ons, if found
     */
    static File[] resolveLayeredModulePath(File... modulePath) {

        boolean foundLayers = false;
        List<File> layeredPath = new ArrayList<File>();
        for (File file : modulePath) {

            // Always add the root, as the user may place modules directly in it
            layeredPath.add(file);

            LayersConfig layersConfig = getLayersConfig(file);

            File layersDir = new File(file, layersConfig.getLayersPath());
            if (!layersDir.exists())  {
                if (layersConfig.isConfigured()) {
                    // Bad config from user
                    throw new IllegalStateException("No layers directory found at " + layersDir);
                }
                // else this isn't a root that has layers and add-ons
                continue;
            }

            boolean validLayers = true;
            List<File> layerFiles = new ArrayList<File>();
            for (String layerName : layersConfig.getLayers()) {
                File layer = new File(layersDir, layerName);
                if (!layer.exists()) {
                    if (layersConfig.isConfigured()) {
                        // Bad config from user
                        throw new IllegalStateException(String.format("Cannot find layer %s under directory %s", layerName, layersDir));
                    }
                    // else this isn't a standard layers and add-ons structure
                    validLayers = false;
                    break;
                }
                loadOverlays(layer, layerFiles);
            }
            if (validLayers) {
                foundLayers = true;
                layeredPath.addAll(layerFiles);
                // Now add-ons
                File[] addOns = new File(file, layersConfig.getAddOnsPath()).listFiles();
                if (addOns != null) {
                    for (File addOn : addOns) {
                        if (addOn.isDirectory()) {
                            loadOverlays(addOn, layeredPath);
                        }
                    }
                }
            }
        }

        return foundLayers ? layeredPath.toArray(new File[layeredPath.size()]) : modulePath;
    }

    private static LayersConfig getLayersConfig(File repoRoot) {
        File layersList = new File(repoRoot, "layers.conf");
        if (!layersList.exists()) {
            return new LayersConfig();
        }
        try (final Reader reader = new InputStreamReader(new FileInputStream(layersList), StandardCharsets.UTF_8)) {
            Properties props = new Properties();
            props.load(reader);
            return new LayersConfig(props);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class LayersConfig {

        private static final String DEFAULT_LAYERS_PATH = "system/layers";
        private static final String DEFAULT_ADD_ONS_PATH = "system/add-ons";

        private final boolean configured;
        private final String layersPath;
        private final String addOnsPath;
        private final List<String> layers;

        private LayersConfig() {
            configured = false;
            layersPath = DEFAULT_LAYERS_PATH;
            addOnsPath = DEFAULT_ADD_ONS_PATH;
            layers = Collections.singletonList("base");
        }

        private LayersConfig(Properties properties) {
            configured = true;
            // Possible future enhancement; probably better to use an xml file
//            layersPath = properties.getProperty("layers.path", DEFAULT_LAYERS_PATH);
//            addOnsPath = properties.getProperty("add-ons.path", DEFAULT_ADD_ONS_PATH);
//            boolean excludeBase = Boolean.valueOf(properties.getProperty("exclude.base.layer", "false"));
            layersPath = DEFAULT_LAYERS_PATH;
            addOnsPath = DEFAULT_ADD_ONS_PATH;
            boolean excludeBase = false;
            String layersProp = (String) properties.get("layers");
            if (layersProp == null || (layersProp = layersProp.trim()).length() == 0) {
                if (excludeBase) {
                    layers = Collections.emptyList();
                } else {
                    layers = Collections.singletonList("base");
                }
            } else {
                String[] layerNames = layersProp.split(",");
                layers = new ArrayList<String>();
                boolean hasBase = false;
                for (String layerName : layerNames) {
                    if ("base".equals(layerName)) {
                        hasBase = true;
                    }
                    layers.add(layerName);
                }
                if (!hasBase && !excludeBase) {
                    layers.add("base");
                }
            }
        }

        boolean isConfigured() {
            return configured;
        }


        String getLayersPath() {
            return layersPath;
        }

        String getAddOnsPath() {
            return addOnsPath;
        }

        List<String> getLayers() {
            return layers;
        }
    }

    private static final String OVERLAYS = ".overlays";

    /**
     * Load the overlays for each layer.
     *
     * @param layeringRoot the layer root
     * @param path the module path
     */
    static void loadOverlays(final File layeringRoot, final List<File> path) {

        final File overlays = new File(layeringRoot, OVERLAYS);
        if (overlays.exists()) {
            final File refs = new File(overlays, OVERLAYS);
            if (refs.exists()) {
                try {
                    for (final String overlay : readRefs(refs)) {
                        final File root = new File(overlays, overlay);
                        path.add(root);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        path.add(layeringRoot);
    }

    public static List<String> readRefs(final File file) throws IOException {
        if(! file.exists()) {
            return Collections.emptyList();
        }
        final InputStream is = new FileInputStream(file);
        try {
            return readRefs(is);
        } finally {
            if (is != null) try {
                is.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static List<String> readRefs(final InputStream is) throws IOException {
        final List<String> refs = new ArrayList<String>();
        final StringBuffer buffer = new StringBuffer();
        do {
            if(buffer.length() > 0) {
                final String ref = buffer.toString().trim();
                if(ref.length() > 0) {
                    refs.add(ref);
                }
            }
        } while(readLine(is, buffer));
        return refs;
    }

    static boolean readLine(InputStream is, StringBuffer buffer) throws IOException {
        buffer.setLength(0);
        int c;
        for(;;) {
            c = is.read();
            switch(c) {
                case '\t':
                case '\r':
                    break;
                case -1: return false;
                case '\n': return true;
                default: buffer.append((char) c);
            }
        }
    }

}
