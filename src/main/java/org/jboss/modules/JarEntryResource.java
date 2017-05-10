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

package org.jboss.modules;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JarEntryResource implements Resource {
    private final JarFile jarFile;
    private final String entryName;
    private final URL resourceURL;

    JarEntryResource(final JarFile jarFile, final String name, final String relativePath, final URL resourceURL) {
        this.jarFile = jarFile;
        this.entryName = relativePath == null ? name : name.substring(relativePath.length() + 1);
        this.resourceURL = resourceURL;
    }

    public String getName() {
        return entryName;
    }

    public URL getURL() {
        return resourceURL;
    }

    public InputStream openStream() throws IOException {
        return jarFile.getInputStream(jarFile.getEntry(entryName));
    }

    public long getSize() {
        final long size = jarFile.getEntry(entryName).getSize();
        return size == -1 ? 0 : size;
    }
}
