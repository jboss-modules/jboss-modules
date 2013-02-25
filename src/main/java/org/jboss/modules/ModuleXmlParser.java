/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.modules;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import org.jboss.modules.filter.MultiplePathFilterBuilder;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.security.FactoryPermissionCollection;
import org.jboss.modules.security.ModularPermissionFactory;
import org.jboss.modules.security.PermissionFactory;

import static javax.xml.stream.XMLStreamConstants.ATTRIBUTE;
import static javax.xml.stream.XMLStreamConstants.CDATA;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static javax.xml.stream.XMLStreamConstants.DTD;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.ENTITY_DECLARATION;
import static javax.xml.stream.XMLStreamConstants.ENTITY_REFERENCE;
import static javax.xml.stream.XMLStreamConstants.NAMESPACE;
import static javax.xml.stream.XMLStreamConstants.NOTATION_DECLARATION;
import static javax.xml.stream.XMLStreamConstants.PROCESSING_INSTRUCTION;
import static javax.xml.stream.XMLStreamConstants.SPACE;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * A fast, validating module.xml parser.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author thomas.diesler@jboss.com
 */
final class ModuleXmlParser {

    interface ResourceRootFactory {
        ResourceLoader createResourceLoader(final String rootPath, final String loaderPath, final String loaderName) throws IOException;
    }

    private ModuleXmlParser() {
    }

    enum Namespace {
        UNKNOWN,
        MODULE_1_0,
        MODULE_1_1,
        MODULE_1_2,
        ;

        private static final Map<String, Namespace> namespaces;

        static {
            Map<String, Namespace> namespacesMap = new HashMap<String, Namespace>();
            namespacesMap.put("urn:jboss:module:1.0", MODULE_1_0);
            namespacesMap.put("urn:jboss:module:1.1", MODULE_1_1);
            namespacesMap.put("urn:jboss:module:1.2", MODULE_1_2);
            namespaces = namespacesMap;
        }

        static Namespace of(QName qName) {
            Namespace namespace = namespaces.get(qName.getNamespaceURI());
            return namespace == null ? UNKNOWN : namespace;
        }
    }

    enum Element {
        MODULE,
        DEPENDENCIES,
        EXPORTS,
        IMPORTS,
        INCLUDE,
        INCLUDE_SET,
        EXCLUDE,
        EXCLUDE_SET,
        RESOURCES,
        MAIN_CLASS,
        RESOURCE_ROOT,
        PATH,
        FILTER,
        CONFIGURATION,
        LOADER,
        MODULE_PATH,
        IMPORT,
        SYSTEM,
        PATHS,
        MODULE_ALIAS,
        PROPERTIES,
        PROPERTY,
        MODULE_ABSENT,
        PERMISSIONS,
        GRANT,

        // default unknown element
        UNKNOWN;

        private static final Map<String, Element> elements;

        static {
            Map<String, Element> elementsMap = new HashMap<String, Element>();
            elementsMap.put("module", MODULE);
            elementsMap.put("dependencies", DEPENDENCIES);
            elementsMap.put("resources", RESOURCES);
            elementsMap.put("main-class", MAIN_CLASS);
            elementsMap.put("resource-root", RESOURCE_ROOT);
            elementsMap.put("path", PATH);
            elementsMap.put("exports", EXPORTS);
            elementsMap.put("imports", IMPORTS);
            elementsMap.put("include", INCLUDE);
            elementsMap.put("exclude", EXCLUDE);
            elementsMap.put("include-set", INCLUDE_SET);
            elementsMap.put("exclude-set", EXCLUDE_SET);
            elementsMap.put("filter", FILTER);
            elementsMap.put("configuration", CONFIGURATION);
            elementsMap.put("loader", LOADER);
            elementsMap.put("module-path", MODULE_PATH);
            elementsMap.put("import", IMPORT);
            elementsMap.put("system", SYSTEM);
            elementsMap.put("paths", PATHS);
            elementsMap.put("module-alias", MODULE_ALIAS);
            elementsMap.put("properties", PROPERTIES);
            elementsMap.put("property", PROPERTY);
            elementsMap.put("permissions", PERMISSIONS);
            elementsMap.put("grant", GRANT);
            elements = elementsMap;
        }

