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
import org.jboss.modules.xml.ModuleXmlParser;

import static org.jboss.modules.Utils.DEPENDENCIES;
import static org.jboss.modules.Utils.EXPORT;
import static org.jboss.modules.Utils.OPTIONAL;
import static org.jboss.modules.Utils.MODULES_DIR;
import static org.jboss.modules.Utils.MODULE_FILE;

/**
 * A module finder which uses a JAR file as a module repository.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @deprecated {@link FileSystemClassPathModuleFinder} and/or {@link ResourceLoaderModuleFinder} should be used instead for more
 *      complete functionality.
 */
@Deprecated
public final class JarModuleFinder implements ModuleFinder {

    private final String myName;
    private final JarFile jarFile;
    private final AccessControlContext context;

    /**
     * Construct a new instance.
     *
     * @param myIdentifier the identifier to use for the JAR itself
     * @param jarFile the JAR file to encapsulate
     */
    public JarModuleFinder(final ModuleIdentifier myIdentifier, final JarFile jarFile) {
        this(myIdentifier.toString(), jarFile);
    }

    /**
     * Construct a new instance.
     *
     * @param myName the name to use for the JAR itself
     * @param jarFile the JAR file to encapsulate
     */
    public JarModuleFinder(final String myName, final JarFile jarFile) {
        this.myName = myName;
        this.jarFile = jarFile;
        context = AccessController.getContext();
    }

    //Replace conditional with polymorphism refactoring
    public ModuleSpec findModule(final String name, final ModuleLoader delegateLoader) throws ModuleLoadException {
        if (name.equals(myName)) {
            return findRootModule(delegateLoader,name);
        } else {
            return findNestedModule(name, delegateLoader);
        }
    }
    
    private ModuleSpec findRootModule(final ModuleLoader delegateLoader,final String name) throws ModuleLoadException {
        // special root JAR module
        Manifest manifest;
        try {
            manifest = jarFile.getManifest();
        } catch (IOException e) {
            throw new ModuleLoadException("Failed to load MANIFEST from JAR", e);
        }
        ModuleSpec.Builder builder = ModuleSpec.build(name);
        Attributes mainAttributes = manifest.getMainAttributes();
        String mainClass = mainAttributes.getValue(Attributes.Name.MAIN_CLASS);
        if (mainClass != null) {
            builder.setMainClass(mainClass);
        }
        String classPath = mainAttributes.getValue(Attributes.Name.CLASS_PATH);
        String dependencies = mainAttributes.getValue(DEPENDENCIES);
        MultiplePathFilterBuilder pathFilterBuilder = PathFilters.multiplePathFilterBuilder(true);
        pathFilterBuilder.addFilter(PathFilters.is(MODULES_DIR), false);
        pathFilterBuilder.addFilter(PathFilters.isChildOf(MODULES_DIR), false);
        builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(new JarFileResourceLoader("", jarFile), pathFilterBuilder.create()));
        String[] classPathEntries = classPath == null ? Utils.NO_STRINGS : classPath.split("\\s+");
        for (String entry : classPathEntries) {
            if (! entry.isEmpty()) {
                addRootEntry(builder, entry);
            }
        }
        String[] dependencyEntries = dependencies == null ? Utils.NO_STRINGS : dependencies.split("\\s*,\\s*");
        for (String dependencyEntry : dependencyEntries) {
            addDependency(builder, dependencyEntry);
        }
        builder.addDependency(DependencySpec.createSystemDependencySpec(JDKPaths.JDK));
        builder.addDependency(DependencySpec.createLocalDependencySpec());
        return builder.create();
    }
    
    private void addRootEntry(final ModuleSpec.Builder builder, final String entry) {
        if (entry.startsWith("../") || entry.startsWith("./") || entry.startsWith("/") || entry.contains("/../")) {
            // invalid
            return;
        }
        File root;
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
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(new PathResourceLoader(entry, root.toPath(), context)));
        } else {
            // assume a JAR
            JarFile childJarFile;
            try {
                childJarFile = JDKSpecific.getJarFile(root, true);
            } catch (IOException e) {
                // ignore and continue
                return;
            }
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(new JarFileResourceLoader(entry, childJarFile)));
        }
    }
    
    private void addDependency(final ModuleSpec.Builder builder, String dependencyEntry) {
        boolean optional = false;
        boolean export = false;
        dependencyEntry = dependencyEntry.trim();
        if (! dependencyEntry.isEmpty()) {
            String[] fields = dependencyEntry.split("\\s+");
            if (fields.length < 1) {
                return;
            }
            String moduleName = fields[0];
            for (int i = 1; i < fields.length; i++) {
                String field = fields[i];
                if (field.equals(OPTIONAL)) {
                    optional = true;
                } else if (field.equals(EXPORT)) {
                    export = true;
                }
                // else ignored
            }
            builder.addDependency(new ModuleDependencySpecBuilder()
                .setName(moduleName)
                .setExport(export)
                .setOptional(optional)
                .build());
        }
    }
    
    private ModuleSpec findNestedModule(final String name, final ModuleLoader delegateLoader) throws ModuleLoadException {
        final String path = PathUtils.basicModuleNameToPath(name);
        if (path == null) {
            return null; // not valid, so not found
        }
        String basePath = MODULES_DIR + "/" + path;
        JarEntry moduleXmlEntry = jarFile.getJarEntry(basePath + "/" + MODULE_FILE);
        if (moduleXmlEntry == null) {
            return null;
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
}
