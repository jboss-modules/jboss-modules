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

import static org.jboss.modules.Utils.MODULES_DIR;
import static org.jboss.modules.Utils.MODULE_FILE;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.modules.xml.ModuleXmlParser;

/**
 * A module loader which loads modules which are stored inside the {@code modules} directory of a single JAR or JAR-like
 * root.
 */
public final class ResourceLoaderModuleFinder implements ModuleFinder {
    private final ResourceLoader resourceLoader;
    private final String modulesDirectory;
    private final NestedResourceRootFactory factory;

    /**
     * Construct a new instance.  The given resource loader should support the nested {@link ResourceLoader#createSubloader(String, String)}
     * operation if {@code &lt;resource-root&gt;} elements are to be supported in {@code module.xml} files found within
     * the loader.
     *
     * @param resourceLoader the base resource loader to use (must not be {@code null})
     * @param modulesDirectory the modules directory to use (must not be {@code null})
     */
    public ResourceLoaderModuleFinder(final ResourceLoader resourceLoader, final String modulesDirectory) {
        factory = new NestedResourceRootFactory(resourceLoader);
        this.resourceLoader = resourceLoader;
        this.modulesDirectory = modulesDirectory;
    }

    /**
     * Construct a new instance.  The given resource loader should support the nested {@link ResourceLoader#createSubloader(String, String)}
     * operation if {@code &lt;resource-root&gt;} elements are to be supported in {@code module.xml} files found within
     * the loader.
     *
     * @param resourceLoader the base resource loader to use (must not be {@code null})
     */
    public ResourceLoaderModuleFinder(final ResourceLoader resourceLoader) {
        this(resourceLoader, MODULES_DIR);
    }

    public ModuleSpec findModule(final String name, final ModuleLoader delegateLoader) throws ModuleLoadException {
        final ResourceLoader resourceLoader = this.resourceLoader;
        String basePath = modulesDirectory + "/" + toPathString(name);
        Resource moduleXmlResource = resourceLoader.getResource(basePath + "/" + MODULE_FILE);
        if (moduleXmlResource == null) {
            basePath = modulesDirectory + "/" + toLegacyPathString(name);
            moduleXmlResource = resourceLoader.getResource(basePath + "/" + MODULE_FILE);
            if (moduleXmlResource == null) {
                return null;
            }
        }
        ModuleSpec moduleSpec;
        try {
            try (final InputStream inputStream = moduleXmlResource.openStream()) {
                moduleSpec = ModuleXmlParser.parseModuleXml(factory, basePath, inputStream, moduleXmlResource.getName(), delegateLoader, name);
            }
        } catch (IOException e) {
            throw new ModuleLoadException("Failed to read " + MODULE_FILE + " file", e);
        }
        return moduleSpec;
    }

    private static String toPathString(final String moduleName) {
        return moduleName.replace('.', '/');
    }

    @SuppressWarnings("deprecation")
    private static String toLegacyPathString(final String moduleName) {
        final ModuleIdentifier moduleIdentifier = ModuleIdentifier.fromString(moduleName);
        return moduleIdentifier.getName().replace('.', '/') + '/' + moduleIdentifier.getSlot();
    }

    static class NestedResourceRootFactory implements ModuleXmlParser.ResourceRootFactory {
        private final ResourceLoader resourceLoader;

        NestedResourceRootFactory(final ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }

        public ResourceLoader createResourceLoader(final String rootPath, final String loaderPath, final String loaderName) throws IOException {
            final ResourceLoader subloader = resourceLoader.createSubloader(loaderPath, loaderName);
            if (subloader == null) {
                throw new IllegalArgumentException("Nested resource loaders not supported by " + resourceLoader);
            }
            return subloader;
        }
    }
}
