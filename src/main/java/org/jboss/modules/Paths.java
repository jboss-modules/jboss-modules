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

package org.jboss.modules;

import java.util.List;
import java.util.Map;

/**
 * A pair of path maps.
 *
 * @param <T> the type of object that each path refers to
 * @param <A> the type of the source object used to calculate the path maps
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class Paths<T, A> {
    private final A[] sourceList;
    private final Map<String, List<T>> allPaths;

    Paths(final A[] sourceList, final Map<String, List<T>> allPaths) {
        this.sourceList = sourceList;
        this.allPaths = allPaths;
    }

    Map<String, List<T>> getAllPaths() {
        return allPaths;
    }

    A[] getSourceList(A[] defVal) {
        final A[] sourceList = this.sourceList;
        return sourceList == null ? defVal : sourceList;
    }

    static final Paths<?, ?> NONE = new Paths<Object, Object>(null, null);

    @SuppressWarnings({ "unchecked" })
    static <T, A> Paths<T, A> none() {
        return (Paths<T, A>) NONE;
    }
}
