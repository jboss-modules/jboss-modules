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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * A redirecting DocumentBuilderFactory
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jason T. Greene
 */
public final class __DocumentBuilderFactory extends DocumentBuilderFactory {
    private static final Constructor<? extends DocumentBuilderFactory> PLATFORM_FACTORY;
    private static volatile Constructor<? extends DocumentBuilderFactory> DEFAULT_FACTORY;

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
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                DEFAULT_FACTORY = PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(DocumentBuilderFactory.class.getName(), __DocumentBuilderFactory.class.getName());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    /**
     * Init method.
     */
    public static void init() {}

    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        Class<? extends DocumentBuilderFactory> clazz = __RedirectedUtils.loadProvider(id, DocumentBuilderFactory.class, loader);
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
     * Construct a new instance.
     */
    public __DocumentBuilderFactory() {
        Constructor<? extends DocumentBuilderFactory> factory = DEFAULT_FACTORY;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            if (loader != null) {
                Class<? extends DocumentBuilderFactory> provider = __RedirectedUtils.loadProvider(DocumentBuilderFactory.class, loader);
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

    private final DocumentBuilderFactory actual;

    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return actual.newDocumentBuilder();
    }

    public void setNamespaceAware(final boolean awareness) {
        actual.setNamespaceAware(awareness);
    }

    public void setValidating(final boolean validating) {
        actual.setValidating(validating);
    }

    public void setIgnoringElementContentWhitespace(final boolean whitespace) {
        actual.setIgnoringElementContentWhitespace(whitespace);
    }

    public void setExpandEntityReferences(final boolean expandEntityRef) {
        actual.setExpandEntityReferences(expandEntityRef);
    }

    public void setIgnoringComments(final boolean ignoreComments) {
        actual.setIgnoringComments(ignoreComments);
    }

    public void setCoalescing(final boolean coalescing) {
        actual.setCoalescing(coalescing);
    }

    public boolean isNamespaceAware() {
        return actual.isNamespaceAware();
    }

    public boolean isValidating() {
        return actual.isValidating();
    }

    public boolean isIgnoringElementContentWhitespace() {
        return actual.isIgnoringElementContentWhitespace();
    }

    public boolean isExpandEntityReferences() {
        return actual.isExpandEntityReferences();
    }

    public boolean isIgnoringComments() {
        return actual.isIgnoringComments();
    }

    public boolean isCoalescing() {
        return actual.isCoalescing();
    }

    public void setAttribute(final String name, final Object value) throws IllegalArgumentException {
        actual.setAttribute(name, value);
    }

    public Object getAttribute(final String name) throws IllegalArgumentException {
        return actual.getAttribute(name);
    }

    public void setFeature(final String name, final boolean value) throws ParserConfigurationException {
        actual.setFeature(name, value);
    }

    public boolean getFeature(final String name) throws ParserConfigurationException {
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
