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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;

/**
 * A wrapper around a {@link ClassFileTransformer}.
 */
public final class JLIClassTransformer implements ClassTransformer {
    private final ClassFileTransformer transformer;

    /**
     * Construct a new instance.
     *
     * @param transformer the delegate transformer (must not be {@code null})
     */
    public JLIClassTransformer(final ClassFileTransformer transformer) {
        if (transformer == null) throw new IllegalArgumentException("transformer is null");
        this.transformer = transformer;
    }

    public ByteBuffer transform(final ClassLoader loader, final String className, final ProtectionDomain protectionDomain, final ByteBuffer classBytes) throws IllegalArgumentException {
        final int position = classBytes.position();
        final int limit = classBytes.limit();
        final byte[] bytes;
        final byte[] result;
        if (classBytes.hasArray() && classBytes.arrayOffset() == 0 && position == 0 && limit == classBytes.capacity()) {
            bytes = classBytes.array();
        } else {
            bytes = new byte[limit - position];
            classBytes.get(bytes);
            classBytes.position(position);
        }
        try {
            result = transformer.transform(loader, className, null, protectionDomain, bytes);
        } catch (IllegalClassFormatException e) {
            throw new IllegalArgumentException(e);
        }
        return result == null ? classBytes : ByteBuffer.wrap(result);
    }
}
