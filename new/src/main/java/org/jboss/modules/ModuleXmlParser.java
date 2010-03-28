/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * A fast module.xml parser.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ModuleXmlParser {

    private ModuleXmlParser() {
    }

    private static final String NAMESPACE = "urn:jboss:module:1.0";

    enum Element {
        MODULE,
        DEPENDENCIES,
        RESOURCES,
        MAIN_CLASS,
        RESOURCE_ROOT,

        // default unknown element
        UNKNOWN;

        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE, "module"), Element.MODULE);
            elementsMap.put(new QName(NAMESPACE, "dependencies"), Element.DEPENDENCIES);
            elementsMap.put(new QName(NAMESPACE, "resources"), Element.RESOURCES);
            elementsMap.put(new QName(NAMESPACE, "main-class"), Element.MAIN_CLASS);
            elementsMap.put(new QName(NAMESPACE, "resource-root"), Element.RESOURCE_ROOT);
            elements = elementsMap;
        }

        static Element of(QName qName) {
            final Element element = elements.get(qName);
            return element == null ? UNKNOWN : element;
        }
    }

    enum Attribute {
        GROUP,
        NAME,
        VERSION,
        EXPORT,
        PATH,
        
        // default unknown attribute
        UNKNOWN;

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName("group"), GROUP);
            attributesMap.put(new QName("name"), NAME);
            attributesMap.put(new QName("version"), VERSION);
            attributesMap.put(new QName("export"), EXPORT);
            attributesMap.put(new QName("path"), PATH);
            attributes = attributesMap;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    static ModuleSpec parse(File file) throws ModuleLoadException {
        final FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new ModuleLoadException("No module.xml file found at " + file);
        }
        try {
            return parse(fis);
        } finally {
            safeClose(fis);
        }
    }

    private static void setIfSupported(XMLInputFactory inputFactory, String property, Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

    static ModuleSpec parse(InputStream source) throws ModuleLoadException {
        try {
            final XMLInputFactory inputFactory = XMLInputFactory.newFactory();
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(source);
            try {
                return parseDocument(streamReader, new ModuleSpec());
            } finally {
                safeClose(streamReader);
            }
        } catch (XMLStreamException e) {
            throw new ModuleLoadException("Failed to read module.xml: " + e.getMessage());
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

    private static XMLStreamException unexpectedContent(final Location location) {
        return new XMLStreamException("Unexpected content", location);
    }

    private static XMLStreamException endOfDocument(final Location location) {
        return new XMLStreamException("Unexpected end of document", location);
    }

    private static XMLStreamException missingAttributes(final Location location, final Set<Attribute> required) {
        final StringBuilder b = new StringBuilder("Missing one or more required attributes:");
        for (Attribute attribute : required) {
            b.append(' ').append(attribute);
        }
        return new XMLStreamException(b.toString(), location);
    }

    private static ModuleSpec parseDocument(XMLStreamReader reader, ModuleSpec spec) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.START_DOCUMENT: {
                    parseRootElement(reader, spec);
                    return spec;
                }
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.SPACE: {
                    // ignore
                    break;
                }
                default: {
                    throw unexpectedContent(reader.getLocation());
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseRootElement(final XMLStreamReader reader, final ModuleSpec spec) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT: {
                    if (Element.of(reader.getName()) != Element.MODULE) {
                        throw unexpectedContent(reader.getLocation());
                    }
                    parseModuleContents(reader, spec);
                    return;
                }
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.SPACE: {
                    // ignore
                    break;
                }
                default: {
                    throw unexpectedContent(reader.getLocation());
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseModuleContents(final XMLStreamReader reader, final ModuleSpec spec) throws XMLStreamException {
        // xsd:all
        Set<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    if (visited.contains(element)) {
                        throw unexpectedContent(reader.getLocation());
                    }
                    visited.add(element);
                    switch (element) {
                        case DEPENDENCIES: parseDependencies(reader, spec); break;
                        case MAIN_CLASS:   parseMainClass(reader, spec); break;
                        case RESOURCES:    parseResources(reader, spec); break;
                        default: throw unexpectedContent(reader.getLocation());
                    }
                    break;
                }
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.SPACE: {
                    // ignore
                    break;
                }
                default: {
                    throw unexpectedContent(reader.getLocation());
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseDependencies(final XMLStreamReader reader, final ModuleSpec spec) throws XMLStreamException {
        // xsd:choice
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case MODULE: parseModuleDependency(reader, spec); break;
                        default: throw unexpectedContent(reader.getLocation());
                    }
                    break;
                }
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.SPACE: {
                    // ignore
                    break;
                }
                default: {
                    throw unexpectedContent(reader.getLocation());
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseModuleDependency(final XMLStreamReader reader, final ModuleSpec spec) throws XMLStreamException {
        String group = null;
        String name = null;
        String version = null;
        boolean export = false;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.VERSION, Attribute.GROUP);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case GROUP:   group = reader.getAttributeValue(i); break;
                case NAME:    name = reader.getAttributeValue(i); break;
                case VERSION: version = reader.getAttributeValue(i); break;
                case EXPORT:  export = Boolean.parseBoolean(reader.getAttributeValue(i)); break;
                default: throw unexpectedContent(reader.getLocation());
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        // todo - assemble DependencySpec

        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parseMainClass(final XMLStreamReader reader, final ModuleSpec spec) throws XMLStreamException {
        String name = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: name = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader.getLocation());
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        spec.setMainClass(name);
        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parseResources(final XMLStreamReader reader, final ModuleSpec spec) throws XMLStreamException {
        // xsd:choice
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case RESOURCE_ROOT: parseResourceRoot(reader, spec); break;
                        default: throw unexpectedContent(reader.getLocation());
                    }
                    break;
                }
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.SPACE: {
                    // ignore
                    break;
                }
                default: {
                    throw unexpectedContent(reader.getLocation());
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseResourceRoot(final XMLStreamReader reader, final ModuleSpec spec) throws XMLStreamException {
        String name = null;
        String path = null;
        final Set<Attribute> required = EnumSet.of(Attribute.PATH);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: name = reader.getAttributeValue(i); break;
                case PATH: path = reader.getAttributeValue(i); break;
                default: throw unexpectedContent(reader.getLocation());
            }
        }
        if (! required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        if (name == null) name = path;
        // todo add to spec

        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parseNoContent(final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.SPACE: {
                    // ignore
                    break;
                }
                default: {
                    throw unexpectedContent(reader.getLocation());
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }
}
