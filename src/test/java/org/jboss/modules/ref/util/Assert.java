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
