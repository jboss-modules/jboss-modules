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

import java.lang.ref.ReferenceQueue;

/**
 * A reapable soft reference with an attachment.  If a {@link Reaper} is given, then it will be used to asynchronously
 * clean up the referent.
 *
 * @param <T> the reference value type
 * @param <A> the attachment type
 *
 * @see java.lang.ref.SoftReference
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 *
 * @deprecated Use {@link java.lang.ref.Cleaner} or one of the reference types from a support library such as
 *      {@code io.smallrye.common:smallrye-common-ref} instead.
 */
@Deprecated
public class SoftReference<T, A> extends java.lang.ref.SoftReference<T> implements Reference<T, A>, Reapable<T, A> {
    private final A attachment;
    private final Reaper<T, A> reaper;

    public SoftReference(final T referent) {
        this(referent, null, (ReferenceQueue<T>) null);
    }

    public SoftReference(final T referent, final A attachment) {
        this(referent, attachment, (ReferenceQueue<T>) null);
    }

    public SoftReference(final T referent, final A attachment, final ReferenceQueue<? super T> q) {
        super(referent, q);
        reaper = null;
        this.attachment = attachment;
    }

    public SoftReference(final T referent, final A attachment, final Reaper<T, A> reaper) {
        super(referent, References.ReaperThread.REAPER_QUEUE);
        this.reaper = reaper;
        this.attachment = attachment;
    }

    public Reaper<T, A> getReaper() {
        return reaper;
    }

    public A getAttachment() {
        return attachment;
    }

    public Type getType() {
        return Type.SOFT;
    }

    public String toString() {
        return "soft reference to " + String.valueOf(get());
    }
}
