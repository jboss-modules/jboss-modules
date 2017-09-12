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

package org.jboss.modules.xml;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.AllPermission;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jboss.modules.AssertionSetting;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Version;
import org.jboss.modules.maven.ArtifactCoordinates;
import org.jboss.modules.maven.MavenArtifactUtil;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.NativeLibraryResourceLoader;
import org.jboss.modules.PathUtils;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;
import org.jboss.modules.filter.MultiplePathFilterBuilder;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.maven.MavenResolver;
import org.jboss.modules.security.FactoryPermissionCollection;
import org.jboss.modules.security.ModularPermissionFactory;
import org.jboss.modules.security.PermissionFactory;

import static org.jboss.modules.xml.XmlPullParser.CDSECT;
import static org.jboss.modules.xml.XmlPullParser.COMMENT;
import static org.jboss.modules.xml.XmlPullParser.DOCDECL;
import static org.jboss.modules.xml.XmlPullParser.END_DOCUMENT;
import static org.jboss.modules.xml.XmlPullParser.END_TAG;
import static org.jboss.modules.xml.XmlPullParser.ENTITY_REF;
import static org.jboss.modules.xml.XmlPullParser.FEATURE_PROCESS_NAMESPACES;
import static org.jboss.modules.xml.XmlPullParser.IGNORABLE_WHITESPACE;
import static org.jboss.modules.xml.XmlPullParser.PROCESSING_INSTRUCTION;
import static org.jboss.modules.xml.XmlPullParser.START_DOCUMENT;
import static org.jboss.modules.xml.XmlPullParser.START_TAG;
import static org.jboss.modules.xml.XmlPullParser.TEXT;

/**
 * A fast, validating {@code module.xml} parser.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author thomas.diesler@jboss.com
 */
public final class ModuleXmlParser {

    /**
     * A factory for resource roots, based on a root path, loader path, and loader name.  Normally it is sufficient to
     * accept the default.
     */
    public interface ResourceRootFactory {
        ResourceLoader createResourceLoader(final String rootPath, final String loaderPath, final String loaderName) throws IOException;
    }

    private ModuleXmlParser() {
    }

    private static final String MODULE_1_0 = "urn:jboss:module:1.0";
    private static final String MODULE_1_1 = "urn:jboss:module:1.1";
    private static final String MODULE_1_2 = "urn:jboss:module:1.2";
    private static final String MODULE_1_3 = "urn:jboss:module:1.3";
    // there is no 1.4
    private static final String MODULE_1_5 = "urn:jboss:module:1.5";
    private static final String MODULE_1_6 = "urn:jboss:module:1.6";

    private static final String E_MODULE = "module";
    private static final String E_ARTIFACT = "artifact";
    private static final String E_NATIVE_ARTIFACT = "native-artifact";
    private static final String E_DEPENDENCIES = "dependencies";
    private static final String E_RESOURCES = "resources";
    private static final String E_MAIN_CLASS = "main-class";
    private static final String E_RESOURCE_ROOT = "resource-root";
    private static final String E_PATH = "path";
    private static final String E_EXPORTS = "exports";
    private static final String E_IMPORTS = "imports";
    private static final String E_INCLUDE = "include";
    private static final String E_EXCLUDE = "exclude";
    private static final String E_INCLUDE_SET = "include-set";
    private static final String E_EXCLUDE_SET = "exclude-set";
    private static final String E_FILTER = "filter";
    private static final String E_SYSTEM = "system";
    private static final String E_PATHS = "paths";
    private static final String E_MODULE_ALIAS = "module-alias";
    private static final String E_MODULE_ABSENT = "module-absent";
    private static final String E_PROPERTIES = "properties";
    private static final String E_PROPERTY = "property";
    private static final String E_PERMISSIONS = "permissions";
    private static final String E_GRANT = "grant";

    private static final String A_NAME = "name";
    private static final String A_SLOT = "slot";
    private static final String A_EXPORT = "export";
    private static final String A_SERVICES = "services";
    private static final String A_PATH = "path";
    private static final String A_OPTIONAL = "optional";
    private static final String A_TARGET_NAME = "target-name";
    private static final String A_TARGET_SLOT = "target-slot";
    private static final String A_VALUE = "value";
    private static final String A_PERMISSION = "permission";
    private static final String A_ACTIONS = "actions";
    private static final String A_VERSION = "version";

    private static final String D_NONE = "none";
    private static final String D_IMPORT = "import";
    private static final String D_EXPORT = "export";

    private static final List<String> LIST_A_NAME = Collections.singletonList(A_NAME);
    private static final List<String> LIST_A_PATH = Collections.singletonList(A_PATH);

    private static final List<String> LIST_A_NAME_A_SLOT = Arrays.asList(A_NAME, A_SLOT);
    private static final List<String> LIST_A_NAME_A_TARGET_NAME = Arrays.asList(A_NAME, A_TARGET_NAME);
    private static final List<String> LIST_A_PERMISSION_A_NAME = Arrays.asList(A_PERMISSION, A_NAME);

