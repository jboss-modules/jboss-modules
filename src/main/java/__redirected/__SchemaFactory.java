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

import java.io.File;
import java.net.URL;
import java.util.function.Supplier;

import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * A redirected SchemaFactory
 *
 * @author Jason T. Greene
 */
public final class __SchemaFactory extends SchemaFactory {
    private static final Supplier<SchemaFactory> PLATFORM_FACTORY = JDKSpecific.getPlatformSchemaFactorySupplier();
    private static volatile Supplier<SchemaFactory> DEFAULT_FACTORY = PLATFORM_FACTORY;

    @Deprecated
    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        final Supplier<SchemaFactory> supplier = __RedirectedUtils.loadProvider(id, SchemaFactory.class, loader);
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
    @Deprecated
    public static void init() {}

    /**
     * Construct a new instance.
     */
    public __SchemaFactory() {
        actual = DEFAULT_FACTORY.get();
    }


    private final SchemaFactory actual;

    public boolean isSchemaLanguageSupported(String objectModel) {
        return actual.isSchemaLanguageSupported(objectModel);
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return actual.getFeature(name);
    }

    public void setFeature(String name, boolean value) throws SAXNotSupportedException, SAXNotRecognizedException {
        actual.setFeature(name, value);
    }

    public void setProperty(String name, Object object) throws SAXNotRecognizedException, SAXNotSupportedException {
        actual.setProperty(name, object);
    }

    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return actual.getProperty(name);
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        actual.setErrorHandler(errorHandler);
    }

    public ErrorHandler getErrorHandler() {
        return actual.getErrorHandler();
    }

    public void setResourceResolver(LSResourceResolver resourceResolver) {
        actual.setResourceResolver(resourceResolver);
    }

    public LSResourceResolver getResourceResolver() {
        return actual.getResourceResolver();
    }

    public Schema newSchema(Source schema) throws SAXException {
        return actual.newSchema(schema);
    }

    public Schema newSchema(File schema) throws SAXException {
        return actual.newSchema(schema);
    }

    public Schema newSchema(URL schema) throws SAXException {
        return actual.newSchema(schema);
    }

    public Schema newSchema(Source[] schemas) throws SAXException {
        return actual.newSchema(schemas);
    }

    public Schema newSchema() throws SAXException {
        return actual.newSchema();
    }
}
