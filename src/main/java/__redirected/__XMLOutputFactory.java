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

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
 * @authore Jason T. Greene
 */
public final class __XMLOutputFactory extends XMLOutputFactory {
    private static final Constructor<? extends XMLOutputFactory> PLATFORM_FACTORY;
    private static volatile Constructor<? extends XMLOutputFactory> DEFAULT_FACTORY;

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
            if (System.getProperty(XMLOutputFactory.class.getName(), "").equals(__XMLOutputFactory.class.getName())) {
                System.clearProperty(XMLOutputFactory.class.getName());
            }
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            try {
                DEFAULT_FACTORY = PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(XMLOutputFactory.class.getName(), __XMLOutputFactory.class.getName());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        Class<? extends XMLOutputFactory> clazz = __RedirectedUtils.loadProvider(id, XMLOutputFactory.class, loader);
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
    public __XMLOutputFactory() {
        Constructor<? extends XMLOutputFactory> factory = DEFAULT_FACTORY;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            if (loader != null) {
                Class<? extends XMLOutputFactory> provider = __RedirectedUtils.loadProvider(XMLOutputFactory.class, loader);
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
