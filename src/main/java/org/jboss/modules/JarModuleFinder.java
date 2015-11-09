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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.jboss.modules.filter.MultiplePathFilterBuilder;
import org.jboss.modules.filter.PathFilters;

/**
 * A module finder which uses a JAR file as a module repository.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class JarModuleFinder implements ModuleFinder {
    private final ModuleIdentifier myIdentifier;
    private final JarFile jarFile;
    private final AccessControlContext context;

    /**
     * Construct a new instance.
     *
     * @param myIdentifier the identifier to use for the JAR itself
     * @param jarFile the JAR file to encapsulate
     */
    public JarModuleFinder(final ModuleIdentifier myIdentifier, final JarFile jarFile) {
        this.myIdentifier = myIdentifier;
        this.jarFile = jarFile;
        context = AccessController.getContext();
    }

    public ModuleSpec findModule(final ModuleIdentifier identifier, final ModuleLoader delegateLoader) throws ModuleLoadException {
        if (identifier.equals(myIdentifier)) {
            // special root JAR module
            Manifest manifest;
            try {
                manifest = jarFile.getManifest();
            } catch (IOException e) {
                throw new ModuleLoadException("Failed to load MANIFEST from JAR", e);
            }
            ModuleSpec.Builder builder = ModuleSpec.build(identifier);
            Attributes mainAttributes = manifest.getMainAttributes();
            String mainClass = mainAttributes.getValue(Attributes.Name.MAIN_CLASS);
            if (mainClass != null) {
                builder.setMainClass(mainClass);
            }
            String classPath = mainAttributes.getValue(Attributes.Name.CLASS_PATH);
            String dependencies = mainAttributes.getValue("Dependencies");
            MultiplePathFilterBuilder pathFilterBuilder = PathFilters.multiplePathFilterBuilder(true);
            pathFilterBuilder.addFilter(PathFilters.is("modules"), false);
            pathFilterBuilder.addFilter(PathFilters.isChildOf("modules"), false);
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(new JarFileResourceLoader("", jarFile), pathFilterBuilder.create()));
            String[] classPathEntries = classPath == null ? JarModuleLoader.NO_STRINGS : classPath.split("\\s+");
            for (String entry : classPathEntries) {
                if (! entry.isEmpty()) {
                    if (entry.startsWith("../") || entry.startsWith("./") || entry.startsWith("/") || entry.contains("/../")) {
                        // invalid
                        continue;
                    }
                    File root = null;
                    try {
                        File path = new File(new URI(entry));
                        if (path.isAbsolute()) {
                          root = path;
                        } else {
                          root = new File(jarFile.getName(), path.getPath());
                        }
                    } catch (URISyntaxException e) {
                        // invalid, will probably fail anyway
                        root = new File(jarFile.getName(), entry);
                    }

                    if (entry.endsWith("/")) {
                        // directory reference
                        FileResourceLoader resourceLoader = new FileResourceLoader(entry, root, context);
                        builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(resourceLoader));
                    } else {
                        // assume a JAR
                        JarFile childJarFile;
                        try {
                            childJarFile = new JarFile(root, true);
                        } catch (IOException e) {
                            // ignore and continue
                            continue;
                        }
                        builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(new JarFileResourceLoader(entry, childJarFile)));
                    }
                }
            }
            String[] dependencyEntries = dependencies == null ? JarModuleLoader.NO_STRINGS : dependencies.split("\\s*,\\s*");
            for (String dependencyEntry : dependencyEntries) {
                boolean optional = false;
                boolean export = false;
                dependencyEntry = dependencyEntry.trim();
                if (! dependencyEntry.isEmpty()) {
                    String[] fields = dependencyEntry.split("\\s+");
                    if (fields.length < 1) {
                        continue;
                    }
                    String moduleName = fields[0];
                    for (int i = 1; i < fields.length; i++) {
                        String field = fields[i];
                        if (field.equals("optional")) {
                            optional = true;
                        } else if (field.equals("export")) {
                            export = true;
                        }
                        // else ignored
                    }
                    builder.addDependency(DependencySpec.createModuleDependencySpec(ModuleIdentifier.fromString(moduleName), export, optional));
                }
            }
            builder.addDependency(DependencySpec.createSystemDependencySpec(JDKPaths.JDK));
            builder.addDependency(DependencySpec.createLocalDependencySpec());
            return builder.create();
        } else {
            String namePath = identifier.getName().replace('.', '/');
            String basePath = "modules/" + namePath + "/" + identifier.getSlot();
            JarEntry moduleXmlEntry = jarFile.getJarEntry(basePath + "/module.xml");
            if (moduleXmlEntry == null) {
                return null;
            }
            ModuleSpec moduleSpec;
            try {
                InputStream inputStream = jarFile.getInputStream(moduleXmlEntry);
                try {
                    moduleSpec = ModuleXmlParser.parseModuleXml(new ModuleXmlParser.ResourceRootFactory() {
                       public ResourceLoader createResourceLoader(final String rootPath, final String loaderPath, final String loaderName) throws IOException {
                            return new JarFileResourceLoader(loaderName, jarFile, loaderPath);
                        }
                    }, basePath, inputStream, moduleXmlEntry.getName(), delegateLoader, identifier);
                } finally {
                    StreamUtil.safeClose(inputStream);
                }
            } catch (IOException e) {
                throw new ModuleLoadException("Failed to read module.xml file", e);
            }
            return moduleSpec;
        }
    }
}
