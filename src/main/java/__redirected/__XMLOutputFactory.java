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
import org.jboss.modules.Module;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class __XMLOutputFactory extends XMLOutputFactory {
    private static final Constructor<? extends XMLOutputFactory> PLATFORM_FACTORY;

    static {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(null);
        try {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            try {
                PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(XMLOutputFactory.class.getName(), __XMLOutputFactory.class.getName());
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
    public __XMLOutputFactory() {
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
