/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.modules;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * The handler for "module:" URL types.  The URL format is:<br/>
 * <pre><code>        module:groupId:moduleName[:slot][/root/name][?path/to/resource]</code></pre>
 */
final class ModuleProtocolHandler extends URLStreamHandler {

    protected URLConnection openConnection(final URL u) throws IOException {
        final ResourceIdentifier identifier = ResourceIdentifier.fromURL(u);
        try {
            return new ResourceConnection(Module.getModule(identifier.getModuleIdentifier()).getExportedResource(identifier.getRoot(), identifier.getPath()), u);
        } catch (ModuleLoadException e) {
            throw new IOException("Cannot connect", e);
        }
    }

    private class ResourceConnection extends URLConnection {

        private Resource resource;

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