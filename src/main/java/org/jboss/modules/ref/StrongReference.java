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
 * A strong reference with an attachment.  Since strong references are always reachable, a reaper may not be used.
 *
 * @param <T> the reference value type
 * @param <A> the attachment type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class StrongReference<T, A> implements Reference<T, A> {

    private volatile T value;
    private final A attachment;

    public StrongReference(final T value, final A attachment) {
        this.value = value;
        this.attachment = attachment;
    }

    public StrongReference(final T value) {
        this(value, null);
    }

    public T get() {
        return value;
    }

    public void clear() {
        value = null;
    }

    public A getAttachment() {
        return attachment;
    }

    public Type getType() {
        return Type.STRONG;
    }

    public String toString() {
        return "strong reference to " + String.valueOf(get());
    }
}
