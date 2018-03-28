/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 */
final class ListCache {
    private static final LocalLoader[] NO_OBJECTS = new LocalLoader[0];

    private final LocalLoader[] items;
    private volatile Map<LocalLoader, ListCache> nextMap = Collections.emptyMap();

    @SuppressWarnings("unchecked")
    ListCache() {
        this.items = NO_OBJECTS;
    }

    private ListCache(final ListCache parent, final LocalLoader next) {
        final LocalLoader[] parentItems = parent.getItems();
        final LocalLoader[] items = Arrays.copyOf(parentItems, parentItems.length + 1);
        items[parentItems.length] = next;
        this.items = items;
    }

    ListCache getChild(LocalLoader next) {
        Map<LocalLoader, ListCache> nextMap = this.nextMap;
        ListCache child = nextMap.get(next);
        if (child == null) {
            synchronized (this) {
                nextMap = this.nextMap;
                child = nextMap.get(next);
                if (child == null) {
                    child = new ListCache(this, next);
                    if (nextMap.isEmpty()) {
                        nextMap = Collections.singletonMap(next, child);
                    } else {
                        nextMap = new HashMap<>(nextMap);
                        nextMap.put(next, child);
                    }
                    this.nextMap = nextMap;
                }
            }
        }
        return child;
    }

    LocalLoader[] getItems() {
        return items;
    }
}
