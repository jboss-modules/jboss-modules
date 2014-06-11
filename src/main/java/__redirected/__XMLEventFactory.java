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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

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
 * @authore Jason T. Greene
 */
@SuppressWarnings("unchecked")
public final class __XMLEventFactory extends XMLEventFactory {
    private static final Constructor<? extends XMLEventFactory> PLATFORM_FACTORY;
    private static volatile Constructor<? extends XMLEventFactory> DEFAULT_FACTORY;

    static {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();

        // Unfortunately we can not use null because of a stupid bug in the jdk JAXP factory finder.
        // Lack of tccl causes the provider file discovery to fallback to the jaxp loader (bootclasspath)
        // which is correct. However, after parsing it, it then disables the fallback for the loading of the class.
        // Thus, the class can not be found.
        //
        // Work around the problem by using the System CL, although in the future we may want to just "inherit"
        // the environment's TCCL
        thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
        try {
            if (System.getProperty(XMLEventFactory.class.getName(), "").equals(__XMLEventFactory.class.getName())) {
                System.clearProperty(XMLEventFactory.class.getName());
            }
            XMLEventFactory factory = XMLEventFactory.newInstance();
            try {
                DEFAULT_FACTORY = PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(XMLEventFactory.class.getName(), __XMLEventFactory.class.getName());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        Class<? extends XMLEventFactory> clazz = __RedirectedUtils.loadProvider(id, XMLEventFactory.class, loader);
        if (clazz != null) {
            try {
                DEFAULT_FACTORY = clazz.getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
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
        Constructor<? extends XMLEventFactory> factory = DEFAULT_FACTORY;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            if (loader != null) {
                Class<? extends XMLEventFactory> provider = __RedirectedUtils.loadProvider(XMLEventFactory.class, loader);
                if (provider != null)
                    factory = provider.getConstructor();
            }

            actual = factory.newInstance();
        } catch (InstantiationException e) {
            throw __RedirectedUtils.wrapped(new InstantiationError(e.getMessage()), e);
        } catch (IllegalAccessException e) {
            throw __RedirectedUtils.wrapped(new IllegalAccessError(e.getMessage()), e);
        } catch (InvocationTargetException e) {
            throw __RedirectedUtils.rethrowCause(e);
        } catch (NoSuchMethodException e) {
            throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
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
