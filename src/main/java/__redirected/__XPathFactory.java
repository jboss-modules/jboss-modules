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
            if (System.getProperty(XPathFactory.class.getName(), "").equals(__XPathFactory.class.getName())) {
                System.clearProperty(XPathFactory.class.getName());
            }
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
