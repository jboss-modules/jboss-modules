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

import java.util.function.Supplier;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * A redirected SAXParserFactory
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jason T. Greene
 */
public final class __SAXParserFactory extends SAXParserFactory {
    private static final Supplier<SAXParserFactory> PLATFORM_FACTORY = JDKSpecific.getPlatformSaxParserFactorySupplier();
    private static volatile Supplier<SAXParserFactory> DEFAULT_FACTORY = PLATFORM_FACTORY;

    @Deprecated
    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        final Supplier<SAXParserFactory> supplier = __RedirectedUtils.loadProvider(id, SAXParserFactory.class, loader);
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
    public __SAXParserFactory() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Supplier<SAXParserFactory> factory = null;
        if (loader != null) {
            factory = __RedirectedUtils.loadProvider(SAXParserFactory.class, loader);
        }
        if (factory == null) factory = DEFAULT_FACTORY;

        actual = factory.get();
    }

    private final SAXParserFactory actual;

    public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
        return actual.newSAXParser();
    }

    public void setNamespaceAware(final boolean awareness) {
        actual.setNamespaceAware(awareness);
    }

    public void setValidating(final boolean validating) {
        actual.setValidating(validating);
    }

    public boolean isNamespaceAware() {
        return actual.isNamespaceAware();
    }

    public boolean isValidating() {
        return actual.isValidating();
    }

    public void setFeature(final String name, final boolean value) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        actual.setFeature(name, value);
    }

    public boolean getFeature(final String name) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        return actual.getFeature(name);
    }

    public Schema getSchema() {
        return actual.getSchema();
    }

    public void setSchema(final Schema schema) {
        actual.setSchema(schema);
    }

    public void setXIncludeAware(final boolean state) {
        actual.setXIncludeAware(state);
    }

    public boolean isXIncludeAware() {
        return actual.isXIncludeAware();
    }
}
