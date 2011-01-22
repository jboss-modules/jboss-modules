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

package org.jboss.modules.ref;

/**
 * An enhanced reference type with a type-safe attachment.
 *
 * @param <T> the reference value type
 * @param <A> the attachment type
 *
 * @see java.lang.ref.Reference
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface Reference<T, A> {

    /**
     * Get the value, or {@code null} if the reference has been cleared.
     *
     * @return the value
     */
    T get();

    /**
     * Get the attachment, if any.
     *
     * @return the attachment
     */
    A getAttachment();

    /**
     * Clear the reference.
     */
    void clear();

    /**
     * Get the type of the reference.
     *
     * @return the type
     */
    Type getType();

    /**
     * A reference type.
     *
     * @apiviz.exclude
     */
    enum Type {

        /**
         * A strong reference.
         */
        STRONG,
        /**
         * A weak reference.
         */
        WEAK,
        /**
         * A phantom reference.
         */
        PHANTOM,
        /**
         * A soft reference.
         */
        SOFT,
        /**
         * A {@code null} reference.
         */
        NULL,
    }
}
