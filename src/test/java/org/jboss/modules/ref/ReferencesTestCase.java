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

import static org.jboss.modules.ref.util.Assert.assertReference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.jboss.modules.ref.Reference.Type;
import org.jboss.modules.ref.References.ReaperThread;
import org.jboss.modules.ref.util.TestReaper;
import org.junit.Test;

/**
 * Test for {@link References} class and internal classes.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ReferencesTestCase {

    @Test
    public void nullReference() {
        final Reference<?, ?> nullReference = References.getNullReference();
        assertReference(nullReference, null, null, Type.NULL);
        nullReference.clear();
        assertReference(nullReference, null, null, Type.NULL);
        assertEquals(nullReference, References.create(Type.NULL, null, null));
        assertEquals(nullReference, References.create(Type.NULL, null, null, (Reaper<Object, Object>) null));
        assertEquals(nullReference, References.create(Type.NULL, null, null, (ReferenceQueue<Object>) null));
    }

    @Test
    public void createStrongReference() {
        final Reference<String, String> reference = createReference(Type.STRONG);
        assertTrue(reference instanceof StrongReference);
    }

    @Test
    public void createSoftReference() {
        final Reference<String, String> reference = createReference(Type.SOFT);
        assertTrue(reference instanceof SoftReference);
    }

    @Test
    public void createWeakReference() {
        final Reference<String, String> reference = createReference(Type.WEAK);
        assertTrue(reference instanceof WeakReference);
    }

    @Test
    public void createIllegalPhantomReference() {
        try {
            References.create(Type.PHANTOM, "value", "attachment");
            fail("IllegalArgumentException expected because of missing reaper/reference queue");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void createStrongReferenceWithReaper() throws Exception {
        final Object referent = new Object();
        final Reference<Object, String> reference = References.create(Type.STRONG, referent, "attachment",
                new TestReaper<Object, String>());
        assertReference(reference, referent, "attachment", Type.STRONG);
        assertTrue(reference instanceof StrongReference);
    }

    @Test
    public void createSoftReferenceWithReaper() throws Exception {
        final Reference<Object, String> reference = createReferenceWithReaper(Type.SOFT, false, false);
        assertTrue(reference instanceof SoftReference);
    }

    @Test
    public void createWeakReferenceWithReaper() throws Exception {
        final Reference<Object, String> reference = createReferenceWithReaper(Type.WEAK, false, true);
        assertTrue(reference instanceof WeakReference);
    }

    @Test
    public void createPhantomReferenceWithReaper() throws Exception {
        final Reference<Object, String> reference = createReferenceWithReaper(Type.PHANTOM, true, true);
        assertTrue(reference instanceof PhantomReference);
    }

    @Test
    public void createStrongReferenceWithQueue() throws Exception {
        final Object referent = new Object();
        final Reference<Object, String> reference = References.create(Type.STRONG, referent, "attachment", new ReferenceQueue<Object>());
        assertReference(reference, referent, "attachment", Type.STRONG);
        assertTrue(reference instanceof StrongReference);
    }

    @Test
    public void createSoftReferenceWithQueue() throws Exception {
        final Reference<?, ?> reference = createReferenceWithQueue(Type.SOFT, false, false);
        assertTrue(reference instanceof SoftReference);
    }

    @Test
    public void createWeakReferenceWithQueue() throws Exception {
        final Reference<?, ?> reference = createReferenceWithQueue(Type.WEAK, false, true);
        assertTrue(reference instanceof WeakReference);
    }

    @Test
    public void createPhantomReferenceWithQueue() throws Exception {
        final Reference<?, ?> reference = createReferenceWithQueue(Type.PHANTOM, true, true);
        assertTrue(reference instanceof PhantomReference);
    }

    @Test
    public void reapUnreapableReference() throws Exception {
        Object referent = new Object();
        final TestReaper<Object, Void> reaper = new TestReaper<Object, Void>();
        final Reference<Object, Void> reference = new UnreapableWeakReference(referent);
        assertReference(reference, referent, null, Type.WEAK);
        referent = null;
        System.gc();
        assertNull(reaper.get(200, TimeUnit.MILLISECONDS));
    }

    @Test
    public void failToReapReference() throws Exception {
        final TestReaper<Object, Void> reaper = new FailToReap<Object, Void>();
        Object referent = new Object();
        final Reference<Object, Void> reference = new WeakReference<Object, Void>(referent, null, reaper);
        assertReference(reference, referent, null, Type.WEAK, reaper);
        referent = null;
        System.gc();
        assertNull(reaper.get(200, TimeUnit.MILLISECONDS));
    }

    private Reference<String, String> createReference(Type type) {
        final Reference<String, String> reference = References.create(type, "value", "attachment");
        assertReference(reference, "value", "attachment", type);
        return reference;
    }

    private Reference<Object, String> createReferenceWithReaper(Type type, boolean expectedNullValue, boolean testReaper) throws Exception {
        Object referent = new Object();
        final TestReaper<Object, String> reaper = new TestReaper<Object, String>();
        final Reference<Object, String> reference = References.create(type, referent, "attachment", reaper);
        assertReference(reference, expectedNullValue? null: referent, "attachment", type, reaper);
        if (testReaper) {
            referent = null;
            System.gc();
            assertSame(reference, reaper.get());
        }
        return reference;
    }

    private Reference<Collection<Integer>, Object> createReferenceWithQueue(Type type, boolean expectedNullValue, boolean testQueue) throws Exception {
        final Object referenceAttachment = new Object();
        Collection<Integer> referent = new ArrayList<Integer>();
        referent.add(Integer.valueOf(1));
        referent.add(Integer.valueOf(11));
        referent.add(Integer.valueOf(111));
        final ReferenceQueue<Collection<Integer>> referenceQueue = new ReferenceQueue<Collection<Integer>>();
        final Reference<Collection<Integer>, Object> reference = References.create(type, referent, referenceAttachment, referenceQueue);
        assertReference(reference, expectedNullValue? null: referent, referenceAttachment, type, null);
        if (testQueue) {
            referent = null;
            System.gc();
            assertSame(reference, referenceQueue.remove(300));
        }
        return reference;
    }

    private static final class UnreapableWeakReference extends java.lang.ref.WeakReference<Object> implements Reference<Object, Void> {

        UnreapableWeakReference(Object referent) {
            super(referent, ReaperThread.REAPER_QUEUE);
        }

        @Override
        public Void getAttachment() {
            return null;
        }

        @Override
        public Reference.Type getType() {
            return Type.WEAK;
        }
    }

    private static final class FailToReap<T, A> extends TestReaper<T, A> {
        @Override
        public void reap(Reference<T, A> reference) {
            throw new RuntimeException("fail to reap");
        }
    }
}
