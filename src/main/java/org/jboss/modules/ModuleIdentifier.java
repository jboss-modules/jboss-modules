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
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.regex.Pattern;

/**
 * A unique identifier for a module within a module loader.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jason T. Greene
 *
 * @apiviz.landmark
 */
public final class ModuleIdentifier implements Serializable {

    private static final long serialVersionUID = 118533026624827995L;

    private static Pattern MODULE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_](?:[-a-zA-Z0-9_]*[a-zA-Z0-9_])?(?:\\.[a-zA-Z0-9_](?:[-a-zA-Z0-9_]*[a-zA-Z0-9_])?)*");
    private static Pattern SLOT_PATTERN = Pattern.compile("[-a-zA-Z0-9_+*.]+");
    private static String DEFAULT_SLOT = "main";

    private final String name;
    private final String slot;

    private final transient int hashCode;

    private static final Field hashField;

    static {
        hashField = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            public Field run() {
                final Field field;
                try {
                    field = ModuleIdentifier.class.getDeclaredField("hashCode");
                    field.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    throw new NoSuchFieldError(e.getMessage());
                }
                return field;
            }
        });
    }

    /**
     * The system module.
     */
    public static final ModuleIdentifier SYSTEM = new ModuleIdentifier("system", DEFAULT_SLOT);

    private ModuleIdentifier(final String name, final String slot) {
        this.name = name;
        this.slot = slot;
        hashCode = calculateHashCode(name, slot);
    }

    private static int calculateHashCode(final String name, final String slot) {
        int h = 17;
        h = 37 * h + name.hashCode();
        h = 37 * h + slot.hashCode();
        return h;
    }

    /**
     * Get the module name.
     *
     * @return the module name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the module version slot.
     *
     * @return the version slot
     */
    public String getSlot() {
        return slot;
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(Object other) {
        return other instanceof ModuleIdentifier && equals((ModuleIdentifier)other);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(ModuleIdentifier other) {
        return this == other || other != null && name.equals(other.name) && slot.equals(other.slot);
    }

    /**
     * Determine the hash code of this module identifier.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Get the string representation of this module identifier.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return name + ":" + slot;
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        try {
            hashField.setInt(this, calculateHashCode(name, slot));
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    /**
     * Parse a module specification from a string.
     *
     * @param moduleSpec the specification string
     * @return the module identifier
     * @throws IllegalArgumentException if the format of the module specification is invalid or it is {@code null}
     */
    public static ModuleIdentifier fromString(String moduleSpec) throws IllegalArgumentException {
        if (moduleSpec == null) {
            throw new IllegalArgumentException("Module specification is null");
        }
        if (moduleSpec.length() == 0) {
            throw new IllegalArgumentException("Empty module specification");
        }

        final int c1 = moduleSpec.lastIndexOf(':');
        final String name;
        final String slot;
        if (c1 != -1) {
            name = moduleSpec.substring(0, c1);
            slot = moduleSpec.substring(c1 + 1);

            if (!SLOT_PATTERN.matcher(slot).matches()) {
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
}