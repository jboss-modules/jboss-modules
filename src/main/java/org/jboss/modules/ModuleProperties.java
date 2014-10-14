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

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ModuleProperties extends Properties {

    private static final long serialVersionUID = 8027401722366934376L;

    private final Set<String> hiddenKeys = new HashSet<>();
    private Set<Object> keySet;
    private Collection<Object> values;
    private Set<Map.Entry<Object, Object>> entrySet;

    ModuleProperties() {
        super();
    }

    ModuleProperties(final Properties defaults) {
        super(defaults);
    }

    protected Properties getDefaults() {
        return defaults;
    }

    public String getProperty(final String key) {
        return super.getProperty(key);
    }

    public String getProperty(final String key, final String defaultValue) {
        return super.getProperty(key, defaultValue);
    }

    public Enumeration<?> propertyNames() {
        synchronized (this) {
            return Collections.enumeration(new ArrayList<>(keySet()));
        }
    }

    public Set<String> stringPropertyNames() {
        synchronized (this) {
            @SuppressWarnings("raw")
            final HashSet<String> set = new HashSet(super.keySet());
            set.removeAll(hiddenKeys);
            return Collections.synchronizedSet(set);
        }
    }

    ArrayList<String> stringPropertyNamesAsList() {
        synchronized (this) {
            @SuppressWarnings("raw")
            final ArrayList<String> list = new ArrayList(super.keySet());
            list.removeAll(hiddenKeys);
            return list;
        }
    }

    public int size() {
        synchronized (this) {
            return super.size();
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Enumeration<Object> keys() {
        return Collections.enumeration(keySet());
    }

    public Enumeration<Object> elements() {
        return Collections.enumeration(values());
    }

    public boolean contains(final Object value) {
        return containsValue(value);
    }

    public boolean containsValue(final Object value) {
        synchronized (this) {
            final Set<Map.Entry<Object, Object>> entries = super.entrySet();
            for (Map.Entry<Object, Object> entry : entries) {
                if (entry.getValue().equals(value) && ! hiddenKeys.contains(entry.getKey())) return true;
            }
        }
        return false;
    }

    public boolean containsKey(final Object key) {
        synchronized (this) {
            return super.containsKey(key) && ! hiddenKeys.contains(key);
        }
    }

    public Object get(final Object key) {
        synchronized (this) {
            return hiddenKeys.contains(key) ? null : super.get(key);
        }
    }

    public Object put(final Object key, final Object value) {
        synchronized (this) {
            if (key instanceof String && value instanceof String) {
                hiddenKeys.remove(key);
                return super.put(key, value);
            }
        }
        return null;
    }

    public Object remove(final Object key) {
        synchronized (this) {
            if (key instanceof String) {
                final Object removed = super.remove(key);
                if (removed == null) {
                    hiddenKeys.add((String) key);
                }
                return removed;
            }
        }
        return null;
    }

    public Set<Object> keySet() {
        synchronized (this) {
            Set<Object> keySet = this.keySet;
            if (keySet == null) {
                keySet = this.keySet = new KeySet();
            }
            return keySet;
        }
    }

    public Collection<Object> values() {
        synchronized (this) {
            Collection<Object> values = this.values;
            if (values == null) {
                values = this.values = new Values();
            }
            return values;
        }
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        synchronized (this) {
            Set<Map.Entry<Object, Object>> entrySet = this.entrySet;
            if (entrySet == null) {
                entrySet = this.entrySet = new EntrySet();
            }
            return entrySet;
        }
    }

    Iterator<Object> superKeySetIterator() {
        return super.keySet().iterator();
    }

    Iterator<Map.Entry<Object, Object>> superEntrySetIterator() {
        return super.entrySet().iterator();
    }

    static final Object NONE = new Object();

    class KeySet extends AbstractSet<Object> {

        public Iterator<Object> iterator() {
            final Iterator<Object> i = superKeySetIterator();
            return new Iterator<Object>() {
                Object next = NONE;
                boolean remove = false;

                public boolean hasNext() {
                    while (next == NONE) {
                        if (!i.hasNext()) {
                            return false;
                        }
                        remove = false;
                        final Object v = i.next();
                        if (! hiddenKeys.contains(v)) {
                            next = v;
                        }
                    }
                    return true;
                }

                public Object next() {
                    if (! hasNext()) {
                        throw new NoSuchElementException();
                    }
                    try {
                        remove = true;
                        return next;
                    } finally {
                        next = NONE;
                    }
                }

                public void remove() {
                    if (! remove) {
                        throw new IllegalStateException();
                    }
                    this.remove = false;
                    KeySet.this.remove(remove);
                }
            };
        }

        public boolean remove(final Object key) {
            synchronized (ModuleProperties.this) {
                if (key instanceof String) {
                    final Object removed = super.remove(key);
                    if (removed == null) {
                        return hiddenKeys.add((String) key);
                    } else {
                        return true;
                    }
                }
            }
            return false;
        }

        public int size() {
            return ModuleProperties.this.size();
        }
    }

    class Values extends AbstractCollection<Object> {

        public Iterator<Object> iterator() {
            final Iterator<Map.Entry<Object, Object>> i = superEntrySetIterator();
            return new Iterator<Object>() {
                Map.Entry<Object, Object> next = null;
                boolean remove = false;

                public boolean hasNext() {
                    while (next == null) {
                        if (!i.hasNext()) {
                            return false;
                        }
                        remove = false;
                        final Map.Entry<Object, Object> e = i.next();
                        if (! hiddenKeys.contains(e.getKey())) {
                            next = e;
                        }
                    }
                    return true;
                }

                public Object next() {
                    if (! hasNext()) {
                        throw new NoSuchElementException();
                    }
                    try {
                        remove = true;
                        return next;
                    } finally {
                        next = null;
                    }
                }

                public void remove() {
                    if (! remove) {
                        throw new IllegalStateException();
                    }
                    remove = false;
                    i.remove();
                }
            };
        }

        public boolean remove(final Object value) {
            synchronized (ModuleProperties.this) {
                if (value instanceof String) {
                    Iterator<Map.Entry<Object, Object>> i = superEntrySetIterator();
                    while (i.hasNext()) {
                        if (i.next().getValue().equals(value)) {
                            i.remove();
                            return true;
                        }
                    }
                    i = getDefaults().entrySet().iterator();
                    while (i.hasNext()) {
                        final Map.Entry<Object, Object> next = i.next();
                        if (next.getValue().equals(value) && next.getKey() instanceof String) {
                            hiddenKeys.add((String) next.getKey());
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public int size() {
            return ModuleProperties.this.size();
        }
    }

    class EntrySet extends AbstractSet<Map.Entry<Object, Object>> {

        public Iterator<Map.Entry<Object, Object>> iterator() {
            final Iterator<Map.Entry<Object, Object>> i = superEntrySetIterator();
            return new Iterator<Map.Entry<Object, Object>>() {
                Map.Entry<Object, Object> next = null;
                boolean remove = false;

                public boolean hasNext() {
                    while (next == null) {
                        if (!i.hasNext()) {
                            return false;
                        }
                        remove = false;
                        final Map.Entry<Object, Object> e = i.next();
                        if (! hiddenKeys.contains(e.getKey())) {
                            next = e;
                        }
                    }
                    return true;
                }

                public Map.Entry<Object, Object> next() {
                    if (! hasNext()) {
                        throw new NoSuchElementException();
                    }
                    try {
                        remove = true;
                        return next;
                    } finally {
                        next = null;
                    }
                }

                public void remove() {
                    if (! remove) {
                        throw new IllegalStateException();
                    }
                    remove = false;
                    i.remove();
                }
            };
        }

        public boolean remove(final Object value) {
            synchronized (ModuleProperties.this) {
                if (value instanceof String) {
                    Iterator<Map.Entry<Object, Object>> i = superEntrySetIterator();
                    while (i.hasNext()) {
                        if (i.next().getValue().equals(value)) {
                            i.remove();
                            return true;
                        }
                    }
                    i = getDefaults().entrySet().iterator();
                    while (i.hasNext()) {
                        final Map.Entry<Object, Object> next = i.next();
                        if (next.getValue().equals(value) && next.getKey() instanceof String) {
                            hiddenKeys.add((String) next.getKey());
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public int size() {
            return ModuleProperties.this.size();
        }
    }
}
