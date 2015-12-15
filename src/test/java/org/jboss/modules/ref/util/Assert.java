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

package org.jboss.modules.ref.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.jboss.modules.ref.Reaper;
import org.jboss.modules.ref.Reference;
import org.jboss.modules.ref.Reference.Type;

/**
 * Assertion methods used by reference tests.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class Assert {

    /**
     * Assert that {@code reference} contains {@code referent}, {@code attachment}, and that its type is {@code type}.
     *
     * @param <T>         the type of referent
     * @param <A>         the type of attachment
     * @param reference   the reference that needs to be checked
     * @param referent    the expected content for {@code reference}
     * @param attachment  the expected attachment contained in {@code reference}
     * @param type        the expected type of {@code reference}
     */
    public static final <T, A> void assertReference(Reference<T, A> reference, T referent, A attachment, Type type) {
        assertEquals(referent, reference.get());
        assertEquals(attachment, reference.getAttachment());
        assertSame(type, reference.getType());
        assertNotNull(reference.toString()); // make sure that no exception is thrown and that null is never returned
    }

    /**
     * Assert that {@code reference} contains {@code referent}, {@code attachment}, and that its type is {@code type}.
     * Also asserts that {@code reaper} is the reaper used by {@code referent}.
     * <p>
     * Call this method only for {@link org.jboss.msc.ref.Reapable Reapable} references. This method assumes that
     * {@code reference} has a method called {@code getReaper} that will return the reaper.
     *
     * @param <T>         the type of referent
     * @param <A>         the type of attachment
     * @param reference   the {@code Reapable} reference that needs to be checked
     * @param referent    the expected content for {@code reference}
     * @param attachment  the expected attachment contained in {@code reference}
     * @param type        the expected type of {@code reference}
     * @param reaper      the expected reaper
     */
    public static final <T, A> void assertReference(Reference<T, A> reference, T referent, A attachment, Type type, Reaper<T, A> reaper) {
        assertReference(reference, referent, attachment, type);
        try {
            assertEquals(reaper, reference.getClass().getMethod("getReaper").invoke(reference));
        } catch (NoSuchMethodException e) {
            fail("Can't find getReaper method at clas " + reference.getClass());
        } catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
