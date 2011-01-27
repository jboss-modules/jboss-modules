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

package org.jboss.modules;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An identity based hash set. A number of properties apply to this set. It
 * compares only using object identity, it supports null entries, it allocates
 * little more than a single object array, and it can be copied quickly. If the
 * copy-ctor is passed another IdentityHashSet, or clone is called on this set,
 * the shallow copy can be performed using little more than a single array copy.
 *
 * Note: It is very important to use a smaller load factor than you normally
 * would for HashSet, since the implementation is open-addressed with linear
 * probing. With a 50% load-factor a get is expected to return in only 2 probes.
 * However, a 90% load-factor is expected to return in around 50 probes.
 *
 * @author Jason T. Greene
 */
class IdentityHashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, Serializable {

    /**
     * Serialization ID
     */
    private static final long serialVersionUID = 10929568968762L;

    /**
     * Same default as HashMap, must be a power of 2
     */
    private static final int DEFAULT_CAPACITY = 8;

    /**
     * MAX_INT - 1
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 67%, just like IdentityHashMap
     */
    private static final float DEFAULT_LOAD_FACTOR = 0.67f;

    /**
     * The open-addressed table
     */
    private transient Object[] table;

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

    public IdentityHashSet(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Can not have a negative size table!");

        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        if (!(loadFactor > 0F && loadFactor <= 1F))
            throw new IllegalArgumentException("Load factor must be greater than 0 and less than or equal to 1");

        this.loadFactor = loadFactor;
        init(initialCapacity, loadFactor);
    }

    @SuppressWarnings("unchecked")
    public IdentityHashSet(Set<? extends E> set) {
        if (set instanceof IdentityHashSet) {
            IdentityHashSet<? extends E> fast = (IdentityHashSet<? extends E>) set;
            table = fast.table.clone();
            loadFactor = fast.loadFactor;
            size = fast.size;
            threshold = fast.threshold;
        } else {
            loadFactor = DEFAULT_LOAD_FACTOR;
            init(set.size(), loadFactor);
            addAll(set);
        }
    }

    private void init(int initialCapacity, float loadFactor) {
        int c = 1;
        for (; c < initialCapacity; c <<= 1);
        threshold = (int) (c * loadFactor);

        // Include the load factor when sizing the table for the first time
        if (initialCapacity > threshold && c < MAXIMUM_CAPACITY) {
            c <<= 1;
            threshold = (int) (c * loadFactor);
        }

        table = new Object[c];
    }

