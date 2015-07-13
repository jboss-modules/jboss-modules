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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;

import javax.xml.XMLConstants;
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
    private static final Constructor<? extends SchemaFactory> PLATFORM_FACTORY;
    private static volatile Constructor<? extends SchemaFactory> DEFAULT_FACTORY;

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
            if (System.getProperty(SchemaFactory.class.getName(), "").equals(__SchemaFactory.class.getName())) {
                System.clearProperty(SchemaFactory.class.getName());
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                DEFAULT_FACTORY = PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(SchemaFactory.class.getName() + ":" + XMLConstants.W3C_XML_SCHEMA_NS_URI, __SchemaFactory.class.getName());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        Class<? extends SchemaFactory> clazz = __RedirectedUtils.loadProvider(id, SchemaFactory.class, loader);
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
    public __SchemaFactory() {
        Constructor<? extends SchemaFactory> factory = DEFAULT_FACTORY;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        SchemaFactory foundInstance = null;
        try {
            if (loader != null) {
                List<Class<? extends SchemaFactory>> providers = __RedirectedUtils.loadProviders(SchemaFactory.class, loader);
                for (Class<? extends SchemaFactory> provider : providers) {
                    SchemaFactory instance = provider.newInstance();
                    if (instance.isSchemaLanguageSupported(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
                        foundInstance = instance;
                        break;
                    }
                }
            }

            actual = foundInstance != null ? foundInstance : factory.newInstance();

        } catch (InstantiationException e) {
            throw __RedirectedUtils.wrapped(new InstantiationError(e.getMessage()), e);
        } catch (IllegalAccessException e) {
            throw __RedirectedUtils.wrapped(new IllegalAccessError(e.getMessage()), e);
        } catch (InvocationTargetException e) {
            throw __RedirectedUtils.rethrowCause(e);
        }
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
