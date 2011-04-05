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

/**
 * Executes common operations against ALL JAXP redirection classes.
 *
 * @author Jason T. Greene
 */
public class __JAXPRedirected {

    /**
     * Change all provided factories to the ones contained in the
     * specified module using the standard META-INF/services lookup
     * pattern.
     *
     * @param id the id for the jaxp module
     * @param loader the loader containing the jaxp module
     */
    public static void changeAll(ModuleIdentifier id, ModuleLoader loader) {
        __DocumentBuilderFactory.changeDefaultFactory(id, loader);
        __SAXParserFactory.changeDefaultFactory(id, loader);
        __XMLEventFactory.changeDefaultFactory(id, loader);
        __TransformerFactory.changeDefaultFactory(id, loader);
        __XPathFactory.changeDefaultFactory(id, loader);
        __XMLEventFactory.changeDefaultFactory(id, loader);
        __XMLInputFactory.changeDefaultFactory(id, loader);
        __XMLOutputFactory.changeDefaultFactory(id, loader);
        __DatatypeFactory.changeDefaultFactory(id, loader);
    }

    /**
     * Restores all JAXP factories to the ones contained in the JDK
     * system classpath.
     */
    public static void restorePlatformFactory() {
        __DocumentBuilderFactory.restorePlatformFactory();
        __SAXParserFactory.restorePlatformFactory();
        __XMLEventFactory.restorePlatformFactory();
        __TransformerFactory.restorePlatformFactory();
        __XPathFactory.restorePlatformFactory();
        __XMLEventFactory.restorePlatformFactory();
        __XMLInputFactory.restorePlatformFactory();
        __XMLOutputFactory.restorePlatformFactory();
        __DatatypeFactory.restorePlatformFactory();
    }

    /**
     * Initializes the JAXP redirection system.
     */
    public static void initAll() {
        __DocumentBuilderFactory.init();
        __SAXParserFactory.init();
        __XMLEventFactory.init();
        __TransformerFactory.init();
        __XPathFactory.init();
        __XMLEventFactory.init();
        __XMLInputFactory.init();
        __XMLOutputFactory.init();
        __DatatypeFactory.init();
    }
}
