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