        static Element of(QName qName) {
            Namespace namespace = Namespace.of(qName);
            if (namespace == Namespace.UNKNOWN) {
                return UNKNOWN;
            }
            final Element element = elements.get(qName.getLocalPart());
            return element == null ? UNKNOWN : element;
        }
    }

    enum Attribute {
        NAME,
        SLOT,
        EXPORT,
        SERVICES,
        PATH,
        OPTIONAL,
        DEFAULT_LOADER,
        TARGET_NAME,
        TARGET_SLOT,
        VALUE,
        PERMISSION,
        ACTIONS,
        GROUP,
        ARTIFACT,
        VERSION,

        // default unknown attribute
        UNKNOWN;

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName("name"), NAME);
            attributesMap.put(new QName("slot"), SLOT);
            attributesMap.put(new QName("export"), EXPORT);
            attributesMap.put(new QName("services"), SERVICES);
            attributesMap.put(new QName("path"), PATH);
            attributesMap.put(new QName("optional"), OPTIONAL);
            attributesMap.put(new QName("default-loader"), DEFAULT_LOADER);
            attributesMap.put(new QName("target-name"), TARGET_NAME);
            attributesMap.put(new QName("target-slot"), TARGET_SLOT);
            attributesMap.put(new QName("value"), VALUE);
            attributesMap.put(new QName("permission"), PERMISSION);
            attributesMap.put(new QName("actions"), ACTIONS);
            attributesMap.put(new QName("group"), GROUP);
            attributesMap.put(new QName("artifact"), ARTIFACT);
            attributesMap.put(new QName("version"), VERSION);
            attributes = attributesMap;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    enum Disposition {
        NONE("none"),
        IMPORT("import"),
        EXPORT("export"),
        ;

        private static final Map<String, Disposition> values;

        static {
            final Map<String, Disposition> map = new HashMap<String, Disposition>();
            for (Disposition d : values()) {
                map.put(d.value, d);
            }
            values = map;
        }

        private final String value;

        Disposition(String value) {
            this.value = value;
        }

        static Disposition of(String value) {
            final Disposition disposition = values.get(value);
            return disposition == null ? NONE : disposition;
        }
    }

   static ResourceLoader createMavenArtifactLoader(final String loaderName, final String groupId, final String artifactId, final String version) throws IOException
   {
      File fp = MavenArtifactUtil.resolveJarArtifact(groupId, artifactId, version);
      if (fp == null) return null;
      JarFile jarFile = new JarFile(fp, true);
      return new JarFileResourceLoader(loaderName, jarFile);
   }

   static ModuleSpec parseModuleXml(final ModuleLoader moduleLoader, final ModuleIdentifier moduleIdentifier, final File root, final File moduleInfoFile) throws ModuleLoadException {
        final FileInputStream fis;
        try {
            fis = new FileInputStream(moduleInfoFile);
        } catch (FileNotFoundException e) {
            throw new ModuleLoadException("No module.xml file found at " + moduleInfoFile);
        }
        try {
            return parseModuleXml(new ResourceRootFactory() {
                public ResourceLoader createResourceLoader(final String rootPath, final String loaderPath, final String loaderName) throws IOException {
                        File file = new File(rootPath, loaderPath);
                        if (file.isDirectory()) {
                            return new FileResourceLoader(loaderName, file);
                        } else {
                            final JarFile jarFile = new JarFile(file, true);
                            return new JarFileResourceLoader(loaderName, jarFile);
                        }
                    }
             }, root.getPath(), new BufferedInputStream(fis), moduleInfoFile.getPath(), moduleLoader, moduleIdentifier);
        } finally {
            safeClose(fis);
        }
    }

