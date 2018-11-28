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

import java.util.function.Supplier;

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
    private static final Supplier<TransformerFactory> PLATFORM_FACTORY = JDKSpecific.getPlatformSaxTransformerFactorySupplier();
    private static volatile Supplier<TransformerFactory> DEFAULT_FACTORY = PLATFORM_FACTORY;

    @Deprecated
    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        final Supplier<TransformerFactory> supplier = __RedirectedUtils.loadProvider(id, TransformerFactory.class, loader);
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
    @Deprecated
    public static void init() {}

    /**
     * Construct a new instance.
     */
    public __TransformerFactory() {
        actual = DEFAULT_FACTORY.get();
        saxtual = (actual instanceof SAXTransformerFactory) ? (SAXTransformerFactory)actual : null;
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
        return saxtual.newTransformerHandler(src);
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
