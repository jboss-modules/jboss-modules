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

import java.io.IOException;
import java.util.function.Supplier;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * A redirected SAXParserFactory
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jason T. Greene
 */
public final class __XMLReaderFactory implements XMLReader {
    private static final Supplier<XMLReader> PLATFORM_FACTORY = JDKSpecific.getPlatformXmlReaderSupplier();
    private static volatile Supplier<XMLReader> DEFAULT_FACTORY;

    static final String SAX_DRIVER = "org.xml.sax.driver";

    @Deprecated
    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        final Supplier<XMLReader> supplier = __RedirectedUtils.loadProvider(id, XMLReader.class, loader, SAX_DRIVER);
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
    public __XMLReaderFactory() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Supplier<XMLReader> factory = null;
        if (loader != null) {
            factory = __RedirectedUtils.loadProvider(XMLReader.class, loader, SAX_DRIVER);
        }
        if (factory == null) factory = DEFAULT_FACTORY;

        actual = factory.get();
    }

    private final XMLReader actual;

    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return actual.getFeature(name);
    }

    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        actual.setFeature(name, value);
    }

    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return actual.getProperty(name);
    }

    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        actual.setProperty(name, value);
    }

    public void setEntityResolver(EntityResolver resolver) {
        actual.setEntityResolver(resolver);
    }

    public EntityResolver getEntityResolver() {
        return actual.getEntityResolver();
    }

    public void setDTDHandler(DTDHandler handler) {
        actual.setDTDHandler(handler);
    }

    public DTDHandler getDTDHandler() {
        return actual.getDTDHandler();
    }

    public void setContentHandler(ContentHandler handler) {
        actual.setContentHandler(handler);
    }

    public ContentHandler getContentHandler() {
        return actual.getContentHandler();
    }

    public void setErrorHandler(ErrorHandler handler) {
        actual.setErrorHandler(handler);
    }

    public ErrorHandler getErrorHandler() {
        return actual.getErrorHandler();
    }

    public void parse(InputSource input) throws IOException, SAXException {
        actual.parse(input);
    }

    public void parse(String systemId) throws IOException, SAXException {
        actual.parse(systemId);
    }
}
