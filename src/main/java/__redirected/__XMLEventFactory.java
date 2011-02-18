/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package __redirected;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import org.jboss.modules.Module;

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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@SuppressWarnings("unchecked")
public final class __XMLEventFactory extends XMLEventFactory {

    private static final Constructor<? extends XMLEventFactory> PLATFORM_FACTORY;

    static {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(null);
        try {
            XMLEventFactory factory = XMLEventFactory.newInstance();
            try {
                PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(XMLEventFactory.class.getName(), __XMLEventFactory.class.getName());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    /**
     * Init method.
     */
    public static void init() {}

    /**
     * Construct a new instance.
     */
    public __XMLEventFactory() {
        Module module = Module.forClassLoader(Thread.currentThread().getContextClassLoader(), true);
        if (module != null) {
            // todo - see if a specific impl is attached to the module
        }
        try {
            actual = PLATFORM_FACTORY.newInstance();
        } catch (InstantiationException e) {
            throw __RedirectedUtils.wrapped(new InstantiationError(e.getMessage()), e);
        } catch (IllegalAccessException e) {
            throw __RedirectedUtils.wrapped(new IllegalAccessError(e.getMessage()), e);
        } catch (InvocationTargetException e) {
            throw __RedirectedUtils.rethrowCause(e);
        }
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
