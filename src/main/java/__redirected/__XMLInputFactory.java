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
import java.util.function.Supplier;

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
    private static final Supplier<XMLInputFactory> PLATFORM_FACTORY = JDKSpecific.getPlatformXmlInputFactorySupplier();
    private static volatile Supplier<XMLInputFactory> DEFAULT_FACTORY;

    @Deprecated
    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        final Supplier<XMLInputFactory> supplier = __RedirectedUtils.loadProvider(id, XMLInputFactory.class, loader);
        if (supplier != null) {
            DEFAULT_FACTORY = supplier;
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
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Supplier<XMLInputFactory> factory = null;
        if (loader != null) {
            factory = __RedirectedUtils.loadProvider(XMLInputFactory.class, loader);
        }
        if (factory == null) factory = DEFAULT_FACTORY;

        actual = factory.get();
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
