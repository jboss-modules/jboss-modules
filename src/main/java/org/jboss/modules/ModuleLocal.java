/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.modules.ref.Reaper;
import org.jboss.modules.ref.Reference;
import org.jboss.modules.ref.WeakReference;

/**
 * A module-local variable.  Used to attach values to modules without creating a strong
 * reference from to the module, or from the module to the key.  The module keeps a strong
 * reference to the value only.  It is important that module-local values do not keep strong references
 * to their keys, otherwise the keys may not be garbage-collected in a timely fashion.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleLocal<T> {

    private final int hashCode = System.identityHashCode(this);

    /**
     * Determine whether this module local is contained in the given module.
     *
     * @param module the module
     * @return {@code true} if this module local is contained therein
     */
    public boolean containedIn(Module module) {
        if (module == null) {
            throw moduleIsNull();
        }
        return module.containsModuleLocal(this);
    }

    /**
     * Get the module local value for this key.
     *
     * @param module the module
     * @return the module local value (may be {@code null}) or {@code null} if none is set
     */
    public T get(Module module) {
        if (module == null) {
            throw moduleIsNull();
        }
        return (T) module.getModuleLocal(this);
    }

    /**
     * Set the module local value for this key.  Setting a {@code null} value does <b>not</b> remove
     * the key.
     *
     * @param module the module
     * @param value the value to set
     * @return the previously set value (may be {@code null}) or {@code null} if none was set
     */
    public T put(Module module, T value) {
        if (module == null) {
            throw moduleIsNull();
        }
        return (T) module.putModuleLocal(new Ref(this, module), value);
    }

    /**
     * Set the module local value for this key if it was not already set.  Setting a {@code null} value does <b>not</b>
     * remove the key.
     *
     * @param module the module
     * @param value the value to set
     * @return the previously set value (may be {@code null}) or {@code null} if the value was not set
     */
    public T putIfAbsent(Module module, T value) {
        if (module == null) {
            throw moduleIsNull();
        }
        return (T) module.putModuleLocalIfAbsent(new Ref(this, module), value);
    }

    /**
     * Remove the module local value for this key.
     *
     * @param module the module
     * @return the removed value
     */
    public T remove(Module module) {
        if (module == null) {
            throw moduleIsNull();
        }
        return (T) module.removeModuleLocal(this);
    }

    /**
     * Remove the module local value for this key if the value equals an expected value.
     *
     * @param module the module
     * @param expectedValue the value to test for
     * @return {@code true} if the value was equal and was removed, {@code false} otherwise
     */
    public boolean remove(Module module, T expectedValue) {
        if (module == null) {
            throw moduleIsNull();
        }
        return module.removeModuleLocal(this, expectedValue);
    }

    /**
     * Replace the module local value for this key with another value if the original value equals an expected value.
     *
     * @param module the module
     * @param expectedValue the value to test for
     * @param replacementValue the new value to set
     * @return {@code true} if the value was equal and was replaced, {@code false} otherwise
     */
    public boolean replace(Module module, T expectedValue, T replacementValue) {
        if (module == null) {
            throw moduleIsNull();
        }
        return module.replaceModuleLocal(this, expectedValue, replacementValue);
    }

    /**
     * Replace the module local value for this key if there was a previous value set.
     *
     * @param module the module
     * @param newValue the new value to set
     * @return the previous value (may be {@code null}) or {@code null} if the value was not present
     */
    public T replace(Module module, T newValue) {
        if (module == null) {
            throw moduleIsNull();
        }
        return (T) module.replaceModuleLocal(this, newValue);
    }

    /**
     * The hash code for this module local key.
     *
     * @return the hash code
     */
    public int hashCode() {
        return hashCode;
    }

    /**
     * Determine if this module local key is equal to the given object.
     *
     * @param obj the object to test
     * @return {@code true} if they are equal
     */
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof Ref && equals((Ref) obj);
    }

    boolean equals(final Ref obj) {
        return this == obj.get();
    }

    private static IllegalArgumentException moduleIsNull() {
        return new IllegalArgumentException("module is null");
    }

    static final class Ref extends WeakReference<ModuleLocal<?>, Module> {
        private final int hashCode;

        Ref(final ModuleLocal<?> value, Module module) {
            super(value, module, REAPER);
            this.hashCode = value.hashCode();
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(final Object obj) {
            return obj == this || obj instanceof ModuleLocal && equals((ModuleLocal<?>) obj);
        }

        boolean equals(final ModuleLocal<?> obj) {
            return get() == obj;
        }
    }

    private static final Reaper<ModuleLocal<?>, Module> REAPER = new Reaper<ModuleLocal<?>, Module>() {
        public void reap(final Reference<ModuleLocal<?>, Module> reference) {
            reference.getAttachment().removeModuleLocal(reference);
        }
    };
}
