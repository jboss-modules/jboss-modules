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

package org.jboss.modules.ref;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;

import org.jboss.modules.ref.Reference.Type;
import org.jboss.modules.ref.util.TestReaper;
import org.junit.Test;

/**
 * Test for {@link SoftReference}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class SoftReferenceTestCase extends AbstractReapableReferenceTest {

    @Test
    public void plainSoftReference() {
        final Reference<String, String> reference = new SoftReference<String, String>("referent", "attachment");
        assertReference(reference, "referent", "attachment", null);
    }

    @Test
    public void softReferenceWithoutAttachment() {
        final Reference<String, String> reference = new SoftReference<String, String>("referent");
        assertReference(reference, "referent", null, null);
    }

    @Test
    public void nullSoftReference() throws Exception {
        final Reference<Thing, Integer> reference = new SoftReference<Thing, Integer>(null, Integer.valueOf(0));
        assertReference(reference, null, Integer.valueOf(0), null);
    }

    @Test
    public void softReferenceWithReferenceQueue() throws Exception {
        final ReferenceQueue<Collection<Object>> referenceQueue = new ReferenceQueue<Collection<Object>>();
        final Collection<Object> collection = new ArrayList<Object>();
        final Reference<Collection<Object>, String> reference = new SoftReference<Collection<Object>, String>(collection, "collection", referenceQueue);
        assertReference(reference, collection, "collection", null);
    }

    @Test
    public void softReferenceWithReaper() throws Exception {
        final Thing thing = new Thing();
        final TestReaper<Thing, Void> reaper = new TestReaper<Thing, Void>();
        final Reference<Thing, Void> reference = new SoftReference<Thing, Void>(thing, null, reaper);
        assertReference(reference, thing, null, reaper);
    }

    @Override
    <T, A> Reference<T, A> createReference(T value, A attachment) {
        return new SoftReference<T, A>(value, attachment);
    }

    @Override
    Type getTestedReferenceType() {
        return Type.SOFT;
    }
}