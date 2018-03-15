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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

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
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A redirected SAXParserFactory
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jason T. Greene
 */
public final class __XMLReaderFactory implements XMLReader {
    private static final Constructor<? extends XMLReader> PLATFORM_FACTORY;
    private static volatile Constructor<? extends XMLReader> DEFAULT_FACTORY;

    private static final String SAX_DRIVER = "org.xml.sax.driver";

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
        // MODULES-248: XMLReaderFactory fields tracking if jar files were already scanned needs to reset
        // before and after switching class loader
        resetScanTracking();
        try {
            if (System.getProperty(SAX_DRIVER, "").equals(__XMLReaderFactory.class.getName())) {
                System.clearProperty(SAX_DRIVER);
            }
            XMLReader factory = XMLReaderFactory.createXMLReader();
            try {
               DEFAULT_FACTORY = PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(SAX_DRIVER, __XMLReaderFactory.class.getName());
        } catch (SAXException e) {
             throw __RedirectedUtils.wrapped(new RuntimeException(e.getMessage()), e);
        } finally {
            resetScanTracking();
            thread.setContextClassLoader(old);
        }
    }

    @Deprecated
    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        Class<? extends XMLReader> clazz = __RedirectedUtils.loadProvider(id, XMLReader.class, loader, SAX_DRIVER);
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

    /**
     * Construct a new instance.
     */
    public __XMLReaderFactory() {
        Constructor<? extends XMLReader> factory = DEFAULT_FACTORY;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            if (loader != null) {
                Class<? extends XMLReader> provider = __RedirectedUtils.loadProvider(XMLReader.class, loader, SAX_DRIVER);
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