    public IdentityHashSet(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public IdentityHashSet() {
        this(DEFAULT_CAPACITY);
    }

    // The normal bit spreader...
    private static int hash(Object o) {
        int h = System.identityHashCode(o);
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
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

    public boolean contains(Object entry) {
        if (entry == null) return false;

        int hash = hash(entry);
        int length = table.length;
        int index = index(hash, length);

        for (int start = index;;) {
            Object e = table[index];
            if (e == null)
                return false;

            if (entry == e)
                return true;

            index = nextIndex(index, length);
            if (index == start) // Full table
                return false;
        }
    }

    public boolean add(E entry) {
        if (entry == null) {
            throw new NullPointerException("entry is null");
        }

        Object[] table = this.table;
        int hash = hash(entry);
        int length = table.length;
        int index = index(hash, length);

        for (int start = index;;) {
            Object e = table[index];
            if (e == null)
                break;

            if (e == entry)
                return false;

            index = nextIndex(index, length);
            if (index == start)
                throw new IllegalStateException("Table is full!");
        }

        modCount++;
        table[index] = entry;
        if (++size >= threshold)
            resize(length);

        return true;
    }

    private void resize(int from) {
        int newLength = from << 1;

        // Can't get any bigger
        if (newLength > MAXIMUM_CAPACITY || newLength <= from)
            return;

        Object[] newTable = new Object[newLength];
        Object[] old = table;

        for (Object e : old) {
            if (e == null)
                continue;

            int index = index(hash(e), newLength);
            while (newTable[index] != null)
                index = nextIndex(index, newLength);

            newTable[index] = e;
        }

        threshold = (int) (loadFactor * newLength);
        table = newTable;
    }

    @SuppressWarnings({ "unchecked" })
    public boolean addAll(Collection<? extends E> collection) {
        int size = collection.size();
        if (size == 0)
            return false;

        if (size > threshold) {
            if (size > MAXIMUM_CAPACITY)
                size = MAXIMUM_CAPACITY;

            int length = table.length;
            for (; length < size; length <<= 1);

            resize(length);
        }

        boolean state = false;

        if (collection instanceof IdentityHashSet) {
            for (E e : ((E[]) (((IdentityHashSet<?>) collection).table)))
                if (e != null) state |= add(e);
        } else {
            for (E e : collection)
                state |= add(e);
        }

        return state;
    }

    public boolean remove(Object o) {
        if (o == null) return false;

        Object[] table = this.table;
        int length = table.length;
        int hash = hash(o);
        int start = index(hash, length);

        for (int index = start;;) {
            Object e = table[index];
            if (e == null)
                return false;

            if (e == o) {
                table[index] = null;
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
        Object[] table = this.table;
        int length = table.length;
        int current = nextIndex(start, length);

        for (;;) {
            Object e = table[current];
            if (e == null)
                return;

            // A Doug Lea variant of Knuth's Section 6.4 Algorithm R.
            // This provides a non-recursive method of relocating
            // entries to their optimal positions once a gap is created.
            int prefer = index(hash(e), length);
            if ((current < prefer && (prefer <= start || start <= current)) || (prefer <= start && start <= current)) {
                table[start] = e;
                table[current] = null;
                start = current;
            }

            current = nextIndex(current, length);
        }
    }

    public void clear() {
        modCount++;
        Object[] table = this.table;
        for (int i = 0; i < table.length; i++)
            table[i] = null;

        size = 0;
    }

    @SuppressWarnings("unchecked")
    public IdentityHashSet<E> clone() {
        try {
            IdentityHashSet<E> clone = (IdentityHashSet<E>) super.clone();
            clone.table = table.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            // should never happen
            throw new IllegalStateException(e);
        }
    }

    /**
     * Advanced method that returns a copy of the internal table. The resulting
     * array will contain nulls at random places that must be skipped. In
     * addition, it will not operate correctly if a null was inserted into the
     * set. Use at your own risk....
     *
     * @return an array containing elements in this set along with randomly
     *         placed nulls,
     */
    @SuppressWarnings({ "unchecked" })
    public E[] toScatteredArray(E[] dummy) {
        final E[] ret = (E[]) Array.newInstance(dummy.getClass().getComponentType(), table.length);
        System.arraycopy((E[])table, 0, ret, 0, ret.length);

        return ret;
    }

    /**
     * Warning: this will crap out if the set contains a {@code null}.
     *
     * @param target the target to write to
     * @param offs the offset into the target
     * @param len the length to write
     * @return the target array
     */
    @SuppressWarnings({ "unchecked" })
    public E[] toArray(final E[] target, final int offs, final int len) {
        assert len <= size;
        final E[] table = (E[]) this.table;
        E e;
        final int last = offs + len;
        for (int i = offs, j = 0; i < last; j ++) {
            e = table[j];
            if (e != null) {
                target[i++] = e;
            }
        }
        return target;
    }

    public void printDebugStats() {
        int optimal = 0;
        int total = 0;
        int totalSkew = 0;
        int maxSkew = 0;
        for (int i = 0; i < table.length; i++) {
            Object e = table[i];
            if (e != null) {

                total++;
                int target = index(hash(e), table.length);
                if (i == target)
                    optimal++;
                else {
                    int skew = Math.abs(i - target);
                    if (skew > maxSkew)
                        maxSkew = skew;
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
            putForCreate((E) s.readObject());
        }

        this.size = size;
    }

    private void putForCreate(E entry) {

        Object[] table = this.table;
        int hash = hash(entry);
        int length = table.length;
        int index = index(hash, length);

        Object e = table[index];
        while (e != null) {
            index = nextIndex(index, length);
            e = table[index];
        }

        table[index] = entry;
    }

    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(size);

        for (Object e : table) {
            if (e != null) {
                s.writeObject(e);
            }
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new IdentityHashSetIterator();
    }

    private class IdentityHashSetIterator implements Iterator<E> {
        private int next = 0;
        private int expectedCount = modCount;
        private int current = -1;
        private boolean hasNext;
        Object table[] = IdentityHashSet.this.table;

        public boolean hasNext() {
            if (hasNext == true)
                return true;

            Object table[] = this.table;
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
        public E next() {
            if (modCount != expectedCount)
                throw new ConcurrentModificationException();

            if (!hasNext && !hasNext())
                throw new NoSuchElementException();

            current = next++;
            hasNext = false;

            return (E) table[current];
        }

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

            Object[] table = this.table;
            if (table != IdentityHashSet.this.table) {
                IdentityHashSet.this.remove(table[delete]);
                table[delete] = null;
                expectedCount = modCount;
                return;
            }

            int length = table.length;
            int i = delete;

            table[delete] = null;
            size--;

            for (;;) {
                i = nextIndex(i, length);
                Object e = table[i];
                if (e == null)
                    break;

                int prefer = index(hash(e), length);
                if ((i < prefer && (prefer <= delete || delete <= i)) || (prefer <= delete && delete <= i)) {
                    // Snapshot the unseen portion of the table if we have
                    // to relocate an entry that was already seen by this
                    // iterator
                    if (i < current && current <= delete && table == IdentityHashSet.this.table) {
                        int remaining = length - current;
                        Object[] newTable = new Object[remaining];
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
