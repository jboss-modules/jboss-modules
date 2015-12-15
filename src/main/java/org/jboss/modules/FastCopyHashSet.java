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

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A HashSet that is optimized for fast shallow copies. If the copy-ctor is
 * passed another FastCopyHashSet, or clone is called on this set, the shallow
 * copy can be performed using little more than a single array copy. In order to
 * accomplish this, immutable objects must be used internally, so update
 * operations result in slightly more object churn than <code>HashSet</code>.
 * <p/>
 * Note: It is very important to use a smaller load factor than you normally
 * would for HashSet, since the implementation is open-addressed with linear
 * probing. With a 50% load-factor a get is expected to return in only 2 probes.
 * However, a 90% load-factor is expected to return in around 50 probes.
 *
 * @author Jason T. Greene
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class FastCopyHashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, Serializable {

    /**
     * Serialization ID
     */
    private static final long serialVersionUID = 10929568968762L;

    /**
     * Same default as HashMap, must be a power of 2
     */
    private static final int DEFAULT_CAPACITY = 64;

    /**
     * MAX_INT - 1
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 50%
     */
    private static final float DEFAULT_LOAD_FACTOR = 0x0.5p0f;

    /**
     * The open-addressed table
     */
    private transient E[] table;

    /**
     * The current number of key-value pairs
     */
    private transient int size;

    /**
     * The next resize
     */
    private transient int threshold;

    /**
     * The user defined load factor which defines when to resize
     */
    private final float loadFactor;

    /**
     * Counter used to detect changes made outside of an iterator
     */
    private transient int modCount;

    /**
     * Accumulated hash code
     */
    private transient int hashCode;

    FastCopyHashSet(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Can not have a negative size table!");

        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        if (!(loadFactor > 0F && loadFactor <= 1F))
            throw new IllegalArgumentException("Load factor must be greater than 0 and less than or equal to 1");

        this.loadFactor = loadFactor;
        init(initialCapacity, loadFactor);
    }

    FastCopyHashSet(Set<? extends E> set) {
        if (set instanceof FastCopyHashSet) {
            FastCopyHashSet<? extends E> fast = (FastCopyHashSet<? extends E>) set;
            table = fast.table.clone();
            loadFactor = fast.loadFactor;
            size = fast.size;
            threshold = fast.threshold;
            hashCode = fast.hashCode;
        } else {
            loadFactor = DEFAULT_LOAD_FACTOR;
            init(set.size(), loadFactor);
            addAll(set);
        }
    }

    @SuppressWarnings("unchecked")
    private void init(int initialCapacity, float loadFactor) {
        int c = 1;
        while (c < initialCapacity) c <<= 1;
        threshold = (int) (c * loadFactor);

        // Include the load factor when sizing the table for the first time
        if (initialCapacity > threshold && c < MAXIMUM_CAPACITY) {
            c <<= 1;
            threshold = (int) (c * loadFactor);
        }

        table = (E[]) new Object[c];
    }

    FastCopyHashSet(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    FastCopyHashSet() {
        this(DEFAULT_CAPACITY);
    }

    private int nextIndex(int index, int length) {
        index = (index >= length - 1) ? 0 : index + 1;
        return index;
    }

    private static int index(int hashCode, int length) {
        return hashCode & (length - 1);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(Object key) {
        if (key == null) {
            return false;
        }

        int hash = key.hashCode();
        int length = table.length;
        int index = index(hash, length);

        for (int start = index; ;) {
            E e = table[index];
            if (e == null)
                return false;

            if (key.equals(e))
                return true;

            index = nextIndex(index, length);
            if (index == start) // Full table
                return false;
        }
    }

    public boolean add(E key) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }

        E[] table = this.table;
        int hash = key.hashCode();
        int length = table.length;
        int index = index(hash, length);

        boolean f = false;
        for (int start = index; ;) {
            E e = table[index];
            if (e == null)
                break;

            if (! f) {
                f= true;
            }
            if (key.equals(e)) {
                return false;
            }

            index = nextIndex(index, length);
            if (index == start)
                throw new IllegalStateException("Table is full!");
        }

        modCount++;
        table[index] = key;
        hashCode += key.hashCode();
        if (++size >= threshold)
            resize(length);

        return true;
    }


    @SuppressWarnings("unchecked")
    private void resize(int from) {
        int newLength = from << 1;

        // Can't get any bigger
        if (newLength > MAXIMUM_CAPACITY || newLength <= from)
            return;

        E[] newTable = (E[]) new Object[newLength];
        E[] old = table;

        for (E e : old) {
            if (e == null)
                continue;

            int index = index(e.hashCode(), newLength);
            while (newTable[index] != null)
                index = nextIndex(index, newLength);

            newTable[index] = e;
        }

        threshold = (int) (loadFactor * newLength);
        table = newTable;
    }

    public boolean addAll(Collection<? extends E> set) {
        int size = set.size();
        if (size == 0)
            return false;

        boolean changed = false;

        for (E e : set) {
            if (add(e)) {
                changed = true;
            }
        }
        return changed;
    }

    public boolean remove(Object key) {
        E[] table = this.table;
        int length = table.length;
        int hash = key.hashCode();
        int start = index(hash, length);

        for (int index = start; ;) {
            E e = table[index];
            if (e == null)
                return false;

            if (key.equals(e)) {
                table[index] = null;
                hashCode -= hash;
                relocate(index);
                modCount++;
                size--;
                return true;
            }

            index = nextIndex(index, length);
            if (index == start)
                return false;
        }
    }

    private void relocate(int start) {
        E[] table = this.table;
        int length = table.length;
        int current = nextIndex(start, length);

        for (; ;) {
            E e = table[current];
            if (e == null)
                return;

            // A Doug Lea variant of Knuth's Section 6.4 Algorithm R.
            // This provides a non-recursive method of relocating
            // entries to their optimal positions once a gap is created.
            int prefer = index(e.hashCode(), length);
            if ((current < prefer && (prefer <= start || start <= current))
                    || (prefer <= start && start <= current)) {
                table[start] = e;
                table[current] = null;
                start = current;
            }

            current = nextIndex(current, length);
        }
    }

    public void clear() {
        modCount++;
        E[] table = this.table;
        for (int i = 0; i < table.length; i++)
            table[i] = null;

        size = hashCode = 0;
    }

    @SuppressWarnings("unchecked")
    public FastCopyHashSet<E> clone() {
        try {
            FastCopyHashSet<E> clone = (FastCopyHashSet<E>) super.clone();
            clone.table = table.clone();
            return clone;
        }
        catch (CloneNotSupportedException e) {
            // should never happen
            throw new IllegalStateException(e);
        }
    }

    public Iterator<E> iterator() {
        return new KeyIterator();
    }

    public void printDebugStats() {
        int optimal = 0;
        int total = 0;
        int totalSkew = 0;
        int maxSkew = 0;
        for (int i = 0; i < table.length; i++) {
            E e = table[i];
            if (e != null) {

                total++;
                int target = index(e.hashCode(), table.length);
                if (i == target)
                    optimal++;
                else {
                    int skew = Math.abs(i - target);
                    if (skew > maxSkew) maxSkew = skew;
                    totalSkew += skew;
                }

            }
        }

        System.out.println(" Size:             " + size);
        System.out.println(" Real Size:        " + total);
        System.out.println(" Optimal:          " + optimal + " (" + (float) optimal * 100 / total + "%)");
        System.out.println(" Average Distance: " + ((float) totalSkew / (total - optimal)));
        System.out.println(" Max Distance:     " + maxSkew);
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        int size = s.readInt();

        init(size, loadFactor);

        for (int i = 0; i < size; i++) {
            E key = (E) s.readObject();
            putForCreate(key);
        }

        this.size = size;
    }

    @SuppressWarnings("unchecked")
    private void putForCreate(E key) {
        E[] table = this.table;
        int hash = key.hashCode();
        int length = table.length;
        int index = index(hash, length);

        E e = table[index];
        while (e != null) {
            index = nextIndex(index, length);
            e = table[index];
        }

        table[index] = key;
    }

    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(size);

        for (E e : table) {
            if (e != null) {
                s.writeObject(e);
            }
        }
    }

    public boolean containsAll(final Collection<?> c) {
        final E[] table = this.table;
        for (E e : table) {
            if (e != null) {
                if (! c.contains(e)) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals")
    public boolean equals(final Object o) {
        if (o == this)
            return true;

        if (! (o instanceof Set))
            return false;
        if (o instanceof FastCopyHashSet) {
            final FastCopyHashSet<?> set = (FastCopyHashSet<?>) o;
            if (hashCode != set.hashCode) {
                return false;
            }
            if (table.length == set.table.length) {
                return Arrays.equals(table, set.table);
            }
        }
        Set<?> set = (Set<?>) o;
        if (set.size() != size())
            return false;
        try {
            return containsAll(set);
        } catch (ClassCastException unused)   {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    public int hashCode() {
        return hashCode;
    }

    public Object[] getRawArray() {
        return table;
    }

    private class KeyIterator implements Iterator<E> {

        private int next = 0;
        private int expectedCount = modCount;
        private int current = -1;
        private boolean hasNext;
        private E[] table = FastCopyHashSet.this.table;

        public E next() {
            if (modCount != expectedCount)
                throw new ConcurrentModificationException();

            if (!hasNext && !hasNext())
                throw new NoSuchElementException();

            current = next++;
            hasNext = false;

            return table[current];
        }

        public boolean hasNext() {
            if (hasNext == true)
                return true;

            E[] table = this.table;
            for (int i = next; i < table.length; i++) {
                if (table[i] != null) {
                    next = i;
                    return hasNext = true;
                }
            }

            next = table.length;
            return false;
        }

        @SuppressWarnings("unchecked")
        public void remove() {
            if (modCount != expectedCount)
                throw new ConcurrentModificationException();

            int current = this.current;
            int delete = current;

            if (current == -1)
                throw new IllegalStateException();

            // Invalidate current (prevents multiple remove)
            this.current = -1;

            // Start were we relocate
            next = delete;

            E[] table = this.table;
            if (table != FastCopyHashSet.this.table) {
                FastCopyHashSet.this.remove(table[delete]);
                table[delete] = null;
                expectedCount = modCount;
                return;
            }


            int length = table.length;
            int i = delete;

            table[delete] = null;
            size--;

            for (; ;) {
                i = nextIndex(i, length);
                E e = table[i];
                if (e == null)
                    break;

                int prefer = index(e.hashCode(), length);
                if ((i < prefer && (prefer <= delete || delete <= i))
                        || (prefer <= delete && delete <= i)) {
                    // Snapshot the unseen portion of the table if we have
                    // to relocate an entry that was already seen by this iterator
                    if (i < current && current <= delete && table == FastCopyHashSet.this.table) {
                        int remaining = length - current;
                        E[] newTable = (E[]) new Object[remaining];
                        System.arraycopy(table, current, newTable, 0, remaining);

                        // Replace iterator's table.
                        // Leave table local var pointing to the real table
                        this.table = newTable;
                        next = 0;
                    }

                    // Do the swap on the real table
                    table[delete] = e;
                    table[i] = null;
                    delete = i;
                }
            }
        }
    }
}
