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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jboss.modules.ref.Reference.Type;
import org.jboss.modules.ref.util.Assert;
import org.junit.Test;

/**
 * Super class for all reference test cases, contains a few tests for functionality common to all
 * references, and a reference assertion method.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public abstract class AbstractReferenceTest {

    abstract <T, A> Reference<T, A> createReference(T value, A attachment);
    abstract Type getTestedReferenceType();

    @Test
    public void referenceEqualsReference() {
        final Reference<StringBuffer, Number> reference1 = createReference(new StringBuffer().append("some text text"), (Number) Long.valueOf(5L));
        final Reference<StringBuffer, Number> reference2 = createReference(new StringBuffer().append("some text"), (Number) Double.valueOf(5.5));
        final Reference<StringBuffer, Number> reference3 = createReference(null, null);
        final Reference<StringBuffer, Number> reference4 = createReference(new StringBuffer().append("some text text"), (Number) Long.valueOf(5L));

        assertEquals(reference1, reference1);
        assertEquals(reference1.hashCode(), reference1.hashCode());
        assertFalse(reference1.equals(reference2));
        assertFalse(reference1.equals(reference3));
        assertFalse(reference1.equals(reference4));

        assertEquals(reference2, reference2);
        assertEquals(reference2.hashCode(), reference2.hashCode());
        assertFalse(reference2.equals(reference3));
        assertFalse(reference2.equals(reference4));

        assertEquals(reference3, reference3);
        assertEquals(reference3.hashCode(), reference3.hashCode());
        assertFalse(reference3.equals(reference4));

        assertEquals(reference4, reference4);
        assertEquals(reference4.hashCode(), reference4.hashCode());
    }

    @Test
    public void clearReference() {
        final Reference<Boolean, String> reference = createReference(Boolean.TRUE, "attachment for true");
        assertTrue(reference.get().booleanValue());
        assertEquals("attachment for true", reference.getAttachment());

        reference.clear();
        assertNull(reference.get());
        assertEquals("attachment for true", reference.getAttachment());
    }

    final <T, A> void assertReference(Reference<T, A> reference, T referent, A attachment) {
        Assert.assertReference(reference, referent, attachment, getTestedReferenceType());
    }
}
