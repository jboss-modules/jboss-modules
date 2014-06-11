/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
