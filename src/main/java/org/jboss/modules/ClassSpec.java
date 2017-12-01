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

package org.jboss.modules;

import java.nio.ByteBuffer;
import java.security.CodeSource;

/**
 * A class definition specification.
 *
 * @apiviz.exclude
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ClassSpec {
    private byte[] bytes;
    private ByteBuffer byteBuffer;
    private CodeSource codeSource;
    private AssertionSetting assertionSetting = AssertionSetting.INHERIT;

    /**
     * Construct a new instance.
     */
    public ClassSpec() {
    }

    /**
     * Get the class file bytes, if they are set.
     *
     * @return the class file bytes, if they are set; {@code null} otherwise
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Set the class file bytes.  Calling this method will clear any previously set {@code ByteBuffer}.
     *
     * @param bytes the class file bytes
     */
    public void setBytes(final byte[] bytes) {
        this.bytes = bytes;
        byteBuffer = null;
    }

    /**
     * Get the class byte buffer, if one is set.
     *
     * @return the class byte buffer, if one is set; {@code null} otherwise
     */
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    /**
     * Set the class byte buffer.  Calling this method will clear any previously set class {@code byte[]}.
     *
     * @param byteBuffer the class byte buffer
     * @return this class specification
     */
    public ClassSpec setByteBuffer(final ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        bytes = null;
        return this;
    }

    /**
     * Get the code source (should not be {@code null}).
     *
     * @return the code source
     */
    public CodeSource getCodeSource() {
        return codeSource;
    }

    /**
     * Set the code source (should not be {@code null}).
     *
     * @param codeSource the code source
     */
    public void setCodeSource(final CodeSource codeSource) {
        this.codeSource = codeSource;
    }

    /**
     * Get the class assertion setting.
     *
     * @return the assertion setting
     */
    public AssertionSetting getAssertionSetting() {
        return assertionSetting;
    }

    /**
     * Set the class assertion setting.
     *
     * @param assertionSetting the assertion setting
     */
    public void setAssertionSetting(final AssertionSetting assertionSetting) {
        if (assertionSetting == null) {
            throw new IllegalArgumentException("assertionSetting is null");
        }
        this.assertionSetting = assertionSetting;
    }
}