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

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.xml.sax.XMLFilter;

/**
 * A redirected TransformerFactory
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jason T. Greene
 */
public final class __TransformerFactory extends SAXTransformerFactory {
    private static final Constructor<? extends TransformerFactory> PLATFORM_FACTORY;
    private static volatile Constructor<? extends TransformerFactory> DEFAULT_FACTORY;

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
            TransformerFactory factory = TransformerFactory.newInstance();
            try {
                DEFAULT_FACTORY = PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(TransformerFactory.class.getName(), __TransformerFactory.class.getName());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        Class<? extends TransformerFactory> clazz = __RedirectedUtils.loadProvider(id, TransformerFactory.class, loader);
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
    public __TransformerFactory() {
        Constructor<? extends TransformerFactory> factory = DEFAULT_FACTORY;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            if (loader != null) {
                Class<? extends TransformerFactory> provider = __RedirectedUtils.loadProvider(TransformerFactory.class, loader);
                if (provider != null)
                    factory = provider.getConstructor();
            }

            actual = factory.newInstance();
            saxtual = (actual instanceof SAXTransformerFactory) ? (SAXTransformerFactory)actual : null;

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

    private final TransformerFactory actual;
    private final SAXTransformerFactory saxtual; // Snicker

    public Transformer newTransformer(Source source) throws TransformerConfigurationException {
        return actual.newTransformer(source);
    }

    public Transformer newTransformer() throws TransformerConfigurationException {
        return actual.newTransformer();
    }

    public Templates newTemplates(Source source) throws TransformerConfigurationException {
        return actual.newTemplates(source);
    }

    public String toString() {
        return actual.toString();
    }

    public Source getAssociatedStylesheet(Source source, String media, String title, String charset)
            throws TransformerConfigurationException {
        return actual.getAssociatedStylesheet(source, media, title, charset);
    }

    public void setURIResolver(URIResolver resolver) {
        actual.setURIResolver(resolver);
    }

    public URIResolver getURIResolver() {
        return actual.getURIResolver();
    }

    public void setFeature(String name, boolean value) throws TransformerConfigurationException {
        actual.setFeature(name, value);
    }

    public boolean getFeature(String name) {
        return actual.getFeature(name);
    }

    public void setAttribute(String name, Object value) {
        actual.setAttribute(name, value);
    }

    public Object getAttribute(String name) {
        return actual.getAttribute(name);
    }

    public void setErrorListener(ErrorListener listener) {
        actual.setErrorListener(listener);
    }

    public ErrorListener getErrorListener() {
        return actual.getErrorListener();
    }

    public TransformerHandler newTransformerHandler(Source src) throws TransformerConfigurationException {
        if (saxtual == null)
            throw new TransformerConfigurationException("Provider is not a SAXTransformerFactory");
        return saxtual.newTransformerHandler();
    }

    public TransformerHandler newTransformerHandler(Templates templates) throws TransformerConfigurationException {
        if (saxtual == null)
            throw new TransformerConfigurationException("Provider is not a SAXTransformerFactory");
        return saxtual.newTransformerHandler(templates);
    }

    public TransformerHandler newTransformerHandler() throws TransformerConfigurationException {
        if (saxtual == null)
            throw new TransformerConfigurationException("Provider is not a SAXTransformerFactory");
        return saxtual.newTransformerHandler();
    }

    public TemplatesHandler newTemplatesHandler() throws TransformerConfigurationException {
        if (saxtual == null)
            throw new TransformerConfigurationException("Provider is not a SAXTransformerFactory");
        return saxtual.newTemplatesHandler();
    }

    public XMLFilter newXMLFilter(Source src) throws TransformerConfigurationException {
        if (saxtual == null)
            throw new TransformerConfigurationException("Provider is not a SAXTransformerFactory");
        return saxtual.newXMLFilter(src);
    }

    public XMLFilter newXMLFilter(Templates templates) throws TransformerConfigurationException {
        if (saxtual == null)
            throw new TransformerConfigurationException("Provider is not a SAXTransformerFactory");
        return saxtual.newXMLFilter(templates);
    }
}
