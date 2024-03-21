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
 * A cleaner for a dead object.
 *
 * @param <T> the reference type
 * @param <A> the reference attachment type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 *
 * @deprecated Use {@link java.lang.ref.Cleaner} or one of the reference types from a support library such as
 *      {@code io.smallrye.common:smallrye-common-ref} instead.
 */
@Deprecated
public interface Reaper<T, A> {

    /**
     * Perform the cleanup action for a reference.
     *
     * @param reference the reference
     */
    void reap(Reference<T, A> reference);
}
