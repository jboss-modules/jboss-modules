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

package org.jboss.module.protocol.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.regex.Pattern;
import org.jboss.module.Module;
import org.jboss.module.ModuleLoader;
import org.jboss.module.ModuleNotFoundException;
import org.jboss.module.Resource;

/**
 * The handler for "module:" URL types.  The URL format is:<br/>
 * <pre><code>        module:groupId:moduleName[:slot][/root/name][?path/to/resource]</code></pre>
 */
public final class Handler extends URLStreamHandler {

    private static final Pattern PATTERN = Pattern.compile(
            "([-_0-9a-zA-Z]+(?:\\.[-_0-9a-zA-Z]+)*)" +
            ":([-_0-9a-zA-Z]+)" +
            "(?::([-_0-9a-zA-Z]+))?" +
            "((?:/[-_0-9a-zA-Z%]+)+)?/?" +
            "(?:\\?([-_0-9a-zA-Z%]+))");

    protected void parseURL(final URL u, final String spec, final int start, final int limit) {
        String moduleSpec = "";
        String resourcePath = "";
        String rootPath = "";
        

        setURL(u, "module", null, -1, null, null, moduleSpec + rootPath, resourcePath, null);
    }

    protected String toExternalForm(final URL u) {
        StringBuilder b = new StringBuilder(64);
        b.append(u.getProtocol()).append(':').append(u.getPath());
        final String q = u.getQuery();
        if (q != null) {
            b.append('?').append(q);
        }
        return b.toString();
    }

    protected URLConnection openConnection(final URL u) throws IOException {
        final URI uri;
        try {
            uri = u.toURI();
        } catch (URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        }
        final String file = uri.getPath();
        final ModuleLoader moduleLoader = ModuleLoader.getCurrent();
        final int idx = file.indexOf('/');
        final String moduleName;
        final String path;
        final String resourcePath = uri.getQuery();
        if (idx == -1) {
            moduleName = file;
            path = "";
        } else {
            moduleName = file.substring(0, idx);
            int e;
            for (e = file.length(); e > 0 && file.charAt(e) == '/'; e--);
            path = file.substring(idx + 1, e);
        }
        if (moduleName.length() == 0) {
            throw new IOException("No module specified");
        }
        final Module module;
        try {
            module = moduleLoader.loadModule(URI.create(moduleName));
        } catch (ModuleNotFoundException e) {
            throw new IOException("No such module '" + moduleName + "'", e);
        }
        final Resource resource;
        if (path.length() == 0) {
            resource = module.getExportedResource(resourcePath);
        } else {
            resource = module.getExportedResource(path, resourcePath);
        }
        return new ResourceConnection(resource, u);
    }

    private class ResourceConnection extends URLConnection {

        private final Resource resource;

        public ResourceConnection(final Resource resource, final URL moduleUrl) {
            super(moduleUrl);
            this.resource = resource;
        }

        public void connect() throws IOException {
        }

        public InputStream getInputStream() throws IOException {
            return resource.openStream();
        }

        public int getContentLength() {
            final long size = resource.getSize();
            return size > (long) Integer.MAX_VALUE || size < 1L ? -1 : (int) size;
        }
    }
}
