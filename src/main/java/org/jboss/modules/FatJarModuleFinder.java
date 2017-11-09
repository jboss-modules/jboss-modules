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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.modules.xml.ModuleXmlParser;

/**
 * A module loader which loads modules which are stored inside the {@code modules} directory of a single JAR file.
 */
public final class FatJarModuleFinder implements ModuleFinder {
    private final JarFile jarFile;

    public FatJarModuleFinder(final JarFile jarFile) {
        this.jarFile = jarFile;
    }

    public ModuleSpec findModule(final String name, final ModuleLoader delegateLoader) throws ModuleLoadException {
        String basePath = MODULES_DIR + "/" + toPathString(name);
        JarEntry moduleXmlEntry = jarFile.getJarEntry(basePath + "/" + MODULE_FILE);
        if (moduleXmlEntry == null) {
            basePath = MODULES_DIR + "/" + toLegacyPathString(name);
            moduleXmlEntry = jarFile.getJarEntry(basePath + "/" + MODULE_FILE);
            if (moduleXmlEntry == null) {
                return null;
            }
        }
        ModuleSpec moduleSpec;
        try {
            try (final InputStream inputStream = jarFile.getInputStream(moduleXmlEntry)) {
                moduleSpec = ModuleXmlParser.parseModuleXml((rootPath, loaderPath, loaderName) -> new JarFileResourceLoader(loaderName, jarFile, loaderPath), basePath, inputStream, moduleXmlEntry.getName(), delegateLoader, name);
            }
        } catch (IOException e) {
            throw new ModuleLoadException("Failed to read " + MODULE_FILE + " file", e);
        }
        return moduleSpec;
    }

    private static String toPathString(final String moduleName) {
        return moduleName.replace('.', '/');
    }

    private static String toLegacyPathString(final String moduleName) {
        final ModuleIdentifier moduleIdentifier = ModuleIdentifier.fromString(moduleName);
        return moduleIdentifier.getName().replace('.', '/') + '/' + moduleIdentifier.getSlot();
    }
}