    /**
     * Parse a {@code module.xml} file.
     *
     * @param moduleLoader the module loader to use for dependency specifications
     * @param moduleIdentifier the module identifier of the module to load
     * @param root the module path root
     * @param moduleInfoFile the {@code File} of the {@code module.xml} content
     * @return a module specification
     * @throws ModuleLoadException if a dependency could not be established or another error occurs
     * @throws IOException if I/O fails
     * @deprecated Use {@link #parseModuleXml(ModuleLoader, String, File, File)} instead.
     */
    @Deprecated
    public static ModuleSpec parseModuleXml(final ModuleLoader moduleLoader, final ModuleIdentifier moduleIdentifier, final File root, final File moduleInfoFile) throws ModuleLoadException, IOException {
        return parseModuleXml(moduleLoader, moduleIdentifier.toString(), root, moduleInfoFile);
    }

    /**
     * Parse a {@code module.xml} file.
     *
     * @param moduleLoader the module loader to use for dependency specifications
     * @param moduleName the name of the module to load
     * @param root the module path root
     * @param moduleInfoFile the {@code File} of the {@code module.xml} content
     * @return a module specification
     * @throws ModuleLoadException if a dependency could not be established or another error occurs
     * @throws IOException if I/O fails
     */
    public static ModuleSpec parseModuleXml(final ModuleLoader moduleLoader, final String moduleName, final File root, final File moduleInfoFile) throws ModuleLoadException, IOException {
        final FileInputStream fis;
        try {
            fis = new FileInputStream(moduleInfoFile);
        } catch (FileNotFoundException e) {
            throw new ModuleLoadException("No module.xml file found at " + moduleInfoFile);
        }
        try {
            return parseModuleXml((rootPath, loaderPath, loaderName) -> {
                final File file;
                final File loaderFile;
                final String loaderFileName;
                if (File.separatorChar == '/') {
                    loaderFileName = loaderPath;
                } else {
                    loaderFileName = loaderPath.replace('/', File.separatorChar);
                }
                loaderFile = new File(loaderFileName);
                if (loaderFile.isAbsolute()) {
                    file = loaderFile;
                } else {
                    final String rootPathName;
                    if (File.separatorChar == '/') {
                        rootPathName = rootPath;
                    } else {
                        rootPathName = rootPath.replace('/', File.separatorChar);
                    }
                    file = new File(rootPathName, loaderFileName);
                }
                if (file.isDirectory()) {
                    return ResourceLoaders.createFileResourceLoader(loaderName, file);
                } else {
                    final Path jarFile = JDKSpecific.getJarFile(file, true);
                    return ResourceLoaders.createJarResourceLoader(loaderName, jarFile);
                }
            }, root.getPath(), new BufferedInputStream(fis), moduleInfoFile.getPath(), moduleLoader, moduleName);
        } finally {
            safeClose(fis);
        }
    }

    /**
     * Parse a {@code module.xml} file.
     *
     * @param factory the resource root factory to use (must not be {@code null})
     * @param rootPath the root path to send in to the resource root factory (must not be {@code null})
     * @param source a stream of the {@code module.xml} content (must not be {@code null})
     * @param moduleInfoFile the {@code File} of the {@code module.xml} content (must not be {@code null})
     * @param moduleLoader the module loader to use for dependency specifications (must not be {@code null})
     * @param moduleIdentifier the module identifier of the module to load
     * @return a module specification
     * @throws ModuleLoadException if a dependency could not be established or another error occurs
     * @throws IOException if I/O fails
     * @deprecated Use {@link #parseModuleXml(ResourceRootFactory, String, InputStream, String, ModuleLoader, String)} instead.
     */
    @Deprecated
    public static ModuleSpec parseModuleXml(final ResourceRootFactory factory, final String rootPath, InputStream source, final String moduleInfoFile, final ModuleLoader moduleLoader, final ModuleIdentifier moduleIdentifier) throws ModuleLoadException, IOException {
        return parseModuleXml(factory, MavenResolver.createDefaultResolver(), rootPath, source, moduleInfoFile, moduleLoader, moduleIdentifier);
    }

    /**
     * Parse a {@code module.xml} file.
     *
     * @param factory the resource root factory to use (must not be {@code null})
     * @param rootPath the root path to send in to the resource root factory (must not be {@code null})
     * @param source a stream of the {@code module.xml} content (must not be {@code null})
     * @param moduleInfoFile the {@code File} of the {@code module.xml} content (must not be {@code null})
     * @param moduleLoader the module loader to use for dependency specifications (must not be {@code null})
     * @param moduleName the module name of the module to load
     * @return a module specification
     * @throws ModuleLoadException if a dependency could not be established or another error occurs
     * @throws IOException if I/O fails
     */
    public static ModuleSpec parseModuleXml(final ResourceRootFactory factory, final String rootPath, InputStream source, final String moduleInfoFile, final ModuleLoader moduleLoader, final String moduleName) throws ModuleLoadException, IOException {
        return parseModuleXml(factory, MavenResolver.createDefaultResolver(), rootPath, source, moduleInfoFile, moduleLoader, moduleName);
    }

