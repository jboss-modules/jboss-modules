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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class JarEntryResource extends Resource {

    private final JarFile jarFile;
    private final JarEntry jarEntry;

    JarEntryResource(final JarFile jarFile, final JarEntry jarEntry) {
        this.jarFile = jarFile;
        this.jarEntry = jarEntry;
    }

    public String getName() {
        return jarEntry.getName();
    }

    public InputStream openStream() throws IOException {
        return jarFile.getInputStream(jarEntry);
    }

    public long getSize() {
        return jarEntry.getSize();
    }

    public URL getURL() {
        final String base = jarFile.getName();
        try {
            return new URL("jar:file:" + base + "!/" + jarEntry.getName());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Cannot get URL for resource", e);
        }
    }
}
