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

package __redirected;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Supplier;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

final class JDKSpecific {

    static final ClassLoader SAFE_CL;

    static {
        // Unfortunately we can not use a null TCCL because of a stupid bug in the jdk JAXP factory finder.
        // Lack of tccl causes the provider file discovery to fallback to the jaxp loader (bootclasspath)
        // which is correct. However, after parsing it, it then disables the fallback for the loading of the class.
        // Thus, the class can not be found.
        //
        // Work around the problem by finding or creating a safe non-null CL to use for our operations.

        ClassLoader safeClassLoader = JDKSpecific.class.getClassLoader();
        if (safeClassLoader == null) {
            safeClassLoader = ClassLoader.getSystemClassLoader();
        }
        if (safeClassLoader == null) {
            safeClassLoader = new ClassLoader() {
            };
        }
        SAFE_CL = safeClassLoader;
    }

    static Supplier<DatatypeFactory> getPlatformDatatypeFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            return getConstructorSupplier(DatatypeFactory.newInstance());
        } catch (DatatypeConfigurationException e) {
            throw new IllegalArgumentException("Problem configuring DatatypeFactory", e);
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<DocumentBuilderFactory> getPlatformDocumentBuilderFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            return getConstructorSupplier(DocumentBuilderFactory.newInstance());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<SAXParserFactory> getPlatformSaxParserFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            return getConstructorSupplier(SAXParserFactory.newInstance());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<SchemaFactory> getPlatformSchemaFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            return getConstructorSupplier(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI));
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<TransformerFactory> getPlatformSaxTransformerFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            return getConstructorSupplier(TransformerFactory.newInstance());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<XMLEventFactory> getPlatformXmlEventFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            return getConstructorSupplier(XMLEventFactory.newInstance());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<XMLInputFactory> getPlatformXmlInputFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            return getConstructorSupplier(XMLInputFactory.newInstance());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<XMLOutputFactory> getPlatformXmlOutputFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            return getConstructorSupplier(XMLOutputFactory.newInstance());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<XMLReader> getPlatformXmlReaderSupplier() {
        final SAXParserFactory parserFactory = getPlatformSaxParserFactorySupplier().get();
        return new Supplier<XMLReader>() {
            public XMLReader get() {
                try {
                    return parserFactory.newSAXParser().getXMLReader();
                } catch (SAXException | ParserConfigurationException e) {
                    throw __RedirectedUtils.wrapped(new RuntimeException(e.getMessage()), e);
                }
            }
        };
    }

    static Supplier<XPathFactory> getPlatformXPathFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            return getConstructorSupplier(XPathFactory.newInstance());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Supplier<T> getConstructorSupplier(final T factory) {
        try {
            return new ConstructorSupplier<T>((Constructor<? extends T>) factory.getClass().getConstructor());
        } catch (NoSuchMethodException e) {
            throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
        }
    }
}
