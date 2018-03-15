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

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.xml.stream.EventFilter;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * A redirected XMLInputFactory
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jason T. Greene
 */
public final class __XMLInputFactory extends XMLInputFactory {
    private static final Constructor<? extends XMLInputFactory> PLATFORM_FACTORY;
    private static volatile Constructor<? extends XMLInputFactory> DEFAULT_FACTORY;

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
            if (System.getProperty(XMLInputFactory.class.getName(), "").equals(__XMLInputFactory .class.getName())) {
                System.clearProperty(XMLInputFactory.class.getName());
            }
            XMLInputFactory factory = XMLInputFactory.newInstance();
            try {
                DEFAULT_FACTORY = PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(XMLInputFactory.class.getName(), __XMLInputFactory.class.getName());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    @Deprecated
    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        Class<? extends XMLInputFactory> clazz = __RedirectedUtils.loadProvider(id, XMLInputFactory.class, loader);
        if (clazz != null) {
            try {
                DEFAULT_FACTORY = clazz.getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
        }
    }

    /**
     * Init method.
     */
    public static void init() {}

    public static void restorePlatformFactory() {
        DEFAULT_FACTORY = PLATFORM_FACTORY;
    }

    /**
     * Construct a new instance.
     */
    public __XMLInputFactory() {
        Constructor<? extends XMLInputFactory> factory = DEFAULT_FACTORY;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            if (loader != null) {
                Class<? extends XMLInputFactory> provider = __RedirectedUtils.loadProvider(XMLInputFactory.class, loader);
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

    private final XMLInputFactory actual;

    public XMLStreamReader createXMLStreamReader(final Reader reader) throws XMLStreamException {
        return actual.createXMLStreamReader(reader);
    }

    public XMLStreamReader createXMLStreamReader(final Source source) throws XMLStreamException {
        return actual.createXMLStreamReader(source);
    }

    public XMLStreamReader createXMLStreamReader(final InputStream stream) throws XMLStreamException {
        return actual.createXMLStreamReader(stream);
    }

    public XMLStreamReader createXMLStreamReader(final InputStream stream, final String encoding) throws XMLStreamException {
        return actual.createXMLStreamReader(stream, encoding);
    }

    public XMLStreamReader createXMLStreamReader(final String systemId, final InputStream stream) throws XMLStreamException {
        return actual.createXMLStreamReader(systemId, stream);
    }

    public XMLStreamReader createXMLStreamReader(final String systemId, final Reader reader) throws XMLStreamException {
        return actual.createXMLStreamReader(systemId, reader);
    }

    public XMLEventReader createXMLEventReader(final Reader reader) throws XMLStreamException {
        return actual.createXMLEventReader(reader);
    }

    public XMLEventReader createXMLEventReader(final String systemId, final Reader reader) throws XMLStreamException {
        return actual.createXMLEventReader(systemId, reader);
    }

    public XMLEventReader createXMLEventReader(final XMLStreamReader reader) throws XMLStreamException {
        return actual.createXMLEventReader(reader);
    }

    public XMLEventReader createXMLEventReader(final Source source) throws XMLStreamException {
        return actual.createXMLEventReader(source);
    }

    public XMLEventReader createXMLEventReader(final InputStream stream) throws XMLStreamException {
        return actual.createXMLEventReader(stream);
    }

    public XMLEventReader createXMLEventReader(final InputStream stream, final String encoding) throws XMLStreamException {
        return actual.createXMLEventReader(stream, encoding);
    }

    public XMLEventReader createXMLEventReader(final String systemId, final InputStream stream) throws XMLStreamException {
        return actual.createXMLEventReader(systemId, stream);
    }

    public XMLStreamReader createFilteredReader(final XMLStreamReader reader, final StreamFilter filter) throws XMLStreamException {
        return actual.createFilteredReader(reader, filter);
    }

    public XMLEventReader createFilteredReader(final XMLEventReader reader, final EventFilter filter) throws XMLStreamException {
        return actual.createFilteredReader(reader, filter);
    }

    public XMLResolver getXMLResolver() {
        return actual.getXMLResolver();
    }

    public void setXMLResolver(final XMLResolver resolver) {
        actual.setXMLResolver(resolver);
    }

    public XMLReporter getXMLReporter() {
        return actual.getXMLReporter();
    }

    public void setXMLReporter(final XMLReporter reporter) {
        actual.setXMLReporter(reporter);
    }

    public void setProperty(final String name, final Object value) throws IllegalArgumentException {
        actual.setProperty(name, value);
    }

    public Object getProperty(final String name) throws IllegalArgumentException {
        return actual.getProperty(name);
    }

    public boolean isPropertySupported(final String name) {
        return actual.isPropertySupported(name);
    }

    public void setEventAllocator(final XMLEventAllocator allocator) {
        actual.setEventAllocator(allocator);
    }

    public XMLEventAllocator getEventAllocator() {
        return actual.getEventAllocator();
    }
}
