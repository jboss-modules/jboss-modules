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

import org.jboss.modules.ref.Reference.Type;
import org.junit.Test;

/**
 * Test for {@link StrongReference}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class StrongReferenceTestCase extends AbstractReferenceTest{

    @Test
    public void plainStrongReference() {
        final Reference<String, String> reference = new StrongReference<String, String>("referent", "attachment");
        assertReference(reference, "referent", "attachment");
    }

    @Test
    public void noAttachmentReference() {
        final Reference<String, Void> reference = new StrongReference<String, Void>("noAttachmentReference");
        assertReference(reference, "noAttachmentReference", null);
    }

    @Test
    public void nullReference() {
        final Reference<Object, String> reference = new StrongReference<Object, String>(null, "attachment of null reference");
        assertReference(reference, null, "attachment of null reference");
    }

    @Test
    public void nullReferenceWithoutAttachment() {
        final Reference<StringBuffer, Number> reference = new StrongReference<StringBuffer, Number>(null);
        assertReference(reference, null, null);
    }

    @Override
    <T, A> Reference<T, A> createReference(T value, A attachment) {
        return new StrongReference<T, A>(value, attachment);
    }

    @Override
    Type getTestedReferenceType() {
        return Type.STRONG;
    }
}