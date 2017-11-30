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

import static org.jboss.modules.Utils.DEPENDENCIES;
import static org.jboss.modules.Utils.EXPORT;
import static org.jboss.modules.Utils.MODULES_DIR;
import static org.jboss.modules.Utils.MODULE_VERSION;
import static org.jboss.modules.Utils.OPTIONAL;
import static org.jboss.modules.Utils.SERVICES;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.modules.filter.MultiplePathFilterBuilder;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.xml.PermissionsXmlParser;
import org.jboss.modules.xml.XmlPullParserException;

/**
 * A module finder which loads modules from individual JARs or directories on the file system, supporting the JAR specification headers
 * as well as the extended {@code MANIFEST} headers supported by JBoss Modules.  The JAR files or modules may in turn contain
 * nested module repositories inside of their {@code modules} subdirectories.  Modules in nested repositories are only visible
 * to the module that contains them.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class FileSystemClassPathModuleFinder implements ModuleFinder {

    static final ModuleLoader EMPTY_MODULE_LOADER = new ModuleLoader();
    static final SimpleSupplier<ModuleLoader> EMPTY_MODULE_LOADER_SUPPLIER = new SimpleSupplier<>(EMPTY_MODULE_LOADER);

    private final AccessControlContext context;
    private final Supplier<ModuleLoader> baseModuleLoaderSupplier;
    private final Supplier<ModuleLoader> extensionModuleLoaderSupplier;
    private static final PathFilter NO_MODULES_DIR;

    static {
        final MultiplePathFilterBuilder builder = PathFilters.multiplePathFilterBuilder(true);
        builder.addFilter(PathFilters.is(MODULES_DIR), false);
        builder.addFilter(PathFilters.isChildOf(MODULES_DIR), false);
        NO_MODULES_DIR = builder.create();
    }

    /**
     * Construct a new instance.
     *
     * @param baseModuleLoader the module loader to use to load module dependencies from (must not be {@code null})
     */
    public FileSystemClassPathModuleFinder(final ModuleLoader baseModuleLoader) {
        this(baseModuleLoader, EMPTY_MODULE_LOADER_SUPPLIER);
    }

    /**
     * Construct a new instance.
     *
     * @param baseModuleLoader the module loader to use to load module dependencies from (must not be {@code null})
     * @param extensionModuleLoaderSupplier a supplier which yields a module loader for loading extensions (must not be {@code null})
     */
    public FileSystemClassPathModuleFinder(final ModuleLoader baseModuleLoader, final Supplier<ModuleLoader> extensionModuleLoaderSupplier) {
        this(new SimpleSupplier<>(baseModuleLoader), extensionModuleLoaderSupplier);
        if (baseModuleLoader == null) throw new IllegalArgumentException("baseModuleLoader is null");
    }

    /**
     * Construct a new instance.
     *
     * @param baseModuleLoaderSupplier the supplier to supply a module loader for loading dependencies (must not be {@code null})
     * @param extensionModuleLoaderSupplier a supplier which yields a module loader for loading extensions (must not be {@code null})
     */
    public FileSystemClassPathModuleFinder(final Supplier<ModuleLoader> baseModuleLoaderSupplier, final Supplier<ModuleLoader> extensionModuleLoaderSupplier) {
        if (baseModuleLoaderSupplier == null) throw new IllegalArgumentException("baseModuleLoaderSupplier is null");
        this.baseModuleLoaderSupplier = baseModuleLoaderSupplier;
        if (extensionModuleLoaderSupplier == null) throw new IllegalArgumentException("extensionModuleLoaderSupplier is null");
        this.extensionModuleLoaderSupplier = extensionModuleLoaderSupplier;
        context = AccessController.getContext();
    }

    public ModuleSpec findModule(final String name, final ModuleLoader delegateLoader) throws ModuleLoadException {
        if (PathUtils.isRelative(name)) {
            return null;
        }
        final String canonName = PathUtils.canonicalize(name);
        if (! name.equals(canonName)) {
            return null;
        }
        final String fileName = PathUtils.fileNameOfPath(canonName);
        if (fileName.isEmpty()) {
            return null;
        }
        try {
            final Manifest manifest;
            final ModuleSpec.Builder builder = ModuleSpec.build(canonName);
            final Path path = Paths.get(canonName);
            final ResourceLoader resourceLoader;
            final ModuleLoader fatModuleLoader;
            final ModuleLoader baseModuleLoader = baseModuleLoaderSupplier.get();
            if (Files.isDirectory(path)) {
                manifest = new Manifest();
                final Path manifestPath = path.resolve("META-INF/MANIFEST.MF");
                if (Files.exists(manifestPath)) try {
                    try (InputStream stream = Files.newInputStream(manifestPath, StandardOpenOption.READ)) {
                        manifest.read(stream);
                    }
                } catch (NoSuchFileException | FileNotFoundException ignored) {
                } catch (IOException e) {
                    throw new ModuleLoadException("Failed to load MANIFEST from " + path, e);
                }
                resourceLoader = new FileResourceLoader(fileName, path.toFile(), context);
                fatModuleLoader = new DelegatingModuleLoader(baseModuleLoader, new LocalModuleFinder(new File[]{ path.resolve(MODULES_DIR).toFile() }));
            } else {
                // assume some kind of JAR file
                final JarFile jarFile = JDKSpecific.getJarFile(canonName, true);
                try {
                    try {
                        manifest = jarFile.getManifest();
                    } catch (IOException e) {
                        throw new ModuleLoadException("Failed to load MANIFEST from " + path, e);
                    }
                    resourceLoader = new JarFileResourceLoader(fileName, jarFile);
                } catch (Throwable t) {
                    try {
                        jarFile.close();
                    } catch (Throwable e2) {
                        e2.addSuppressed(t);
                        throw e2;
                    }
                    throw t;
                }
                fatModuleLoader = new DelegatingModuleLoader(baseModuleLoader, new ResourceLoaderModuleFinder(resourceLoader));
            }
            // now build the module specification from the manifest information
            try {
                addSelfContent(builder, resourceLoader);
                addSelfDependency(builder);
                final Attributes mainAttributes = manifest.getMainAttributes();
                setMainClass(builder, mainAttributes);
                addClassPathDependencies(builder, delegateLoader, path, mainAttributes);
                final ModuleLoader extensionModuleLoader = extensionModuleLoaderSupplier.get();
                addExtensionDependencies(builder, mainAttributes, extensionModuleLoader);
                addModuleDependencies(builder, fatModuleLoader, mainAttributes);
                setModuleVersion(builder, mainAttributes);
                addSystemDependencies(builder);
                addPermissions(builder, resourceLoader, delegateLoader);
            } catch (Throwable t) {
                resourceLoader.close();
                throw t;
            }
            return builder.create();
        } catch (IOException e) {
            throw new ModuleLoadException(e);
        }
    }

    void addPermissions(final ModuleSpec.Builder builder, final ResourceLoader resourceLoader, final ModuleLoader moduleLoader) {
        final Resource resource = resourceLoader.getResource("META-INF/permissions.xml");
        if (resource != null) {
            try {
                try (InputStream stream = resource.openStream()) {
                    builder.setPermissionCollection(PermissionsXmlParser.parsePermissionsXml(stream, moduleLoader, builder.getName()));
                }
            } catch (XmlPullParserException | IOException ignored) {
            }
        }
    }

    void addSystemDependencies(final ModuleSpec.Builder builder) {
        builder.addDependency(DependencySpec.createSystemDependencySpec(JDKPaths.JDK));
    }

    void addModuleDependencies(final ModuleSpec.Builder builder, final ModuleLoader fatModuleLoader, final Attributes mainAttributes) {
        final String dependencies = mainAttributes.getValue(DEPENDENCIES);
        final String[] dependencyEntries = dependencies == null ? Utils.NO_STRINGS : dependencies.split("\\s*,\\s*");
        for (String dependencyEntry : dependencyEntries) {
            boolean optional = false;
            boolean export = false;
            boolean services = false;
            dependencyEntry = dependencyEntry.trim();
            if (! dependencyEntry.isEmpty()) {
                String[] fields = dependencyEntry.split("\\s+");
                if (fields.length < 1) {
                    continue;
                }
                String moduleName = fields[0];
                for (int i = 1; i < fields.length; i++) {
                    String field = fields[i];
                    if (field.equals(OPTIONAL)) {
                        optional = true;
                    } else if (field.equals(EXPORT)) {
                        export = true;
                    } else if (field.equals(SERVICES)) {
                        services = true;
                    }
                    // else ignored
                }
                builder.addDependency(DependencySpec.createModuleDependencySpec(
                    services ? PathFilters.getDefaultImportFilterWithServices() : PathFilters.getDefaultImportFilter(),
                    export ? PathFilters.acceptAll() : PathFilters.rejectAll(),
                    fatModuleLoader,
                    moduleName,
                    optional
                ));
            }
        }
    }

    void setModuleVersion(final ModuleSpec.Builder builder, final Attributes mainAttributes) {
        final String versionString = mainAttributes.getValue(MODULE_VERSION);
        if (versionString != null) {
            builder.setVersion(Version.parse(versionString));
        }
    }

    void addExtensionDependencies(final ModuleSpec.Builder builder, final Attributes mainAttributes, final ModuleLoader extensionModuleLoader) {
        final String extensionList = mainAttributes.getValue(Attributes.Name.EXTENSION_LIST);
        final String[] extensionListEntries = extensionList == null ? Utils.NO_STRINGS : extensionList.split("\\s+");
        for (String entry : extensionListEntries) {
            if (! entry.isEmpty()) {
                builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.rejectAll(), extensionModuleLoader, entry, true));
            }
        }
    }

    void addClassPathDependencies(final ModuleSpec.Builder builder, final ModuleLoader moduleLoader, final Path path, final Attributes mainAttributes) {
        final String classPath = mainAttributes.getValue(Attributes.Name.CLASS_PATH);
        final String[] classPathEntries = classPath == null ? Utils.NO_STRINGS : classPath.split("\\s+");
        for (String entry : classPathEntries) {
            if (! entry.isEmpty()) {
                final URI uri;
                try {
                    uri = new URI(entry);
                } catch (URISyntaxException e) {
                    // ignore invalid class path entries
                    continue;
                }
                final Path depPath = path.resolveSibling(Paths.get(uri)).normalize();
                // simple dependency; class path deps are always optional
                builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.rejectAll(), moduleLoader, depPath.toString(), true));
            }
        }
    }

    void setMainClass(final ModuleSpec.Builder builder, final Attributes mainAttributes) {
        final String mainClass = mainAttributes.getValue(Attributes.Name.MAIN_CLASS);
        if (mainClass != null) {
            builder.setMainClass(mainClass);
        }
    }

    void addSelfDependency(final ModuleSpec.Builder builder) {
        // add our own dependency
        builder.addDependency(DependencySpec.createLocalDependencySpec());
    }

    void addSelfContent(final ModuleSpec.Builder builder, final ResourceLoader resourceLoader) {
        // add our own content
        builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(resourceLoader, NO_MODULES_DIR));
    }
}
