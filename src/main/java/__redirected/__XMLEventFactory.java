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

package __redirected;

import java.util.Iterator;
import java.util.function.Supplier;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * A redirected XMLEventFactory
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jason T. Greene
 */
@SuppressWarnings("unchecked")
public final class __XMLEventFactory extends XMLEventFactory {
    private static final Supplier<XMLEventFactory> PLATFORM_FACTORY = JDKSpecific.getPlatformXmlEventFactorySupplier();
    private static volatile Supplier<XMLEventFactory> DEFAULT_FACTORY = PLATFORM_FACTORY;

    static {
        System.setProperty(XMLEventFactory.class.getName(), __XMLEventFactory.class.getName());
    }

    @Deprecated
    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        final Supplier<XMLEventFactory> supplier = __RedirectedUtils.loadProvider(id, XMLEventFactory.class, loader);
        if (supplier != null) {
            DEFAULT_FACTORY = supplier;
        }
    }

    public static void restorePlatformFactory() {
        DEFAULT_FACTORY = PLATFORM_FACTORY;
    }

    /**
     * Init method.
     */
    public static void init() {}

    /**
     * Construct a new instance.
     */
    public __XMLEventFactory() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Supplier<XMLEventFactory> factory = null;
        if (loader != null) {
            factory = __RedirectedUtils.loadProvider(XMLEventFactory.class, loader);
        }
        if (factory == null) factory = DEFAULT_FACTORY;

        actual = factory.get();
    }

    private final XMLEventFactory actual;

    public void setLocation(final Location location) {
        actual.setLocation(location);
    }

    public Attribute createAttribute(final String prefix, final String namespaceURI, final String localName, final String value) {
        return actual.createAttribute(prefix, namespaceURI, localName, value);
    }

    public Attribute createAttribute(final String localName, final String value) {
        return actual.createAttribute(localName, value);
    }

    public Attribute createAttribute(final QName name, final String value) {
        return actual.createAttribute(name, value);
    }

    public Namespace createNamespace(final String namespaceURI) {
        return actual.createNamespace(namespaceURI);
    }

    public Namespace createNamespace(final String prefix, final String namespaceUri) {
        return actual.createNamespace(prefix, namespaceUri);
    }

    public StartElement createStartElement(final QName name, final Iterator attributes, final Iterator namespaces) {
        return actual.createStartElement(name, attributes, namespaces);
    }

    public StartElement createStartElement(final String prefix, final String namespaceUri, final String localName) {
        return actual.createStartElement(prefix, namespaceUri, localName);
    }

    public StartElement createStartElement(final String prefix, final String namespaceUri, final String localName, final Iterator attributes, final Iterator namespaces) {
        return actual.createStartElement(prefix, namespaceUri, localName, attributes, namespaces);
    }

    public StartElement createStartElement(final String prefix, final String namespaceUri, final String localName, final Iterator attributes, final Iterator namespaces, final NamespaceContext context) {
        return actual.createStartElement(prefix, namespaceUri, localName, attributes, namespaces, context);
    }

    public EndElement createEndElement(final QName name, final Iterator namespaces) {
        return actual.createEndElement(name, namespaces);
    }

    public EndElement createEndElement(final String prefix, final String namespaceUri, final String localName) {
        return actual.createEndElement(prefix, namespaceUri, localName);
    }

    public EndElement createEndElement(final String prefix, final String namespaceUri, final String localName, final Iterator namespaces) {
        return actual.createEndElement(prefix, namespaceUri, localName, namespaces);
    }

    public Characters createCharacters(final String content) {
        return actual.createCharacters(content);
    }

    public Characters createCData(final String content) {
        return actual.createCData(content);
    }

    public Characters createSpace(final String content) {
        return actual.createSpace(content);
    }

    public Characters createIgnorableSpace(final String content) {
        return actual.createIgnorableSpace(content);
    }

    public StartDocument createStartDocument() {
        return actual.createStartDocument();
    }

    public StartDocument createStartDocument(final String encoding, final String version, final boolean standalone) {
        return actual.createStartDocument(encoding, version, standalone);
    }

    public StartDocument createStartDocument(final String encoding, final String version) {
        return actual.createStartDocument(encoding, version);
    }

    public StartDocument createStartDocument(final String encoding) {
        return actual.createStartDocument(encoding);
    }

    public EndDocument createEndDocument() {
        return actual.createEndDocument();
    }

    public EntityReference createEntityReference(final String name, final EntityDeclaration declaration) {
        return actual.createEntityReference(name, declaration);
    }

    public Comment createComment(final String text) {
        return actual.createComment(text);
    }

    public ProcessingInstruction createProcessingInstruction(final String target, final String data) {
        return actual.createProcessingInstruction(target, data);
    }

    public DTD createDTD(final String dtd) {
        return actual.createDTD(dtd);
    }
}