    /**
     * Parse a {@code module.xml} file.
     *
     * @param factory the resource root factory to use (must not be {@code null})
     * @param mavenResolver the Maven artifact resolver to use (must not be {@code null})
     * @param rootPath the root path to send in to the resource root factory (must not be {@code null})
     * @param source a stream of the {@code module.xml} content (must not be {@code null})
     * @param moduleInfoFile the {@code File} of the {@code module.xml} content (must not be {@code null})
     * @param moduleLoader the module loader to use for dependency specifications (must not be {@code null})
     * @param moduleIdentifier the module identifier of the module to load
     * @return a module specification
     * @throws ModuleLoadException if a dependency could not be established or another error occurs
     * @throws IOException if I/O fails
     * @deprecated Use {@link #parseModuleXml(ResourceRootFactory, MavenResolver, String, InputStream, String, ModuleLoader, String)} instead.
     */
    @Deprecated
    public static ModuleSpec parseModuleXml(final ResourceRootFactory factory, final MavenResolver mavenResolver, final String rootPath, InputStream source, final String moduleInfoFile, final ModuleLoader moduleLoader, final ModuleIdentifier moduleIdentifier) throws ModuleLoadException, IOException {
        return parseModuleXml(factory, mavenResolver, rootPath, source, moduleInfoFile, moduleLoader, moduleIdentifier.toString());
    }