    private static void setIfSupported(XMLInputFactory inputFactory, String property, Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    static ModuleSpec parseModuleXml(final ResourceRootFactory factory, final String rootPath, InputStream source, final String moduleInfoFile, final ModuleLoader moduleLoader, final ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        try {
            final XMLInputFactory inputFactory = INPUT_FACTORY;
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(source);
            try {
                return parseDocument(factory, rootPath, streamReader, moduleLoader, moduleIdentifier);
            } finally {
                safeClose(streamReader);
            }
        } catch (XMLStreamException e) {
            throw new ModuleLoadException("Error loading module from " + moduleInfoFile, e);
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private static void safeClose(final XMLStreamReader streamReader) {
        if (streamReader != null) try {
            streamReader.close();
        } catch (XMLStreamException e) {
            // ignore
        }
    }

    private static XMLStreamException unexpectedContent(final XMLStreamReader reader) {
        final String kind;
        switch (reader.getEventType()) {
            case ATTRIBUTE: kind = "attribute"; break;
            case CDATA: kind = "cdata"; break;
            case CHARACTERS: kind = "characters"; break;
            case COMMENT: kind = "comment"; break;
            case DTD: kind = "dtd"; break;
            case END_DOCUMENT: kind = "document end"; break;
            case END_ELEMENT: kind = "element end"; break;
            case ENTITY_DECLARATION: kind = "entity declaration"; break;
            case ENTITY_REFERENCE: kind = "entity ref"; break;
            case NAMESPACE: kind = "namespace"; break;
            case NOTATION_DECLARATION: kind = "notation declaration"; break;
            case PROCESSING_INSTRUCTION: kind = "processing instruction"; break;
            case SPACE: kind = "whitespace"; break;
            case START_DOCUMENT: kind = "document start"; break;
            case START_ELEMENT: kind = "element start"; break;
            default: kind = "unknown"; break;
        }
        final StringBuilder b = new StringBuilder("Unexpected content of type '").append(kind).append('\'');
        if (reader.hasName()) {
            b.append(" named '").append(reader.getName()).append('\'');
        }
        if (reader.hasText()) {
            b.append(", text is: '").append(reader.getText()).append('\'');
        }
        return new XMLStreamException(b.toString(), reader.getLocation());
    }

    private static XMLStreamException endOfDocument(final Location location) {
        return new XMLStreamException("Unexpected end of document", location);
    }

    private static XMLStreamException invalidModuleName(final Location location, final ModuleIdentifier expected) {
        return new XMLStreamException("Invalid/mismatched module name (expected " + expected + ")", location);
    }

    private static XMLStreamException missingAttributes(final Location location, final Set<Attribute> required) {
        final StringBuilder b = new StringBuilder("Missing one or more required attributes:");
        for (Attribute attribute : required) {
            b.append(' ').append(attribute);
        }
        return new XMLStreamException(b.toString(), location);
    }

    private static ModuleSpec parseDocument(final ResourceRootFactory factory, final String rootPath, XMLStreamReader reader, final ModuleLoader moduleLoader, final ModuleIdentifier moduleIdentifier) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case START_DOCUMENT: {
                    return parseRootElement(factory, rootPath, reader, moduleLoader, moduleIdentifier);
                }
                case START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case MODULE: {
                            final ModuleSpec.Builder specBuilder = ModuleSpec.build(moduleIdentifier);
                            parseModuleContents(reader, factory, moduleLoader, moduleIdentifier, specBuilder, rootPath);
                            parseEndDocument(reader);
                            return specBuilder.create();
                        }
                        case MODULE_ALIAS: {
                            final ModuleSpec moduleSpec = parseModuleAliasContents(reader, moduleIdentifier);
                            parseEndDocument(reader);
                            return moduleSpec;
                        }
                        case MODULE_ABSENT: {
                            parseModuleAbsentContents(reader, moduleIdentifier);
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
        throw endOfDocument(reader.getLocation());
    }

    private static ModuleSpec parseRootElement(final ResourceRootFactory factory, final String rootPath, final XMLStreamReader reader, final ModuleLoader moduleLoader, final ModuleIdentifier moduleIdentifier) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case MODULE: {
                            final ModuleSpec.Builder specBuilder = ModuleSpec.build(moduleIdentifier);
                            parseModuleContents(reader, factory, moduleLoader, moduleIdentifier, specBuilder, rootPath);
                            parseEndDocument(reader);
                            return specBuilder.create();
                        }
                        case MODULE_ALIAS: {
                            final ModuleSpec moduleSpec = parseModuleAliasContents(reader, moduleIdentifier);
                            parseEndDocument(reader);
                            return moduleSpec;
                        }
                        case MODULE_ABSENT: {
                            parseModuleAbsentContents(reader, moduleIdentifier);
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
        throw endOfDocument(reader.getLocation());
    }

    private static ModuleSpec parseModuleAliasContents(final XMLStreamReader reader, final ModuleIdentifier moduleIdentifier) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        String slot = null;
        String targetName = null;
        String targetSlot = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.TARGET_NAME);
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:    name = reader.getAttributeValue(i); break;
                case SLOT:    slot = reader.getAttributeValue(i); break;
                case TARGET_NAME: targetName = reader.getAttributeValue(i); break;
                case TARGET_SLOT: targetSlot = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        if (! moduleIdentifier.equals(ModuleIdentifier.create(name, slot))) {
            throw invalidModuleName(reader.getLocation(), moduleIdentifier);
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return ModuleSpec.buildAlias(moduleIdentifier, ModuleIdentifier.create(targetName, targetSlot)).create();
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseModuleAbsentContents(final XMLStreamReader reader, final ModuleIdentifier moduleIdentifier) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        String slot = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.TARGET_NAME);
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:    name = reader.getAttributeValue(i); break;
                case SLOT:    slot = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        if (! moduleIdentifier.equals(ModuleIdentifier.create(name, slot))) {
            throw invalidModuleName(reader.getLocation(), moduleIdentifier);
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseModuleContents(final XMLStreamReader reader, final ResourceRootFactory factory, final ModuleLoader moduleLoader, final ModuleIdentifier moduleIdentifier, final ModuleSpec.Builder specBuilder, final String rootPath) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        String slot = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:    name = reader.getAttributeValue(i); break;
                case SLOT:    slot = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        if (! specBuilder.getIdentifier().equals(ModuleIdentifier.create(name, slot))) {
            throw invalidModuleName(reader.getLocation(), specBuilder.getIdentifier());
        }
        // xsd:all
        MultiplePathFilterBuilder exportsBuilder = PathFilters.multiplePathFilterBuilder(true);
        Set<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    specBuilder.addDependency(DependencySpec.createLocalDependencySpec(PathFilters.acceptAll(), exportsBuilder.create()));
                    return;
                }
                case START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    if (visited.contains(element)) {
                        throw unexpectedContent(reader);
                    }
                    visited.add(element);
                    switch (element) {
                        case EXPORTS:      parseFilterList(reader, exportsBuilder); break;
                        case DEPENDENCIES: parseDependencies(reader, specBuilder); break;
                        case MAIN_CLASS:   parseMainClass(reader, specBuilder); break;
                        case RESOURCES:    parseResources(factory, rootPath, reader, specBuilder); break;
                        case PROPERTIES:   parseProperties(reader, specBuilder); break;
                        case PERMISSIONS:  parsePermissions(reader, moduleLoader, moduleIdentifier, specBuilder); break;
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseDependencies(final XMLStreamReader reader, final ModuleSpec.Builder specBuilder) throws XMLStreamException {
        // xsd:choice
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case MODULE: parseModuleDependency(reader, specBuilder); break;
                        case SYSTEM: parseSystemDependency(reader, specBuilder); break;
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseModuleDependency(final XMLStreamReader reader, final ModuleSpec.Builder specBuilder) throws XMLStreamException {
        String name = null;
        String slot = null;
        boolean export = false;
        boolean optional = false;
        Disposition services = Disposition.NONE;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:    name = reader.getAttributeValue(i); break;
                case SLOT:    slot = reader.getAttributeValue(i); break;
                case EXPORT:  export = Boolean.parseBoolean(reader.getAttributeValue(i)); break;
                case SERVICES:services = Disposition.of(reader.getAttributeValue(i)); break;
                case OPTIONAL:optional = Boolean.parseBoolean(reader.getAttributeValue(i)); break;
                default: throw unexpectedContent(reader);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        final MultiplePathFilterBuilder importBuilder = PathFilters.multiplePathFilterBuilder(true);
        final MultiplePathFilterBuilder exportBuilder = PathFilters.multiplePathFilterBuilder(export);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (services == ModuleXmlParser.Disposition.EXPORT) {
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
                        importFilter = services == Disposition.NONE ? PathFilters.getDefaultImportFilter() : PathFilters.getDefaultImportFilterWithServices();
                    } else {
                        if (services != Disposition.NONE) {
                            importBuilder.addFilter(PathFilters.getMetaInfServicesFilter(), true);
                        }
                        importBuilder.addFilter(PathFilters.getMetaInfSubdirectoriesFilter(), false);
                        importBuilder.addFilter(PathFilters.getMetaInfFilter(), false);
                        importFilter = importBuilder.create();
                    }
                    specBuilder.addDependency(DependencySpec.createModuleDependencySpec(importFilter, exportFilter, null, ModuleIdentifier.create(name, slot), optional));
                    return;
                }
                case START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case EXPORTS: parseFilterList(reader, exportBuilder); break;
                        case IMPORTS: parseFilterList(reader, importBuilder); break;
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

    private static void parseSystemDependency(final XMLStreamReader reader, final ModuleSpec.Builder specBuilder) throws XMLStreamException {
        boolean export = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case EXPORT:  export = Boolean.parseBoolean(reader.getAttributeValue(i)); break;
                default: throw unexpectedContent(reader);
            }
        }
        Set<String> paths = Collections.emptySet();
        final MultiplePathFilterBuilder exportBuilder = PathFilters.multiplePathFilterBuilder(export);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    final PathFilter exportFilter = exportBuilder.create();
                    specBuilder.addDependency(DependencySpec.createSystemDependencySpec(PathFilters.acceptAll(), exportFilter, paths));
                    return;
                }
                case START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case PATHS: {
                            paths = parseSet(reader);
                            break;
                        }
                        case EXPORTS: {
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

    private static void parseMainClass(final XMLStreamReader reader, final ModuleSpec.Builder specBuilder) throws XMLStreamException {
        String name = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: name = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        specBuilder.setMainClass(name);
        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parseResources(final ResourceRootFactory factory, final String rootPath, final XMLStreamReader reader, final ModuleSpec.Builder specBuilder) throws XMLStreamException {
        // xsd:choice
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    specBuilder.addResourceRoot(new ResourceLoaderSpec(new NativeLibraryResourceLoader(new File(rootPath, "lib")), PathFilters.rejectAll()));
                    return;
                }
                case START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case RESOURCE_ROOT: {
                            parseResourceRoot(factory, rootPath, reader, specBuilder);
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
        throw endOfDocument(reader.getLocation());
    }

    private static void parseResourceRoot(final ResourceRootFactory factory, final String rootPath, final XMLStreamReader reader, final ModuleSpec.Builder specBuilder) throws XMLStreamException {
        String name = null;
        String path = null;
        String group = null;
        String artifact = null;
        String version = null;
        final Set<Attribute> required = EnumSet.of(Attribute.PATH, Attribute.GROUP, Attribute.ARTIFACT, Attribute.VERSION);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: name = reader.getAttributeValue(i); break;
                case PATH: path = reader.getAttributeValue(i); break;
                case GROUP: group = reader.getAttributeValue(i); break;
                case ARTIFACT: artifact = reader.getAttributeValue(i); break;
                case VERSION: version = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader);
            }
        }
        // PATH or full maven attributes required
        if (required.contains(Attribute.PATH))
        {
           // if no attributes defined
           if (required.size() == 4) throw missingAttributes(reader.getLocation(), required);

           // check that maven attributes are defined
           required.remove(Attribute.PATH);
           if (!required.isEmpty()) throw missingAttributes(reader.getLocation(), required);
        }
        if (name == null) name = path;

        final MultiplePathFilterBuilder filterBuilder = PathFilters.multiplePathFilterBuilder(true);
        final ResourceLoader resourceLoader;

        final Set<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (path != null) {
                        try {
                            resourceLoader = factory.createResourceLoader(rootPath, path, name);
                        } catch (IOException e) {
                            throw new XMLStreamException(String.format("Failed to add resource root '%s' at path '%s'", name, path), reader.getLocation(), e);
                        }
                    } else {
                        try {
                           if (name == null) name = group + ":" + artifact + ":" + version;
                           resourceLoader = createMavenArtifactLoader(name, group, artifact, version);
                           if (resourceLoader == null) throw new XMLStreamException(String.format("Failed to add resource root maven artifact '%s'", name), reader.getLocation());
                        } catch (IOException e) {
                           throw new XMLStreamException(String.format("Failed to add resource root artifact '%s'", name), reader.getLocation(), e);
                        }
                    }
                    specBuilder.addResourceRoot(new ResourceLoaderSpec(resourceLoader, filterBuilder.create()));
                    return;
                }
                case START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    if (! encountered.add(element)) throw unexpectedContent(reader);
                    switch (element) {
                        case FILTER: parseFilterList(reader, filterBuilder); break;
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

    private static void parseFilterList(final XMLStreamReader reader, final MultiplePathFilterBuilder builder) throws XMLStreamException {
        // xsd:choice
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case INCLUDE: parsePath(reader, true, builder); break;
                        case EXCLUDE: parsePath(reader, false, builder); break;
                        case INCLUDE_SET: parseSet(reader, true, builder); break;
                        case EXCLUDE_SET: parseSet(reader, false, builder); break;
                        default: throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parsePath(final XMLStreamReader reader, final boolean include, final MultiplePathFilterBuilder builder) throws XMLStreamException {
        String path = null;
        final Set<Attribute> required = EnumSet.of(Attribute.PATH);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case PATH: path = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
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

    private static Set<String> parseSet(final XMLStreamReader reader) throws XMLStreamException {
        final Set<String> set = new FastCopyHashSet<String>();
        // xsd:choice
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return set;
                }
                case START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case PATH: parsePathName(reader, set); break;
                    }
                }
            }
        }
        return set;
    }

    private static void parseSet(final XMLStreamReader reader, final boolean include, final MultiplePathFilterBuilder builder) throws XMLStreamException {
        builder.addFilter(PathFilters.in(parseSet(reader)), include);
    }

    private static void parsePathName(final XMLStreamReader reader, final Set<String> set) throws XMLStreamException {
        String name = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: name = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        set.add(name);

        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parseProperties(final XMLStreamReader reader, final ModuleSpec.Builder specBuilder) throws XMLStreamException {
        // xsd:choice
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case PROPERTY: {
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
        throw endOfDocument(reader.getLocation());
    }

    private static void parseProperty(final XMLStreamReader reader, final ModuleSpec.Builder specBuilder) throws XMLStreamException {
        String name = null;
        String value = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: name = reader.getAttributeValue(i); break;
                case VALUE: value = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        specBuilder.addProperty(name, value == null ? "true" : value);
        if ("jboss.assertions".equals(name)) try {
            specBuilder.setAssertionSetting(AssertionSetting.valueOf(value.toUpperCase(Locale.US)));
        } catch (IllegalArgumentException ignored) {}

        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parsePermissions(final XMLStreamReader reader, final ModuleLoader moduleLoader, final ModuleIdentifier moduleIdentifier, final ModuleSpec.Builder specBuilder) throws XMLStreamException {
        // xsd:choice
        ArrayList<PermissionFactory> list = new ArrayList<PermissionFactory>();
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    specBuilder.setPermissionCollection(new FactoryPermissionCollection(list.toArray(new PermissionFactory[list.size()])));
                    return;
                }
                case START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case GRANT: {
                            parseGrant(reader, moduleLoader, moduleIdentifier, list);
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
        throw endOfDocument(reader.getLocation());
    }

    private static void parseGrant(final XMLStreamReader reader, final ModuleLoader moduleLoader, final ModuleIdentifier moduleIdentifier, final ArrayList<PermissionFactory> list) throws XMLStreamException {
        String permission = null;
        String name = null;
        String actions = null;
        final Set<Attribute> required = EnumSet.of(Attribute.PERMISSION, Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case PERMISSION: permission = reader.getAttributeValue(i); break;
                case NAME: name = reader.getAttributeValue(i); break;
                case ACTIONS: actions = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader);
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        list.add(new ModularPermissionFactory(moduleLoader, moduleIdentifier, permission, name, actions));

        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parseNoContent(final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseEndDocument(final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_DOCUMENT: {
                    return;
                }
                case CHARACTERS: {
                    if (! reader.isWhiteSpace()) {
                        throw unexpectedContent(reader);
                    }
                    // ignore
                    break;
                }
                case COMMENT:
                case SPACE: {
                    // ignore
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        return;
    }
}
