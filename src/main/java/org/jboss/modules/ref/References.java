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
 * A set of utility methods for reference types.
 */
public final class References {
    @SuppressWarnings({ "RawUseOfParameterizedType" })
    private static final Reference NULL = new Reference() {
        public Object get() {
            return null;
        }

        public Object getAttachment() {
            return null;
        }

        public void clear() {
        }

        public Type getType() {
            return Type.NULL;
        }

        public String toString() {
            return "NULL reference";
        }
    };

    private References() {
    }

    static final class ReaperThread extends Thread {
        static final ReferenceQueue<Object> REAPER_QUEUE = new ReferenceQueue<Object>();

        static {
            final ReaperThread thr = new ReaperThread();
            thr.setName("Reference Reaper");
            thr.setDaemon(true);
            thr.start();
        }

        public void run() {
            for (;;) try {
                final java.lang.ref.Reference<? extends Object> ref = REAPER_QUEUE.remove();
                if (ref instanceof Reapable) {
                    reap((Reapable<?, ?>) ref);
                }
            } catch (InterruptedException ignored) {
            } catch (Throwable ignored) {
            }
        }

        @SuppressWarnings({ "unchecked" })
        private static <T, A> void reap(final Reapable<T, A> reapable) {
            reapable.getReaper().reap((Reference<T, A>) reapable);
        }
    }

    /**
     * Create a reference of a given type with the provided value and attachment.  If the reference type is
     * {@link Reference.Type#STRONG} or {@link Reference.Type#NULL} then the reaper argument is ignored.  If
     * the reference type is {@link Reference.Type#NULL} then the value and attachment arguments are ignored.
     *
     * @param type the reference type
     * @param value the reference value
     * @param attachment the attachment value
     * @param reaper the reaper to use, if any
     * @param <T> the reference value type
     * @param <A> the reference attachment type
     * @return the reference
     */
    public static <T, A> Reference<T, A> create(Reference.Type type, T value, A attachment, Reaper<T, A> reaper) {
        switch (type) {
            case STRONG:
                return new StrongReference<T, A>(value, attachment);
            case WEAK:
                return new WeakReference<T, A>(value, attachment, reaper);
            case PHANTOM:
                return new PhantomReference<T, A>(value, attachment, reaper);
            case SOFT:
                return new SoftReference<T, A>(value, attachment, reaper);
            case NULL:
                return getNullReference();
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Create a reference of a given type with the provided value and attachment.  If the reference type is
     * {@link Reference.Type#STRONG} or {@link Reference.Type#NULL} then the reference queue argument is ignored.  If
     * the reference type is {@link Reference.Type#NULL} then the value and attachment arguments are ignored.
     *
     * @param type the reference type
     * @param value the reference value
     * @param attachment the attachment value
     * @param referenceQueue the reference queue to use, if any
     * @param <T> the reference value type
     * @param <A> the reference attachment type
     * @return the reference
     */
    public static <T, A> Reference<T, A> create(Reference.Type type, T value, A attachment, ReferenceQueue<? super T> referenceQueue) {
        switch (type) {
            case STRONG:
                return new StrongReference<T, A>(value, attachment);
            case WEAK:
                return new WeakReference<T, A>(value, attachment, referenceQueue);
            case PHANTOM:
                return new PhantomReference<T, A>(value, attachment, referenceQueue);
            case SOFT:
                return new SoftReference<T, A>(value, attachment, referenceQueue);
            case NULL:
                return getNullReference();
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Create a reference of a given type with the provided value and attachment.  If the reference type is
     * {@link Reference.Type#PHANTOM} then this method will throw an {@code IllegalArgumentException} because
     * such references are not constructable without a queue or reaper.  If the reference type is
     * {@link Reference.Type#NULL} then the value and attachment arguments are ignored.
     *
     * @param type the reference type
     * @param value the reference value
     * @param attachment the attachment value
     * @param <T> the reference value type
     * @param <A> the reference attachment type
     * @return the reference
     * @throws IllegalArgumentException if the reference type is {@link Reference.Type#PHANTOM}
     */
    public static <T, A> Reference<T, A> create(Reference.Type type, T value, A attachment) throws IllegalArgumentException {
        switch (type) {
            case STRONG:
                return new StrongReference<T, A>(value, attachment);
            case WEAK:
                return new WeakReference<T, A>(value, attachment);
            case PHANTOM:
                throw new IllegalArgumentException("Phantom reference may not be created without a queue or reaper");
            case SOFT:
                return new SoftReference<T, A>(value, attachment);
            case NULL:
                return getNullReference();
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Get a null reference.  This reference type is always cleared and does not retain an attachment; as such
     * there is only one single instance of it.
     *
     * @param <T> the reference value type
     * @param <A> the attachment value type
     * @return the null reference
     */
    @SuppressWarnings({ "unchecked" })
    public static <T, A> Reference<T, A> getNullReference() {
        return (Reference<T, A>) NULL;
    }
}