    /**
     * Parse a {@code module.xml} file.
     *
     * @param factory the resource root factory to use (must not be {@code null})
     * @param mavenResolver the Maven artifact resolver to use (must not be {@code null})
     * @param rootPath the root path to send in to the resource root factory (must not be {@code null})
     * @param source a stream of the {@code module.xml} content (must not be {@code null})
     * @param moduleInfoFile the {@code File} of the {@code module.xml} content (must not be {@code null})
     * @param moduleLoader the module loader to use for dependency specifications (must not be {@code null})
     * @param moduleName the module name of the module to load
     * @return a module specification
     * @throws ModuleLoadException if a dependency could not be established or another error occurs
     * @throws IOException if I/O fails
     */
    public static ModuleSpec parseModuleXml(final ResourceRootFactory factory, final MavenResolver mavenResolver, final String rootPath, InputStream source, final String moduleInfoFile, final ModuleLoader moduleLoader, final String moduleName) throws ModuleLoadException, IOException {
        try {
            final MXParser parser = new MXParser();
            parser.setFeature(FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(source, null);
            return parseDocument(mavenResolver, factory, rootPath, parser, moduleLoader, moduleName);
        } catch (XmlPullParserException e) {
            throw new ModuleLoadException("Error loading module from " + moduleInfoFile, e);
        } finally {
            safeClose(source);
        }
    }

    public static XmlPullParserException unexpectedContent(final XmlPullParser reader) {
        final String kind;
        switch (reader.getEventType()) {
            case CDSECT: kind = "cdata"; break;
            case COMMENT: kind = "comment"; break;
            case DOCDECL: kind = "document decl"; break;
            case END_DOCUMENT: kind = "document end"; break;
            case END_TAG: kind = "element end"; break;
            case ENTITY_REF: kind = "entity ref"; break;
            case PROCESSING_INSTRUCTION: kind = "processing instruction"; break;
            case IGNORABLE_WHITESPACE: kind = "whitespace"; break;
            case START_DOCUMENT: kind = "document start"; break;
            case START_TAG: kind = "element start"; break;
            case TEXT: kind = "text"; break;
            default: kind = "unknown"; break;
        }
        final StringBuilder b = new StringBuilder("Unexpected content of type '").append(kind).append('\'');
        if (reader.getName() != null) {
            b.append(" named '").append(reader.getName()).append('\'');
        }
        if (reader.getText() != null) {
            b.append(", text is: '").append(reader.getText()).append('\'');
        }
        return new XmlPullParserException(b.toString(), reader, null);
    }

    public static XmlPullParserException endOfDocument(final XmlPullParser reader) {
        return new XmlPullParserException("Unexpected end of document", reader, null);
    }

    private static XmlPullParserException invalidModuleName(final XmlPullParser reader, final String expected) {
        return new XmlPullParserException("Invalid/mismatched module name (expected " + expected + ")", reader, null);
    }

    private static XmlPullParserException missingAttributes(final XmlPullParser reader, final Set<String> required) {
        final StringBuilder b = new StringBuilder("Missing one or more required attributes:");
        for (String attribute : required) {
            b.append(' ').append(attribute);
        }
        return new XmlPullParserException(b.toString(), reader, null);
    }

    private static XmlPullParserException unknownAttribute(final XmlPullParser parser, final int index) {
        final String namespace = parser.getAttributeNamespace(index);
        final String prefix = parser.getAttributePrefix(index);
        final String name = parser.getAttributeName(index);
        final StringBuilder eb = new StringBuilder("Unknown attribute \"");
        if (prefix != null) eb.append(prefix).append(':');
        eb.append(name);
        if (namespace != null) eb.append("\" from namespace \"").append(namespace);
        eb.append('"');
        return new XmlPullParserException(eb.toString(), parser, null);
    }

    private static XmlPullParserException unknownAttributeValue(final XmlPullParser parser, final int index) {
        final String namespace = parser.getAttributeNamespace(index);
        final String prefix = parser.getAttributePrefix(index);
        final String name = parser.getAttributeName(index);
        final StringBuilder eb = new StringBuilder("Unknown value \"");
        eb.append(parser.getAttributeValue(index));
        eb.append("\" for attribute \"");
        if (prefix != null && ! prefix.isEmpty()) eb.append(prefix).append(':');
        eb.append(name);
        if (namespace != null && ! namespace.isEmpty()) eb.append("\" from namespace \"").append(namespace);
        eb.append('"');
        return new XmlPullParserException(eb.toString(), parser, null);
    }

    private static void validateNamespace(final XmlPullParser reader) throws XmlPullParserException {
        switch (reader.getNamespace()) {
            case MODULE_1_0:
            case MODULE_1_1:
            case MODULE_1_2:
            case MODULE_1_3:
            case MODULE_1_5:
            case MODULE_1_6:
                break;
            default: throw unexpectedContent(reader);
        }
    }

    private static boolean atLeast1_6(final XmlPullParser reader) {
            return MODULE_1_6.equals(reader.getNamespace());
    }

    private static void assertNoAttributes(final XmlPullParser reader) throws XmlPullParserException {
        final int attributeCount = reader.getAttributeCount();
        if (attributeCount > 0) {
            throw unknownAttribute(reader, 0);
        }
    }

    private static void validateAttributeNamespace(final XmlPullParser reader, final int index) throws XmlPullParserException {
        if (! reader.getAttributeNamespace(index).isEmpty()) {
            throw unknownAttribute(reader, index);
        }
    }

    private static ModuleSpec parseDocument(final MavenResolver mavenResolver, final ResourceRootFactory factory, final String rootPath, XmlPullParser reader, final ModuleLoader moduleLoader, final String moduleName) throws XmlPullParserException, IOException {
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case START_DOCUMENT: {
                    return parseRootElement(mavenResolver, factory, rootPath, reader, moduleLoader, moduleName);
                }
                case START_TAG: {
                    validateNamespace(reader);
                    final String element = reader.getName();
                    switch (element) {
                        case E_MODULE: {
                            final ModuleSpec.Builder specBuilder = ModuleSpec.build(moduleName);
                            parseModuleContents(mavenResolver, reader, factory, moduleLoader, moduleName, specBuilder, rootPath);
                            parseEndDocument(reader);
                            return specBuilder.create();
                        }
                        case E_MODULE_ALIAS: {
                            final ModuleSpec moduleSpec = parseModuleAliasContents(reader, moduleName);
                            parseEndDocument(reader);
                            return moduleSpec;
                        }
                        case E_MODULE_ABSENT: {
                            parseModuleAbsentContents(reader, moduleName);
                            return null;
                        }
                        default: {
                            throw unexpectedContent(reader);
                        }
                    }
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static ModuleSpec parseRootElement(final MavenResolver mavenResolver, final ResourceRootFactory factory, final String rootPath, final XmlPullParser reader, final ModuleLoader moduleLoader, final String moduleName) throws XmlPullParserException, IOException {
        assertNoAttributes(reader);
        int eventType;
        while ((eventType = reader.nextTag()) != END_DOCUMENT) {
            switch (eventType) {
                case START_TAG: {
                    validateNamespace(reader);
                    final String element = reader.getName();
                    switch (element) {
                        case E_MODULE: {
                            final ModuleSpec.Builder specBuilder = ModuleSpec.build(moduleName);
                            parseModuleContents(mavenResolver, reader, factory, moduleLoader, moduleName, specBuilder, rootPath);
                            parseEndDocument(reader);
                            return specBuilder.create();
                        }
                        case E_MODULE_ALIAS: {
                            final ModuleSpec moduleSpec = parseModuleAliasContents(reader, moduleName);
                            parseEndDocument(reader);
                            return moduleSpec;
                        }
                        case E_MODULE_ABSENT: {
                            parseModuleAbsentContents(reader, moduleName);
                            return null;
                        }
                        default: {
                            throw unexpectedContent(reader);
                        }
                    }
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader);
    }

    private static ModuleSpec parseModuleAliasContents(final XmlPullParser reader, final String moduleName) throws XmlPullParserException, IOException {
        final int count = reader.getAttributeCount();
        String name = null;
        String slot = null;
        String targetName = null;
        String targetSlot = null;
        boolean noSlots = atLeast1_6(reader);
        final Set<String> required = new HashSet<>(LIST_A_NAME_A_TARGET_NAME);
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_NAME:    name = reader.getAttributeValue(i); break;
                case A_SLOT:    if (noSlots) throw unknownAttribute(reader,i); else slot = reader.getAttributeValue(i); break;
                case A_TARGET_NAME: targetName = reader.getAttributeValue(i); break;
                case A_TARGET_SLOT: if (noSlots) throw unknownAttribute(reader,i); else targetSlot = reader.getAttributeValue(i); break;
                default: throw unknownAttribute(reader, i);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }
        if (noSlots) {
            if (! moduleName.equals(name)) {
                throw invalidModuleName(reader, moduleName);
            }
        } else {
            if (! ModuleIdentifier.fromString(moduleName).equals(ModuleIdentifier.create(name, slot))) {
                throw invalidModuleName(reader, moduleName);
            }
        }
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    if (noSlots) {
                        return ModuleSpec.buildAlias(moduleName, targetName).create();
                    } else {
                        return ModuleSpec.buildAlias(ModuleIdentifier.fromString(moduleName), ModuleIdentifier.create(targetName, targetSlot)).create();
                    }
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseModuleAbsentContents(final XmlPullParser reader, final String moduleName) throws XmlPullParserException, IOException {
        final int count = reader.getAttributeCount();
        String name = null;
        String slot = null;
        boolean noSlots = atLeast1_6(reader);
        final Set<String> required = new HashSet<>(LIST_A_NAME_A_SLOT);
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_NAME:    name = reader.getAttributeValue(i); break;
                case A_SLOT:    if (noSlots) throw unknownAttribute(reader, i); else slot = reader.getAttributeValue(i); break;
                default: throw unknownAttribute(reader, i);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }
        if (noSlots) {
            if (! name.equals(moduleName)) {
                throw invalidModuleName(reader, moduleName);
            }
        } else {
            if (! ModuleIdentifier.fromString(moduleName).equals(ModuleIdentifier.create(name, slot))) {
                throw invalidModuleName(reader, moduleName);
            }
        }
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    return;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static final AllPermission ALL_PERMISSION = new AllPermission();

    static final Permissions DEFAULT_PERMISSION_COLLECTION = getAllPermission();

    private static Permissions getAllPermission() {
        final Permissions permissions = new Permissions();
        permissions.add(ALL_PERMISSION);
        return permissions;
    }

    private static void parseModuleContents(final MavenResolver mavenResolver, final XmlPullParser reader, final ResourceRootFactory factory, final ModuleLoader moduleLoader, final String moduleName, final ModuleSpec.Builder specBuilder, final String rootPath) throws XmlPullParserException, IOException {
        final int count = reader.getAttributeCount();
        String name = null;
        String slot = null;
        boolean noSlots = atLeast1_6(reader);
        Version version = null;
        final Set<String> required = noSlots ? new HashSet<>(LIST_A_NAME) : new HashSet<>(LIST_A_NAME);
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_NAME:    name = reader.getAttributeValue(i); break;
                case A_SLOT:    if (noSlots) throw unknownAttribute(reader, i); else slot = reader.getAttributeValue(i); break;
                case A_VERSION:
                    try {
                        version = Version.parse(reader.getAttributeValue(i));
                        break;
                    } catch (IllegalArgumentException ex) {
                        throw new XmlPullParserException(ex.getMessage(), reader, ex);
                    }
                default: throw unknownAttribute(reader, i);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }
        if (noSlots) {
            if (! specBuilder.getName().equals(name)) {
                throw invalidModuleName(reader, specBuilder.getName());
            }
        } else {
            if (! specBuilder.getIdentifier().equals(ModuleIdentifier.create(name, slot))) {
                throw invalidModuleName(reader, specBuilder.getIdentifier().toString());
            }
        }
        specBuilder.setVersion(version);
        // xsd:all
        MultiplePathFilterBuilder exportsBuilder = PathFilters.multiplePathFilterBuilder(true);
        ArrayList<DependencySpec> dependencies = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        int eventType;
        boolean gotPerms = false;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    specBuilder.addDependency(DependencySpec.createLocalDependencySpec(PathFilters.acceptAll(), exportsBuilder.create()));
                    for (DependencySpec dependency : dependencies) {
                        specBuilder.addDependency(dependency);
                    }
                    if (! gotPerms) specBuilder.setPermissionCollection(DEFAULT_PERMISSION_COLLECTION);
                    return;
                }
                case START_TAG: {
                    validateNamespace(reader);
                    final String element = reader.getName();
                    if (visited.contains(element)) {
                        throw unexpectedContent(reader);
                    }
                    visited.add(element);
                    switch (element) {
                        case E_EXPORTS:      parseFilterList(reader, exportsBuilder); break;
                        case E_DEPENDENCIES: parseDependencies(reader, dependencies); break;
                        case E_MAIN_CLASS:   parseMainClass(reader, specBuilder); break;
                        case E_RESOURCES:    parseResources(mavenResolver, factory, rootPath, reader, specBuilder); break;
                        case E_PROPERTIES:   parseProperties(reader, specBuilder); break;
                        case E_PERMISSIONS:  parsePermissions(reader, moduleLoader, moduleName, specBuilder); gotPerms = true; break;
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseDependencies(final XmlPullParser reader, final ArrayList<DependencySpec> dependencies) throws XmlPullParserException, IOException {
        assertNoAttributes(reader);
        // xsd:choice
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    return;
                }
                case START_TAG: {
                    validateNamespace(reader);
                    switch (reader.getName()) {
                        case E_MODULE: parseModuleDependency(reader, dependencies); break;
                        case E_SYSTEM: parseSystemDependency(reader, dependencies); break;
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseModuleDependency(final XmlPullParser reader, final ArrayList<DependencySpec> dependencies) throws XmlPullParserException, IOException {
        String name = null;
        String slot = null;
        boolean export = false;
        boolean optional = false;
        boolean noSlots = atLeast1_6(reader);
        String services = D_NONE;
        final Set<String> required = new HashSet<>(LIST_A_NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_NAME:     name = reader.getAttributeValue(i); break;
                case A_SLOT:     if (noSlots) throw unknownAttribute(reader, i); else slot = reader.getAttributeValue(i); break;
                case A_EXPORT:   export = Boolean.parseBoolean(reader.getAttributeValue(i)); break;
                case A_OPTIONAL: optional = Boolean.parseBoolean(reader.getAttributeValue(i)); break;
                case A_SERVICES: {
                    services = reader.getAttributeValue(i);
                    switch (services) {
                        case D_NONE:
                        case D_IMPORT:
                        case D_EXPORT:
                            break;
                        default: throw unknownAttributeValue(reader, i);
                    }
                    break;
                }
                default: throw unknownAttribute(reader, i);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }
        final MultiplePathFilterBuilder importBuilder = PathFilters.multiplePathFilterBuilder(true);
        final MultiplePathFilterBuilder exportBuilder = PathFilters.multiplePathFilterBuilder(export);
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    assert services.equals(D_NONE) || services.equals(D_EXPORT) || services.equals(D_IMPORT);
                    if (services.equals(D_EXPORT)) {
                        // If services are to be re-exported, add META-INF/services -> true near the end of the list
                        exportBuilder.addFilter(PathFilters.getMetaInfServicesFilter(), true);
                    }
                    if (export) {
                        // If re-exported, add META-INF/** -> false at the end of the list (require explicit override)
                        exportBuilder.addFilter(PathFilters.getMetaInfSubdirectoriesFilter(), false);
                        exportBuilder.addFilter(PathFilters.getMetaInfFilter(), false);
                    }
                    final PathFilter exportFilter = exportBuilder.create();
                    final PathFilter importFilter;
                    if (importBuilder.isEmpty()) {
                        importFilter = services.equals(D_NONE) ? PathFilters.getDefaultImportFilter() : PathFilters.getDefaultImportFilterWithServices();
                    } else {
                        if (! services.equals(D_NONE)) {
                            importBuilder.addFilter(PathFilters.getMetaInfServicesFilter(), true);
                        }
                        importBuilder.addFilter(PathFilters.getMetaInfSubdirectoriesFilter(), false);
                        importBuilder.addFilter(PathFilters.getMetaInfFilter(), false);
                        importFilter = importBuilder.create();
                    }
                    if (noSlots) {
                        dependencies.add(DependencySpec.createModuleDependencySpec(importFilter, exportFilter, null, name, optional));
                    } else {
                        dependencies.add(DependencySpec.createModuleDependencySpec(importFilter, exportFilter, null, ModuleIdentifier.create(name, slot), optional));
                    }
                    return;
                }
                case START_TAG: {
                    validateNamespace(reader);
                    switch (reader.getName()) {
                        case E_EXPORTS: parseFilterList(reader, exportBuilder); break;
                        case E_IMPORTS: parseFilterList(reader, importBuilder); break;
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseSystemDependency(final XmlPullParser reader, final ArrayList<DependencySpec> dependencies) throws XmlPullParserException, IOException {
        boolean export = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            switch (attribute) {
                case A_EXPORT:  export = Boolean.parseBoolean(reader.getAttributeValue(i)); break;
                default: throw unexpectedContent(reader);
            }
        }
        Set<String> paths = Collections.emptySet();
        final MultiplePathFilterBuilder exportBuilder = PathFilters.multiplePathFilterBuilder(export);
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    final PathFilter exportFilter = exportBuilder.create();
                    dependencies.add(DependencySpec.createSystemDependencySpec(PathFilters.acceptAll(), exportFilter, paths));
                    return;
                }
                case START_TAG: {
                    validateNamespace(reader);
                    switch (reader.getName()) {
                        case E_PATHS: {
                            paths = parseSet(reader);
                            break;
                        }
                        case E_EXPORTS: {
                            parseFilterList(reader, exportBuilder);
                            break;
                        }
                        default: {
                            throw unexpectedContent(reader);
                        }
                    }
                }
            }
        }
    }

    private static void parseMainClass(final XmlPullParser reader, final ModuleSpec.Builder specBuilder) throws XmlPullParserException, IOException {
        String name = null;
        final Set<String> required = new HashSet<>(LIST_A_NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_NAME: name = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }
        specBuilder.setMainClass(name);
        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parseResources(final MavenResolver mavenResolver, final ResourceRootFactory factory, final String rootPath, final XmlPullParser reader, final ModuleSpec.Builder specBuilder) throws XmlPullParserException, IOException {
        assertNoAttributes(reader);
        // xsd:choice
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    specBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(new NativeLibraryResourceLoader(new File(rootPath, "lib")), PathFilters.rejectAll()));
                    return;
                }
                case START_TAG: {
                    validateNamespace(reader);
                    switch (reader.getName()) {
                        case E_RESOURCE_ROOT: {
                            parseResourceRoot(factory, rootPath, reader, specBuilder);
                            break;
                        }
                        case E_ARTIFACT: {
                            parseArtifact(mavenResolver, reader, specBuilder);
                            break;
                        }
                        case E_NATIVE_ARTIFACT: {
                            parseNativeArtifact(mavenResolver, reader, specBuilder);
                            break;
                        }
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void createMavenNativeArtifactLoader(final MavenResolver mavenResolver, final String name, final XmlPullParser reader, final ModuleSpec.Builder specBuilder) throws IOException, XmlPullParserException {
        File fp = mavenResolver.resolveJarArtifact(ArtifactCoordinates.fromString(name));
        if (fp == null) throw new XmlPullParserException(String.format("Failed to resolve native artifact '%s'", name), reader, null);
        File lib = new File(fp.getParentFile(), "lib");
        if (!lib.exists()) {
            if (!fp.getParentFile().canWrite()) throw new XmlPullParserException(String.format("Native artifact '%s' cannot be unpacked", name), reader, null);
            unzip(fp, fp.getParentFile());
        }
        specBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(new NativeLibraryResourceLoader(lib), PathFilters.rejectAll()));
    }

    private static void parseNativeArtifact(final MavenResolver mavenResolver, final XmlPullParser reader, final ModuleSpec.Builder specBuilder) throws XmlPullParserException, IOException {
        String name = null;
        final Set<String> required = new HashSet<>(LIST_A_NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_NAME: name = reader.getAttributeValue(i); break;
                default: throw unknownAttribute(reader, i);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }

        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    try {
                        createMavenNativeArtifactLoader(mavenResolver, name, reader, specBuilder);
                    } catch (IOException e) {
                        throw new XmlPullParserException(String.format("Failed to add artifact '%s'", name), reader, e);
                    }
                    return;
                }
                case START_TAG: {
                    throw unexpectedContent(reader);
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseArtifact(final MavenResolver mavenResolver, final XmlPullParser reader, final ModuleSpec.Builder specBuilder) throws XmlPullParserException, IOException {
        String name = null;
        final Set<String> required = new HashSet<>(LIST_A_NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_NAME: name = reader.getAttributeValue(i); break;
                default: throw unknownAttribute(reader, i);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }

        final MultiplePathFilterBuilder filterBuilder = PathFilters.multiplePathFilterBuilder(true);
        final ResourceLoader resourceLoader;

        final Set<String> encountered = new HashSet<>();
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    try {
                        resourceLoader = MavenArtifactUtil.createMavenArtifactLoader(mavenResolver, name);
                    } catch (IOException e) {
                        throw new XmlPullParserException(String.format("Failed to add artifact '%s'", name), reader, e);
                    }
                    if (resourceLoader == null) throw new XmlPullParserException(String.format("Failed to resolve artifact '%s'", name), reader, null);
                    specBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(resourceLoader, filterBuilder.create()));
                    return;
                }
                case START_TAG: {
                    validateNamespace(reader);
                    final String element = reader.getName();
                    if (! encountered.add(element)) throw unexpectedContent(reader);
                    switch (element) {
                        case E_FILTER: parseFilterList(reader, filterBuilder); break;
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseResourceRoot(final ResourceRootFactory factory, final String rootPath, final XmlPullParser reader, final ModuleSpec.Builder specBuilder) throws XmlPullParserException, IOException {
        String name = null;
        String path = null;
        final Set<String> required = new HashSet<>(LIST_A_PATH);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_NAME: name = reader.getAttributeValue(i); break;
                case A_PATH: path = reader.getAttributeValue(i); break;
                default: throw unknownAttribute(reader, i);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }
        if (name == null) name = path;

        final MultiplePathFilterBuilder filterBuilder = PathFilters.multiplePathFilterBuilder(true);
        final ResourceLoader resourceLoader;

        final Set<String> encountered = new HashSet<>();
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    try {
                        resourceLoader = factory.createResourceLoader(rootPath, path, name);
                    } catch (IOException e) {
                        throw new XmlPullParserException(String.format("Failed to add resource root '%s' at path '%s'", name, path), reader, e);
                    }
                    specBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(resourceLoader, filterBuilder.create()));
                    return;
                }
                case START_TAG: {
                    validateNamespace(reader);
                    final String element = reader.getName();
                    if (! encountered.add(element)) throw unexpectedContent(reader);
                    switch (element) {
                        case E_FILTER: parseFilterList(reader, filterBuilder); break;
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseFilterList(final XmlPullParser reader, final MultiplePathFilterBuilder builder) throws XmlPullParserException, IOException {
        assertNoAttributes(reader);
        // xsd:choice
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    return;
                }
                case START_TAG: {
                    validateNamespace(reader);
                    switch (reader.getName()) {
                        case E_INCLUDE: parsePath(reader, true, builder); break;
                        case E_EXCLUDE: parsePath(reader, false, builder); break;
                        case E_INCLUDE_SET: parseSet(reader, true, builder); break;
                        case E_EXCLUDE_SET: parseSet(reader, false, builder); break;
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parsePath(final XmlPullParser reader, final boolean include, final MultiplePathFilterBuilder builder) throws XmlPullParserException, IOException {
        String path = null;
        final Set<String> required = new HashSet<>(LIST_A_PATH);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_PATH: path = reader.getAttributeValue(i); break;
                default: throw unknownAttribute(reader, i);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }

        final boolean literal = path.indexOf('*') == -1 && path.indexOf('?') == -1;
        if (literal) {
            if (path.charAt(path.length() - 1) == '/') {
                builder.addFilter(PathFilters.isChildOf(path), include);
            } else {
                builder.addFilter(PathFilters.is(path), include);
            }
        } else {
            builder.addFilter(PathFilters.match(path), include);
        }

        // consume remainder of element
        parseNoContent(reader);
    }

    private static Set<String> parseSet(final XmlPullParser reader) throws XmlPullParserException, IOException {
        assertNoAttributes(reader);
        final Set<String> set = new HashSet<>();
        // xsd:choice
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    return set;
                }
                case START_TAG: {
                    validateNamespace(reader);
                    switch (reader.getName()) {
                        case E_PATH: parsePathName(reader, set); break;
                        default: throw unexpectedContent(reader);
                    }
                }
            }
        }
    }

    private static void parseSet(final XmlPullParser reader, final boolean include, final MultiplePathFilterBuilder builder) throws XmlPullParserException, IOException {
        builder.addFilter(PathFilters.in(parseSet(reader)), include);
    }

    private static void parsePathName(final XmlPullParser reader, final Set<String> set) throws XmlPullParserException, IOException {
        String name = null;
        final Set<String> required = new HashSet<>(LIST_A_NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_NAME: name = reader.getAttributeValue(i); break;
                default: throw unknownAttribute(reader, i);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }
        set.add(name);

        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parseProperties(final XmlPullParser reader, final ModuleSpec.Builder specBuilder) throws XmlPullParserException, IOException {
        assertNoAttributes(reader);
        // xsd:choice
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    return;
                }
                case START_TAG: {
                    validateNamespace(reader);
                    switch (reader.getName()) {
                        case E_PROPERTY: {
                            parseProperty(reader, specBuilder);
                            break;
                        }
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseProperty(final XmlPullParser reader, final ModuleSpec.Builder specBuilder) throws XmlPullParserException, IOException {
        String name = null;
        String value = null;
        final Set<String> required = new HashSet<>(LIST_A_NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_NAME: name = reader.getAttributeValue(i); break;
                case A_VALUE: value = reader.getAttributeValue(i); break;
                default: throw unknownAttribute(reader, i);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }
        specBuilder.addProperty(name, value == null ? "true" : value);
        if ("jboss.assertions".equals(name)) try {
            specBuilder.setAssertionSetting(AssertionSetting.valueOf(value.toUpperCase(Locale.US)));
        } catch (IllegalArgumentException ignored) {}

        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parsePermissions(final XmlPullParser reader, final ModuleLoader moduleLoader, final String moduleName, final ModuleSpec.Builder specBuilder) throws XmlPullParserException, IOException {
        assertNoAttributes(reader);
        // xsd:choice
        ArrayList<PermissionFactory> list = new ArrayList<>();
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    specBuilder.setPermissionCollection(new FactoryPermissionCollection(list.toArray(new PermissionFactory[list.size()])));
                    return;
                }
                case START_TAG: {
                    validateNamespace(reader);
                    switch (reader.getName()) {
                        case E_GRANT: {
                            parseGrant(reader, moduleLoader, moduleName, list);
                            break;
                        }
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseGrant(final XmlPullParser reader, final ModuleLoader moduleLoader, final String moduleName, final ArrayList<PermissionFactory> list) throws XmlPullParserException, IOException {
        String permission = null;
        String name = null;
        String actions = null;
        final Set<String> required = new HashSet<>(LIST_A_PERMISSION_A_NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            validateAttributeNamespace(reader, i);
            final String attribute = reader.getAttributeName(i);
            required.remove(attribute);
            switch (attribute) {
                case A_PERMISSION: permission = reader.getAttributeValue(i); break;
                case A_NAME: name = reader.getAttributeValue(i); break;
                case A_ACTIONS: actions = reader.getAttributeValue(i); break;
                default: throw unknownAttribute(reader, i);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader, required);
        }
        expandName(moduleLoader, moduleName, list, permission, name, actions);

        // consume remainder of element
        parseNoContent(reader);
    }

    private static void expandName(final ModuleLoader moduleLoader, final String moduleName,
            final ArrayList<PermissionFactory> list, String permission, String name, String actions) {
        String expandedName = PolicyExpander.expand(name);
        //If a property can't be expanded in a permission entry that entry is ignored.
        //https://docs.oracle.com/javase/8/docs/technotes/guides/security/PolicyFiles.html#PropertyExp
        if(expandedName != null)
            list.add(new ModularPermissionFactory(moduleLoader, moduleName, permission, expandedName, actions));
    }

    private static void parseNoContent(final XmlPullParser reader) throws XmlPullParserException, IOException {
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case END_TAG: {
                    return;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseEndDocument(final XmlPullParser reader) throws XmlPullParserException, IOException {
        int eventType;
        for (;;) {
            eventType = reader.nextToken();
            switch (eventType) {
                case END_DOCUMENT: {
                    return;
                }
                case TEXT:
                case CDSECT: {
                    if (! reader.isWhitespace()) {
                        throw unexpectedContent(reader);
                    }
                    // ignore
                    break;
                }
                case IGNORABLE_WHITESPACE:
                case COMMENT: {
                    // ignore
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void unzip(File src, File destDir) throws IOException {
        final String absolutePath = destDir.getAbsolutePath();
        final ZipFile zip = new ZipFile(src);

        try {
            final Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                final File fp = new File(absolutePath, PathUtils.canonicalize(PathUtils.relativize(entry.getName())));
                final File parent = fp.getParentFile();
                if (! parent.exists()) {
                    parent.mkdirs();
                }
                final InputStream is = zip.getInputStream(entry);
                try {
                    final FileOutputStream os = new FileOutputStream(fp);
                    try {
                        copy(is, os);
                    } finally {
                        safeClose(os);
                    }
                } finally {
                    safeClose(is);
                }
            }
        } finally {
            safeClose(zip);
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[16384];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.flush();
    }

    private static void safeClose(Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable ignored) {}
    }
}
