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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A redirected SAXParserFactory
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @authore Jason T. Greene
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
            thread.setContextClassLoader(old);
        }
    }

    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
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
