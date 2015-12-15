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

import org.jboss.modules.ref.util.Assert;

/**
 * Super class for all test cases that apply to reapable references.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @see Reapable
 */
public abstract class AbstractReapableReferenceTest extends AbstractReferenceTest {

    final <T, A> void assertReference(Reference<T, A> reference, T referenceValue, A attachment, Reaper<T, A> reaper) {
        Assert.assertReference(reference, referenceValue, attachment, getTestedReferenceType(), reaper);
    }
}
