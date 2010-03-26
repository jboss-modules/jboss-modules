/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.module;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class LocalModuleLoader extends ModuleLoader {

    private final String[] modulePaths;
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newFactory();

    public LocalModuleLoader(final String[] modulePaths) {
        super(null);
        this.modulePaths = modulePaths;
    }

    private static String[] fastSplit(String subject, char ch) {
        return fastSplit0(subject, ch, 0, 0);
    }

    private static String[] fastSplit0(String subject, char ch, int sidx, int idx) {
        int offs = subject.indexOf(ch, sidx);
        final String[] array;
        if (offs == -1) {
            array = new String[idx + 1];
            array[idx] = subject.substring(offs);
        } else {
            array = fastSplit0(subject, ch, offs + 1, idx + 1);
            array[idx] = subject.substring(sidx, offs);
        }
        return array;
    }

    protected Module findModule(URI uri) throws ModuleNotFoundException {
        final String moduleId = uri.getSchemeSpecificPart();
        final String[] parts = fastSplit(moduleId, ':');
        final String groupId;
        final String artifactId;
        final String slotId;
        final VersionFilter versionSpec;
        final int len = parts.length;
        switch (len) {
            case 0: return null;
            case 1: {
                groupId = "";
                artifactId = parts[0];
                versionSpec = VersionFilter.TRUE;
                slotId = null;
                break;
            }
            case 2: {
                groupId = parts[0];
                artifactId = parts[1];
                versionSpec = VersionFilter.TRUE;
                slotId = null;
                break;
            }
            case 3: {
                groupId = parts[0];
                artifactId = parts[1];
                // todo compile from parts[2]
                versionSpec = VersionFilter.TRUE;
                slotId = null;
                break;
            }
            case 4: {
                
            }
            default: {

            }
        }
        final String groupPath = groupId.replace('.', '/');
        final NavigableMap<Version, File> versions = new TreeMap<Version, File>();
        for (String modulePath : modulePaths) {
            final File groupDir = new File(modulePath, groupPath);
            if (! groupDir.isDirectory()) {
                // no such group
                continue;
            }
            // add all found versions.
            for (File file : groupDir.listFiles()) {
                if (! file.isDirectory()) {
                    continue;
                }
                final Version foundVersion = new Version(file.getName());
                if (versionSpec.matches(foundVersion)) {
                    versions.put(foundVersion, file);
                }
            }
        }
        for (Version version : versions.descendingKeySet()) {
            // try to open the given module
            final File file = versions.get(version);
            final File moduleInfo = new File(file, "module-info.xml");
            try {
                ModuleInfo info = parseModuleInfo(moduleInfo);
                return defineModule(uri, new JarLoader(new File(file, artifactId + "-" + version + ".jar")), info.getImports(), info.getExports(), ClassFilter.ALL, ClassFilter.ALL);
            } catch (XMLStreamException e) {
                continue;
            } catch (IOException e) {
                continue;
            }
        }
        return null;
    }

    private static final class ModuleInfo {
        private List<URI> imports;
        private List<URI> exports;

        public List<URI> getImports() {
            return imports;
        }

        public void setImports(final List<URI> imports) {
            this.imports = imports;
        }

        public List<URI> getExports() {
            return exports;
        }

        public void setExports(final List<URI> exports) {
            this.exports = exports;
        }
    }

    private static final String NAMESPACE = "urn:jboss:module:1.0";

    private static ModuleInfo parseModuleInfo(File moduleInfoFile) throws IOException, XMLStreamException {
        final XMLInputFactory inputFactory = XML_INPUT_FACTORY;
        final FileInputStream inputStream = new FileInputStream(moduleInfoFile);
        try {
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(inputStream);
            try {
                ModuleInfo moduleInfo = null;
                while (streamReader.hasNext()) {
                    switch (streamReader.next()) {
                        case XMLStreamConstants.START_ELEMENT: {
                            if (! NAMESPACE.equals(streamReader.getNamespaceURI()) || ! "module".equals(streamReader.getLocalName())) {
                                throw new XMLStreamException("Unexpected root element" + streamReader.getName(), streamReader.getLocation());
                            }
                            moduleInfo = parseModuleElement(streamReader);
                            break;
                        }
                        case XMLStreamConstants.END_DOCUMENT: {
                            break;
                        }
                    }
                }
                if (moduleInfo == null) {
                    throw new XMLStreamException("No <module> element found", streamReader.getLocation());
                }
                return moduleInfo;
            } finally {
                safeClose(streamReader);
            }
        } finally {
            safeClose(inputStream);
        }
    }



    private static void safeClose(final XMLStreamReader streamReader) {
        try {
            streamReader.close();
        } catch (Exception e) {
            //
        }
    }

    private static void safeClose(final Closeable c) {
        try {
            c.close();
        } catch (Exception e) {
            //
        }
    }

    private static void consumeElement(final XMLStreamReader streamReader) throws XMLStreamException {
        while (streamReader.hasNext()) {
            switch (streamReader.next()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    consumeElement(streamReader);
                    break;
                }
            }
        }
    }

    private static ModuleInfo parseModuleElement(final XMLStreamReader streamReader) throws XMLStreamException {
        final ModuleInfo moduleInfo = new ModuleInfo();
        final List<URI> imports = new ArrayList<URI>();
        final List<URI> exports = new ArrayList<URI>();
        moduleInfo.setImports(imports);
        moduleInfo.setExports(exports);
        while (streamReader.hasNext()) {
            switch (streamReader.next()) {
                case XMLStreamConstants.START_ELEMENT: {
                    if (NAMESPACE.equals(streamReader.getNamespaceURI())) {
                        if ("imports".equals(streamReader.getLocalName())) {
                            parseModulesElement(streamReader, imports);
                        } else if ("exports".equals(streamReader.getLocalName())) {
                            parseModulesElement(streamReader, exports);
                        } else {
                            throw new XMLStreamException("Unknown element " + streamReader.getName(), streamReader.getLocation());
                        }
                    } else {
                        // ignore unrecognized namespaces
                        consumeElement(streamReader);
                    }
                    break;
                }
                case XMLStreamConstants.END_ELEMENT: {
                    return moduleInfo;
                }
            }
        }
        throw new XMLStreamException("Unterminated element", streamReader.getLocation());
    }

    private static void parseModulesElement(final XMLStreamReader streamReader, final List<URI> imports) throws XMLStreamException {
        while (streamReader.hasNext()) {
            switch (streamReader.next()) {
                case XMLStreamConstants.START_ELEMENT: {
                    if (NAMESPACE.equals(streamReader.getNamespaceURI())) {
                        if ("module".equals(streamReader.getLocalName())) {
                            final String uri = streamReader.getAttributeValue(null, "uri");
                            if (uri == null) {
                                throw new XMLStreamException("Required attribute 'uri' is missing", streamReader.getLocation());
                            }
                            imports.add(URI.create(uri));
                            consumeElement(streamReader);
                        } else {
                            throw new XMLStreamException("Unknown element " + streamReader.getName(), streamReader.getLocation());
                        }
                    } else {
                        // ignore unrecognized namespaces
                        consumeElement(streamReader);
                    }
                    break;
                }
            }
        }
        throw new XMLStreamException("Unterminated element", streamReader.getLocation());
    }
}
