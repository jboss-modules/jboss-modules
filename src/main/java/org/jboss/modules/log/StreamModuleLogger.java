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

package org.jboss.modules.log;

import java.io.OutputStream;
import java.io.PrintStream;

import org.jboss.modules.Module;
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
    public void moduleDefined(final String name, final ModuleLoader moduleLoader) {
    }

    /** {@inheritDoc} */
    public void classDefineFailed(final Throwable throwable, final String className, final Module module) {
    }

    public void classDefined(final String name, final Module module) {
    }
}
