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
    private static final Supplier<DocumentBuilderFactory> PLATFORM_FACTORY = JDKSpecific.getPlatformDocumentBuilderFactorySupplier();
    private static volatile Supplier<DocumentBuilderFactory> DEFAULT_FACTORY = PLATFORM_FACTORY;

    /**
     * Init method.
     */
    public static void init() {}

    @Deprecated
    public static void changeDefaultFactory(ModuleIdentifier id, ModuleLoader loader) {
        changeDefaultFactory(id.toString(), loader);
    }

    public static void changeDefaultFactory(String id, ModuleLoader loader) {
        final Supplier<DocumentBuilderFactory> supplier = __RedirectedUtils.loadProvider(id, DocumentBuilderFactory.class, loader);
        if (supplier != null) {
            DEFAULT_FACTORY = supplier;
        }
    }

    public static void restorePlatformFactory() {
        DEFAULT_FACTORY = PLATFORM_FACTORY;
    }

    /**
     * Construct a new instance.
     */
    public __DocumentBuilderFactory() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Supplier<DocumentBuilderFactory> factory = null;
        if (loader != null) {
            factory = __RedirectedUtils.loadProvider(DocumentBuilderFactory.class, loader);
        }
        if (factory == null) factory = DEFAULT_FACTORY;

        actual = factory.get();
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
