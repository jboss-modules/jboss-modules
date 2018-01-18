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
import java.util.List;
import java.util.function.Supplier;

/**
 * A redirected XPathFactory
 *
 * @author Jason T. Greene
 */
public final class __XPathFactory extends XPathFactory {
    private static final Supplier<XPathFactory> PLATFORM_FACTORY = JDKSpecific.getPlatformXPathFactorySupplier();
    private static volatile Supplier<XPathFactory> DEFAULT_FACTORY = PLATFORM_FACTORY;

    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        final Supplier<XPathFactory> supplier = __RedirectedUtils.loadProvider(id, XPathFactory.class, loader);
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
    public static void init() {}

    /**
     * Construct a new instance.
     */
    public __XPathFactory() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        XPathFactory foundInstance = null;
        if (loader != null) {
            final List<Supplier<XPathFactory>> providers = __RedirectedUtils.loadProviders(XPathFactory.class, loader);
            for (Supplier<XPathFactory> provider : providers) {
                XPathFactory instance = provider.get();
                if (instance.isObjectModelSupported(XPathFactory.DEFAULT_OBJECT_MODEL_URI)) {
                    foundInstance = instance;
                    break;
                }
            }
        }
        actual = foundInstance != null ? foundInstance : DEFAULT_FACTORY.get();
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
