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

package org.jboss.modules;

import java.security.CodeSource;

/**
 * A class definition specification.
 *
 * @apiviz.exclude
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ClassSpec {
    private byte[] bytes;
    private CodeSource codeSource;
    private AssertionSetting assertionSetting = AssertionSetting.INHERIT;

    /**
     * Construct a new instance.
     */
    public ClassSpec() {
    }

    /**
     * Get the class file bytes.
     *
     * @return the class file bytes
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Set the class file bytes.
     *
     * @param bytes the class file bytes
     */
    public void setBytes(final byte[] bytes) {
        this.bytes = bytes;
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