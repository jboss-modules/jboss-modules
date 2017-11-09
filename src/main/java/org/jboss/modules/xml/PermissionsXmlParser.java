/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

package org.jboss.modules.xml;

import static org.jboss.modules.xml.XmlPullParser.END_TAG;
import static org.jboss.modules.xml.XmlPullParser.START_TAG;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jboss.modules.ModuleLoader;
import org.jboss.modules.security.FactoryPermissionCollection;
import org.jboss.modules.security.ModularPermissionFactory;
import org.jboss.modules.security.PermissionFactory;

/**
 * A simple parser for enterprise-style {@code permissions.xml} files.
 */
public final class PermissionsXmlParser {

    private static final PermissionFactory[] NO_PERMISSION_FACTORIES = new PermissionFactory[0];

    /**
     * Parse the {@code permissions.xml} stream content.
     *
     * @param inputStream the input stream
     * @param moduleLoader the module loader to load from
     * @param moduleName the module name to load from
     * @return the permission collection (not {@code null})
     * @throws IOException if the input stream throws an exception
     * @throws XmlPullParserException if the XML parser throws an exception
     */
    public static FactoryPermissionCollection parsePermissionsXml(final InputStream inputStream, ModuleLoader moduleLoader, final String moduleName) throws XmlPullParserException, IOException {
        final MXParser mxParser = new MXParser();
        mxParser.setInput(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        return parsePermissionsXml(mxParser, moduleLoader, moduleName);
    }

    /**
     * Parse the {@code permissions.xml} content.
     *
     * @param reader the parser
     * @param moduleLoader the module loader to load from
     * @param moduleName the module name to load from
     * @return the permission collection (not {@code null})
     * @throws IOException if the input stream throws an exception
     * @throws XmlPullParserException if the XML parser throws an exception
     */
    public static FactoryPermissionCollection parsePermissionsXml(final XmlPullParser reader, ModuleLoader moduleLoader, final String moduleName) throws IOException, XmlPullParserException {
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case START_TAG: {
                    validateNamespace(reader);
                    switch (reader.getName()) {
                        case "permissions": {
                            return parsePermissionsElement(reader, moduleLoader, moduleName);
                        }
                        default: {
                            throw ModuleXmlParser.unexpectedContent(reader);
                        }
                    }
                }
                case END_TAG: {
                    return new FactoryPermissionCollection();
                }
            }
        }
    }

    private static void validateNamespace(final XmlPullParser reader) throws XmlPullParserException {
        final String namespace = reader.getNamespace();
        if (namespace != null) {
            switch (namespace) {
                case "http://xmlns.jcp.org/xml/ns/javaee":
                case "http://java.sun.com/xml/ns/javaee":
                case "http://java.sun.com/xml/ns/j2ee":
                case "http://java.sun.com/dtd":
                    return;
            }
            throw ModuleXmlParser.unexpectedContent(reader);
        }
    }

    private static FactoryPermissionCollection parsePermissionsElement(final XmlPullParser reader, final ModuleLoader moduleLoader, final String moduleName) throws IOException, XmlPullParserException {
        final int attributeCount = reader.getAttributeCount();
        for (int i = 0; i < attributeCount; i ++) {
            if (! reader.getAttributeNamespace(i).isEmpty() || ! reader.getAttributeName(i).equals("version")) {
                throw ModuleXmlParser.unknownAttribute(reader, i);
            }
        }
        final List<PermissionFactory> factories = new ArrayList<>();
        int eventType;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case START_TAG: {
                    validateNamespace(reader);
                    switch (reader.getName()) {
                        case "permission": {
                            factories.add(parsePermissionElement(reader, moduleLoader, moduleName));
                            break;
                        }
                        default: {
                            throw ModuleXmlParser.unexpectedContent(reader);
                        }
                    }
                }
                case END_TAG: {
                    return new FactoryPermissionCollection(factories.toArray(NO_PERMISSION_FACTORIES));
                }
            }
        }
    }

    private static PermissionFactory parsePermissionElement(final XmlPullParser reader, final ModuleLoader moduleLoader, final String moduleName) throws IOException, XmlPullParserException {
        if (reader.getAttributeCount() > 0) throw ModuleXmlParser.unknownAttribute(reader, 0);
        int eventType;
        String className = null;
        String name = null;
        String actions = null;
        for (;;) {
            eventType = reader.nextTag();
            switch (eventType) {
                case START_TAG: {
                    validateNamespace(reader);
                    switch (reader.getName()) {
                        case "class-name": {
                            if (className != null || name != null || actions != null) {
                                throw ModuleXmlParser.unexpectedContent(reader);
                            }
                            className = reader.nextText().trim();
                            break;
                        }
                        case "name": {
                            if (className == null || name != null || actions != null) {
                                throw ModuleXmlParser.unexpectedContent(reader);
                            }
                            name = PolicyExpander.expand(reader.nextText().trim());
                            break;
                        }
                        case "actions": {
                            if (className == null || actions != null) {
                                throw ModuleXmlParser.unexpectedContent(reader);
                            }
                            actions = reader.nextText().trim();
                            break;
                        }
                        default: {
                            throw ModuleXmlParser.unexpectedContent(reader);
                        }
                    }
                }
                case END_TAG: {
                    return new ModularPermissionFactory(moduleLoader, moduleName, className, name, actions);
                }
            }
        }
    }
}
