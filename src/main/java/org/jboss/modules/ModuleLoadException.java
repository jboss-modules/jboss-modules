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

/**
 * Module load exception, thrown when there is some problem loading a module.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ModuleLoadException extends Exception {

    private static final long serialVersionUID = 3286005346300416890L;

    /**
     * Constructs a {@code ModuleLoadException} with no detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause(Throwable) initCause}.
     */
    public ModuleLoadException() {
    }

    /**
     * Constructs a {@code ModuleLoadException} with the specified detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     */
    public ModuleLoadException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a {@code ModuleLoadException} with the specified cause. The detail message is set to:
     * <pre>(cause == null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public ModuleLoadException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a {@code ModuleLoadException} with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public ModuleLoadException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    /**
     * Convert to an unchecked error type.
     *
     * @return the unchecked error
     */
    public ModuleLoadError toError() {
        final ModuleLoadError error = new ModuleLoadError(getMessage(), getCause());
        error.setStackTrace(getStackTrace());
        return error;
    }
}
