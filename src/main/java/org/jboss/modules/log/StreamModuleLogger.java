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

package org.jboss.modules.log;

import java.io.OutputStream;
import java.io.PrintStream;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * A {@link ModuleLogger} implementation that logs all output (including trace) to an output or print stream.
 *
 * @author thomas.diesler@jboss.com
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class StreamModuleLogger implements ModuleLogger {

    private PrintStream print;

    /**
     * Construct a new instance.
     *
     * @param stream the print stream to write to
     */
    public StreamModuleLogger(PrintStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("stream is null");
        }
        print = new PrintStream(stream);
    }

    /**
     * Construct a new instance.
     *
     * @param stream the output stream to write to
     */
    public StreamModuleLogger(OutputStream stream) {
        this(new PrintStream(stream));
    }

    /** {@inheritDoc} */
    public void trace(final String message) {
        print.print("modules TRACE: ");
        print.println(message);
        print.flush();
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object arg1) {
        print.print("modules TRACE: ");
        print.printf(format, arg1);
        print.println();
        print.flush();
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object arg1, final Object arg2) {
        print.print("modules TRACE: ");
        print.printf(format, arg1, arg2);
        print.println();
        print.flush();
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object arg1, final Object arg2, final Object arg3) {
        print.print("modules TRACE: ");
        print.printf(format, arg1, arg2, arg3);
        print.println();
        print.flush();
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object... args) {
        print.print("modules TRACE: ");
        print.printf(format, (Object[]) args);
        print.println();
        print.flush();
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String message) {
        print.print("modules TRACE: ");
        print.print(message);
        print.print(": ");
        t.printStackTrace(print);
        print.flush();
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object arg1) {
        print.print("modules TRACE: ");
        print.printf(format, arg1);
        print.print(": ");
        t.printStackTrace(print);
        print.flush();
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object arg1, final Object arg2) {
        print.print("modules TRACE: ");
        print.printf(format, arg1, arg2);
        print.print(": ");
        t.printStackTrace(print);
        print.flush();
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object arg1, final Object arg2, final Object arg3) {
        print.print("modules TRACE: ");
        print.printf(format, arg1, arg2, arg3);
        print.print(": ");
        t.printStackTrace(print);
        print.flush();
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object... args) {
        print.print("modules TRACE: ");
        print.printf(format, args);
        print.print(": ");
        t.printStackTrace(print);
        print.flush();
    }

    /** {@inheritDoc} */
    public void greeting() {
    }

    /** {@inheritDoc} */
    public void moduleDefined(final ModuleIdentifier identifier, final ModuleLoader moduleLoader) {
    }

    /** {@inheritDoc} */
    public void classDefineFailed(final Throwable throwable, final String className, final Module module) {
    }

    public void classDefined(final String name, final Module module) {
    }

    public void providerUnloadable(String name, ClassLoader loader) {
    }
}
