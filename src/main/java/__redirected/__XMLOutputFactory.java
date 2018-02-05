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

import java.io.OutputStream;
import java.io.Writer;
import java.util.function.Supplier;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * A redirected XMLOutputFactory
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jason T. Greene
 */
public final class __XMLOutputFactory extends XMLOutputFactory {
    private static final Supplier<XMLOutputFactory> PLATFORM_FACTORY = JDKSpecific.getPlatformXmlOutputFactorySupplier();
    private static volatile Supplier<XMLOutputFactory> DEFAULT_FACTORY = PLATFORM_FACTORY;

    static {
        System.setProperty(XMLOutputFactory.class.getName(), __XMLOutputFactory.class.getName());
    }

    @Deprecated
    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        final Supplier<XMLOutputFactory> supplier = __RedirectedUtils.loadProvider(id, XMLOutputFactory.class, loader);
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
    public __XMLOutputFactory() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Supplier<XMLOutputFactory> factory = null;
        if (loader != null) {
            factory = __RedirectedUtils.loadProvider(XMLOutputFactory.class, loader);
        }
        if (factory == null) factory = DEFAULT_FACTORY;

        actual = factory.get();
    }

    private final XMLOutputFactory actual;

    public XMLStreamWriter createXMLStreamWriter(final Writer stream) throws XMLStreamException {
        return actual.createXMLStreamWriter(stream);
    }

    public XMLStreamWriter createXMLStreamWriter(final OutputStream stream) throws XMLStreamException {
        return actual.createXMLStreamWriter(stream);
    }

    public XMLStreamWriter createXMLStreamWriter(final OutputStream stream, final String encoding) throws XMLStreamException {
        return actual.createXMLStreamWriter(stream, encoding);
    }

    public XMLStreamWriter createXMLStreamWriter(final Result result) throws XMLStreamException {
        return actual.createXMLStreamWriter(result);
    }

    public XMLEventWriter createXMLEventWriter(final Result result) throws XMLStreamException {
        return actual.createXMLEventWriter(result);
    }

    public XMLEventWriter createXMLEventWriter(final OutputStream stream) throws XMLStreamException {
        return actual.createXMLEventWriter(stream);
    }

    public XMLEventWriter createXMLEventWriter(final OutputStream stream, final String encoding) throws XMLStreamException {
        return actual.createXMLEventWriter(stream, encoding);
    }

    public XMLEventWriter createXMLEventWriter(final Writer stream) throws XMLStreamException {
        return actual.createXMLEventWriter(stream);
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
}
