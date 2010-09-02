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

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleIdentifier implements Serializable {

    private static final long serialVersionUID = 118533026624827995L;

    private static Pattern MODULE_NAME_PATTERN = Pattern.compile("[a-zA-Z_][-a-zA-Z0-9_]*(?:\\.[a-zA-Z0-9_][-a-zA-Z0-9_]*)*");
    private static Pattern SLOT_PATTERN = Pattern.compile("[-a-zA-Z0-9_+*.]+");
    private static String DEFAULT_SLOT = "main";

    private final String name;
    private final String slot;

    private int hash = 0;

    /**
     * The system module.
     */
    public static final ModuleIdentifier SYSTEM = new ModuleIdentifier("system", DEFAULT_SLOT);

    private ModuleIdentifier(final String name, final String slot) {
        this.name = name;
        this.slot = slot;
    }

    public String getName() {
        return name;
    }

    public String getSlot() {
        return slot;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof ModuleIdentifier && equals((ModuleIdentifier) o);
    }

    public boolean equals(final ModuleIdentifier o) {
        return this == o || o != null && name.equals(o.name) && slot.equals(o.slot);
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            int h = 17;
            h = 37 * h + name.hashCode();
            h = 37 * h + slot.hashCode();

            hash = h;
        }

        return hash;
    }

    @Override
    public String toString() {
        return "module:" + toSpecString();
    }

    public static ModuleIdentifier fromURL(URL url) throws MalformedURLException {
        if (url.getAuthority() != null) {
            throw new MalformedURLException("Modules cannot have an authority part");
        }
        final String moduleRootSpec = url.getFile();
        final int si = moduleRootSpec.indexOf('/');
        final String moduleName;
        if (si == -1) {
            moduleName = moduleRootSpec;
        } else {
            moduleName = moduleRootSpec.substring(0, si);
        }
        if (moduleName.length() == 0) {
            throw new MalformedURLException("Empty module URL");
        }
        return fromString(moduleName);
    }

    public static ModuleIdentifier fromURI(URI uri) throws URISyntaxException {
        final String scheme = uri.getScheme();
        if (scheme == null || ! scheme.equals("module")) {
            throw new URISyntaxException(uri.toString(), "Module URIs must start with \"module:\"");
        }
        if (uri.getAuthority() != null) {
            throw new URISyntaxException(uri.toString(), "Modules cannot have an authority part");
        }
        final String moduleFullSpec = uri.getSchemeSpecificPart();
        final int sli = moduleFullSpec.indexOf('/');
        final int qi = moduleFullSpec.indexOf('?');
        final int si = qi == -1 ? sli == -1 ? -1 : sli : sli == -1 ? qi : Math.min(sli, qi);
        final String moduleName;
        if (si == -1) {
            moduleName = moduleFullSpec;
        } else {
            moduleName = moduleFullSpec.substring(0, si);
        }
        if (moduleName.length() == 0) {
            throw new URISyntaxException(uri.toString(), "Empty module URI");
        }
        return fromString(moduleName);
    }

    public static ModuleIdentifier fromString(String moduleSpec) throws IllegalArgumentException {
        if (moduleSpec == null) {
            throw new IllegalArgumentException("Module specification is null");
        }
        if (moduleSpec.length() == 0) {
            throw new IllegalArgumentException("Empty module specificiation");
        }

        final int c1 = moduleSpec.lastIndexOf(':');
        final String name;
        final String slot;
        if (c1 != -1) {
            name = moduleSpec.substring(0, c1);
            slot = moduleSpec.substring(c1 + 1);

            if (!SLOT_PATTERN.matcher(name).matches()) {
                throw new IllegalArgumentException("Slot has invalid characters or is empty");
            }
        } else {
            name = moduleSpec;
            slot = DEFAULT_SLOT;
        }

        if (!MODULE_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Module name contains invalid characters, or empty segments");
        }

        return new ModuleIdentifier(name, slot);
    }

    private String toSpecString() {
        return name + ":" + slot;
    }

    /**
     * Creates a new module identifier using the specified name and slot.
     * A slot allows for multiple modules to exist with the same name.
     * The main usage pattern for this is to differentiate between
     * two incompatible release streams of a module.
     *
     * Normally all module definitions wind up in the "main" slot.
     * An unspecified or null slot will result in placement in the "main"
     * slot.
     *
     * Unless you have a true need for a slot, it should not be specified.
     * When in doubt use the {{@link #create(String)} method instead.
     *
     * @param name the name of the module
     * @param slot the slot this module belongs in
     * @return the identifier
     */
    public static ModuleIdentifier create(final String name, String slot) {
        if (name == null)
            throw new IllegalArgumentException("Name can not be null");

        if (slot == null)
            slot = DEFAULT_SLOT;

        return new ModuleIdentifier(name, slot);
    }

    /**
     * Creates a new module identifier using the specified name.
     *
     * @param name the name of the module
     * @return the identifier
     */
    public static ModuleIdentifier create(String name) {
        return create(name, null);
    }

    public URL toURL() throws MalformedURLException {
        return new URL("module", null, -1, toSpecString());
    }

    public URL toURL(String resourceRoot) throws MalformedURLException {
        if (resourceRoot == null) {
            return toURL();
        } else {
            return new URL("module", null, -1, toSpecString() + "/" + resourceRoot);
        }
    }

    public URL toURL(String resourceRoot, String resourceName) throws MalformedURLException {
        if (resourceName == null) {
            return toURL(resourceRoot);
        } else if (resourceRoot == null) {
            return new URL("module", null, -1, toSpecString() + "?/" + resourceName);
        } else {
            return new URL("module", null, -1, toSpecString() + "/" + resourceRoot + "?/" + resourceName);
        }
    }
}