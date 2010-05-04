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

package org.jboss.modules;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ResourceIdentifier implements Serializable {
    private static final long serialVersionUID = -2181205071871484104L;

    private final ModuleIdentifier moduleIdentifier;
    private final String root;
    private final String path;

    public ResourceIdentifier(final ModuleIdentifier moduleIdentifier, final String root, final String path) {
        if (moduleIdentifier == null) {
            throw new IllegalArgumentException("mI is null");
        }
        if (root == null) {
            throw new IllegalArgumentException("root is null");
        }
        if (path == null) {
            throw new IllegalArgumentException("path is null");
        }
        this.moduleIdentifier = moduleIdentifier;
        this.root = root; // todo normalize
        this.path = path; // todo normalize
    }

    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    public String getRoot() {
        return root;
    }

    public String getPath() {
        return path;
    }

    public boolean equals(final Object o) {
        return o instanceof ResourceIdentifier && equals((ResourceIdentifier) o);
    }

    public boolean equals(final ResourceIdentifier o) {
        return o == this || o != null && moduleIdentifier.equals(o.moduleIdentifier) && root.equals(o.root) && path.equals(o.path);
    }

    public int hashCode() {
        int result = moduleIdentifier.hashCode();
        result = 31 * result + root.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    public static ResourceIdentifier fromURL(URL url) throws MalformedURLException {
        final ModuleIdentifier moduleIdentifier = ModuleIdentifier.fromURL(url);
        final String moduleRootSpec = url.getPath();
        final int si = moduleRootSpec.indexOf('/');
        final String rootSpec;
        if (si == -1) {
            rootSpec = "";
        } else {
            rootSpec = moduleRootSpec.substring(si + 1);
        }
        final String pathSpec = url.getQuery();
        return new ResourceIdentifier(moduleIdentifier, rootSpec, pathSpec == null ? "" : pathSpec);
    }

    public static ResourceIdentifier fromURI(URI uri) throws URISyntaxException {
        final ModuleIdentifier moduleIdentifier = ModuleIdentifier.fromURI(uri);
        final String moduleFullSpec = uri.getSchemeSpecificPart();
        final int si = moduleFullSpec.indexOf('/');
        final int qi = moduleFullSpec.indexOf('?');
        final String rootSpec;
        final String pathSpec;
        if (qi == -1) {
            if (si == -1) {
                rootSpec = "";
                pathSpec = "";
            } else {
                rootSpec = moduleFullSpec.substring(si + 1);
                pathSpec = "";
            }
        } else {
            if (si == -1 || si > qi) {
                rootSpec = "";
                pathSpec = moduleFullSpec.substring(qi + 1);
            } else {
                rootSpec = moduleFullSpec.substring(si + 1, qi);
                pathSpec = moduleFullSpec.substring(qi + 1);
            }
        }
        return new ResourceIdentifier(moduleIdentifier, rootSpec, pathSpec);
    }

    public URL toURL() throws MalformedURLException {
        return moduleIdentifier.toURL(root, path);
    }

    public String toString() {
        return moduleIdentifier.toString() + "/" + root + "?" + path;
    }
}
