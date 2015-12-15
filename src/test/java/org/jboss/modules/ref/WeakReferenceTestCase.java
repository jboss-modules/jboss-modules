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

import static org.junit.Assert.assertSame;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;

import org.jboss.modules.ref.Reference.Type;
import org.jboss.modules.ref.util.TestReaper;
import org.junit.Test;

/**
 * Test for {@link WeakReference}.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class WeakReferenceTestCase extends AbstractReapableReferenceTest {

    @Test
    public void plainWeakReference() {
        final Reference<String, String> reference = new WeakReference<String, String>("referent", "attachment");
        assertReference(reference, "referent", "attachment", null);
    }

    @Test
    public void weakReferenceWithoutAttachment() {
        final Reference<String, String> reference = new WeakReference<String, String>("referent");
        assertReference(reference, "referent", null, null);
    }

    @Test
    public void nullWeakReference() throws Exception {
        final Reference<Thing, Integer> reference = new WeakReference<Thing, Integer>(null, Integer.valueOf(0));
        assertReference(reference, null, Integer.valueOf(0));
    }

    @Test
    public void weakReferenceWithReferenceQueue() throws Exception {
        final ReferenceQueue<Collection<Object>> referenceQueue = new ReferenceQueue<Collection<Object>>();
        Collection<Object> collection = new ArrayList<Object>();
        final Reference<Collection<Object>, String> reference = new WeakReference<Collection<Object>, String>(collection, "collection", referenceQueue);
        assertReference(reference, collection, "collection", null);
        collection = null;
        System.gc();
        assertSame(reference, referenceQueue.remove(300));
    }

    @Test
    public void weakReferenceWithReaper() throws Exception {
        Thing thing = new Thing();
        final TestReaper<Thing, Void> reaper = new TestReaper<Thing, Void>();
        final Reference<Thing, Void> reference = new WeakReference<Thing, Void>(thing, null, reaper);
        assertReference(reference, thing, null, reaper);
        thing = null;
        System.gc();
        assertSame(reference, reaper.getReapedReference());
    }

    @Override
    <T, A> Reference<T, A> createReference(T value, A attachment) {
        return new WeakReference<T, A>(value, attachment);
    }

    @Override
    Type getTestedReferenceType() {
        return Type.WEAK;
    }
}