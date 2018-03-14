/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

package org.jboss.modules;

import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Collection;

/**
 * A class file transformer which operates on byte buffers.
 */
public interface ClassTransformer {
    /**
     * Transform the bytes of a class.  The position and limit of both the passed-in and returned buffers must mark
     * the start and end of the class bytes.
     *
     * @param loader the class loader of the class being transformed
     * @param className the <em>internal</em> name of the class being transformed (not {@code null})
     * @param protectionDomain the protection domain of the class, if any
     * @param classBytes the class bytes being transformed (not {@code null}; may be a direct or heap buffer)
     * @return the transformation result (may be a direct or heap buffer)
     * @throws IllegalArgumentException if the class could not be transformed for some reason
     */
    ByteBuffer transform(ClassLoader loader, String className, ProtectionDomain protectionDomain, ByteBuffer classBytes)
        throws IllegalArgumentException;

    /**
     * Get a new transformer which applies this transformation followed by another transformation.
     *
     * @param other the other transformation (must not be {@code null})
     * @return the new transformer (not {@code null})
     */
    default ClassTransformer andThen(ClassTransformer other) {
        if (other == null) throw new IllegalArgumentException("other is null");
        return new ClassTransformer() {
            public ByteBuffer transform(final ClassLoader loader, final String className, final ProtectionDomain protectionDomain, final ByteBuffer classBytes) throws IllegalArgumentException {
                ByteBuffer firstStage = ClassTransformer.this.transform(loader, className, protectionDomain, classBytes);
                if (firstStage == null) firstStage = classBytes;
                return other.transform(loader, className, protectionDomain, firstStage);
            }
        };
    }

    /**
     * Get a new transformer which applies all the transformations in the given collection.  The collection should either be
     * immutable or safe for concurrent iteration.  A synchronized collection is insufficiently thread-safe.
     *
     * @param transformers the transformer collection (must not be {@code null})
     * @return the new transformer (not {@code null})
     */
    static ClassTransformer allOf(Collection<? extends ClassTransformer> transformers) {
        if (transformers == null) throw new IllegalArgumentException("transformers is null");
        return new ClassTransformer() {
            public ByteBuffer transform(final ClassLoader loader, final String className, final ProtectionDomain protectionDomain, ByteBuffer classBytes) throws IllegalArgumentException {
                ByteBuffer transformed;
                for (final ClassTransformer transformer : transformers) {
                    if (transformer != null) {
                        transformed = transformer.transform(loader, className, protectionDomain, classBytes);
                        if (transformed != null) {
                            classBytes = transformed;
                        }
                    }
                }
                return classBytes;
            }
        };
    }

    /**
     * The identity transformation, which does not modify the class bytes at all.
     */
    ClassTransformer IDENTITY = new ClassTransformer() {
        public ByteBuffer transform(final ClassLoader loader, final String className, final ProtectionDomain protectionDomain, final ByteBuffer classBytes) throws IllegalArgumentException {
            return classBytes;
        }

        public ClassTransformer andThen(final ClassTransformer other) {
            if (other == null) throw new IllegalArgumentException("other is null");
            return other;
        }
    };
}
