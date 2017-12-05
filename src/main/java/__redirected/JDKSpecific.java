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
            clearProperty(DatatypeFactory.class, __DatatypeFactory.class);
            final Supplier<DatatypeFactory> supplier = getConstructorSupplier(DatatypeFactory.newInstance());
            System.setProperty(DatatypeFactory.class.getName(), __DatatypeFactory.class.getName());
            return supplier;
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
            clearProperty(DocumentBuilderFactory.class, __DocumentBuilderFactory.class);
            final Supplier<DocumentBuilderFactory> supplier = getConstructorSupplier(DocumentBuilderFactory.newInstance());
            System.setProperty(DocumentBuilderFactory.class.getName(), __DocumentBuilderFactory.class.getName());
            return supplier;
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<SAXParserFactory> getPlatformSaxParserFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            clearProperty(SAXParserFactory.class, __SAXParserFactory.class);
            final Supplier<SAXParserFactory> supplier = getConstructorSupplier(SAXParserFactory.newInstance());
            System.setProperty(SAXParserFactory.class.getName(), __SAXParserFactory.class.getName());
            return supplier;
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<SchemaFactory> getPlatformSchemaFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            clearProperty(SchemaFactory.class, __SchemaFactory.class);
            final Supplier<SchemaFactory> supplier = getConstructorSupplier(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI));
            System.setProperty(SchemaFactory.class.getName() + ":" + XMLConstants.W3C_XML_SCHEMA_NS_URI, __SchemaFactory.class.getName());
            return supplier;
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<TransformerFactory> getPlatformSaxTransformerFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            clearProperty(TransformerFactory.class, __TransformerFactory.class);
            final Supplier<TransformerFactory> supplier = getConstructorSupplier(TransformerFactory.newInstance());
            System.setProperty(TransformerFactory.class.getName(), __TransformerFactory.class.getName());
            return supplier;
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<XMLEventFactory> getPlatformXmlEventFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            clearProperty(XMLEventFactory.class, __XMLEventFactory.class);
            final Supplier<XMLEventFactory> supplier = getConstructorSupplier(XMLEventFactory.newInstance());
            System.setProperty(XMLEventFactory.class.getName(), __XMLEventFactory.class.getName());
            return supplier;
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<XMLInputFactory> getPlatformXmlInputFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            clearProperty(XMLInputFactory.class, __XMLInputFactory.class);
            final Supplier<XMLInputFactory> supplier = getConstructorSupplier(XMLInputFactory.newInstance());
            System.setProperty(XMLInputFactory.class.getName(), __XMLInputFactory.class.getName());
            return supplier;
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<XMLOutputFactory> getPlatformXmlOutputFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            clearProperty(XMLOutputFactory.class, __XMLOutputFactory .class);
            final Supplier<XMLOutputFactory> supplier = getConstructorSupplier(XMLOutputFactory.newInstance());
            System.setProperty(XMLOutputFactory.class.getName(), __XMLOutputFactory.class.getName());
            return supplier;
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<XMLReader> getPlatformXmlReaderSupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        // MODULES-248: XMLReaderFactory fields tracking if jar files were already scanned needs to reset
        // before and after switching class loader
        resetScanTracking();
        try {
            clearProperty(__XMLReaderFactory.SAX_DRIVER, __XMLReaderFactory.class);
            final Supplier<XMLReader> supplier = getConstructorSupplier(XMLReaderFactory.createXMLReader());
            System.setProperty(__XMLReaderFactory.SAX_DRIVER, __XMLReaderFactory.class.getName());
            return supplier;
        } catch (SAXException e) {
             throw __RedirectedUtils.wrapped(new RuntimeException(e.getMessage()), e);
        } finally {
            resetScanTracking();
            thread.setContextClassLoader(old);
        }
    }

    static Supplier<XPathFactory> getPlatformXPathFactorySupplier() {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(SAFE_CL);
        try {
            clearProperty(XPathFactory.class, __XPathFactory.class);
            final Supplier<XPathFactory> supplier = getConstructorSupplier(XPathFactory.newInstance());
            System.setProperty(XPathFactory.class.getName() + ":" + XPathFactory.DEFAULT_OBJECT_MODEL_URI, __XPathFactory.class.getName());
            return supplier;
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    private static void resetScanTracking() {
        try {
            Field clsFromJar = XMLReaderFactory.class.getDeclaredField("_clsFromJar");
            clsFromJar.setAccessible(true);
            clsFromJar.set(XMLReaderFactory.class, null);
            Field jarread = XMLReaderFactory.class.getDeclaredField("_jarread");
            jarread.setAccessible(true);
            jarread.setBoolean(XMLReaderFactory.class, false);
        } catch (NoSuchFieldException e) {
            //no-op, original SAX XMLReaderFactory hasn't helper fields _clsFromJar and _jarread
        } catch (IllegalAccessException e) {
            throw __RedirectedUtils.wrapped(new RuntimeException(e.getMessage()), e);
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

    private static void clearProperty(final Class<?> propertyClass, final Class<?> expectClass) {
        clearProperty(propertyClass.getName(), expectClass);
    }

    private static void clearProperty(final String propertyName, final Class<?> expectClass) {
        if (System.getProperty(propertyName, "").equals(expectClass.getName())) {
            System.clearProperty(propertyName);
        }
    }
}
