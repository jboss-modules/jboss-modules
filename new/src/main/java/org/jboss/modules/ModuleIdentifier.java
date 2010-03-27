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
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleIdentifier implements Serializable {

    private static final long serialVersionUID = 118533026624827995L;

    private final String group;
    private final String artifact;
    private final String version;

    public ModuleIdentifier(final String group, final String artifact, final String version) {
        if (group == null) {
            throw new IllegalArgumentException("group is null");
        }
        if (artifact == null) {
            throw new IllegalArgumentException("artifact is null");
        }
        if (version == null) {
            throw new IllegalArgumentException("version is null");
        }
        this.group = group;
        this.artifact = artifact;
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof ModuleIdentifier && equals((ModuleIdentifier) o);
    }

    public boolean equals(final ModuleIdentifier o) {
        return this == o || o != null && group.equals(o.group) && artifact.equals(o.artifact) && version.equals(o.version);
    }

    @Override
    public int hashCode() {
        int result = group != null ? group.hashCode() : 0;
        result = 31 * result + (artifact != null ? artifact.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("module:%s:%s:%s", group, artifact, version);
    }

    public ModuleIdentifier fromURL(URL url) throws MalformedURLException {
        if (url.getAuthority() != null) {
            throw new MalformedURLException("Modules cannot have an authority part");
        }
        final String moduleRootSpec = url.getFile();
        final int si = moduleRootSpec.indexOf('/');
        final String moduleSpec;
        if (si == -1) {
            moduleSpec = moduleRootSpec;
        } else {
            moduleSpec = moduleRootSpec.substring(0, si);
        }
        final int c1 = moduleSpec.indexOf(':');
        if (c1 == -1) {
            throw new MalformedURLException("")
        }
    }

    public ModuleIdentifier fromURI(URI uri) throws URISyntaxException {
        final String scheme = uri.getScheme();
        if (scheme == null || ! scheme.equals("module")) {
            throw new URISyntaxException(uri.toString(), "Module URIs must start with \"module:\"");
        }
        if (uri.getAuthority() != null) {
            throw new URISyntaxException(uri.toString(), "Modules cannot have an authority part");
        }
        if (uri.isAbsolute())
    }
}