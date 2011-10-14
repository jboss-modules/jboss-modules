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

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * A redirected XPathFactory
 *
 * @author Jason T. Greene
 */
public final class __XPathFactory extends XPathFactory {
    private static final Constructor<? extends XPathFactory> PLATFORM_FACTORY;
    private static volatile Constructor<? extends XPathFactory> DEFAULT_FACTORY;

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
            XPathFactory factory = XPathFactory.newInstance();
            try {
                DEFAULT_FACTORY = PLATFORM_FACTORY = factory.getClass().getConstructor();
            } catch (NoSuchMethodException e) {
                throw __RedirectedUtils.wrapped(new NoSuchMethodError(e.getMessage()), e);
            }
            System.setProperty(XPathFactory.class.getName() + ":" + XPathFactory.DEFAULT_OBJECT_MODEL_URI, __XPathFactory.class.getName());
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        Class<? extends XPathFactory> clazz = __RedirectedUtils.loadProvider(id, XPathFactory.class, loader);
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
    public __XPathFactory() {
        Constructor<? extends XPathFactory> factory = DEFAULT_FACTORY;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        XPathFactory foundInstance = null;
        try {
            if (loader != null) {
                List<Class<? extends XPathFactory>> providers = __RedirectedUtils.loadProviders(XPathFactory.class, loader);
                for (Class<? extends XPathFactory> provider : providers) {
                    XPathFactory instance = provider.newInstance();
                    if (instance.isObjectModelSupported(XPathFactory.DEFAULT_OBJECT_MODEL_URI)) {
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

    private final XPathFactory actual;

    public boolean isObjectModelSupported(String objectModel) {
        return actual.isObjectModelSupported(objectModel);
    }

    public void setFeature(String name, boolean value) throws XPathFactoryConfigurationException {
        actual.setFeature(name, value);
    }

    public boolean getFeature(String name) throws XPathFactoryConfigurationException {
        return actual.getFeature(name);
    }

    public void setXPathVariableResolver(XPathVariableResolver resolver) {
        actual.setXPathVariableResolver(resolver);
    }

    public void setXPathFunctionResolver(XPathFunctionResolver resolver) {
        actual.setXPathFunctionResolver(resolver);
    }

    public XPath newXPath() {
        return actual.newXPath();
    }
}
