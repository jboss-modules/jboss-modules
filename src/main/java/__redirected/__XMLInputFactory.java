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

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.jboss.modules.Module;

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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class __XMLInputFactory extends XMLInputFactory {
    private static final Constructor<? extends XMLInputFactory> PLATFORM_FACTORY;

    static {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(null);
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            try {
                PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(XMLInputFactory.class.getName(), __XMLInputFactory.class.getName());
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
    public __XMLInputFactory() {
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
