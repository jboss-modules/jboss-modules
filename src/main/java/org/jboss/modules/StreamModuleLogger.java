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

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A {@link ModuleLogger} implementation that logs to an output stream.
 *
 * @author thomas.diesler@jboss.com
 * @since 13-Jul-2010
 */
public class StreamModuleLogger implements ModuleLogger {

    private String name;
    private PrintStream print;

    private StreamModuleLogger(String name, OutputStream stream) {
        if (name == null)
            throw new IllegalArgumentException("Null logger name");
        if (stream == null)
            throw new IllegalArgumentException("Null logger stream");
        this.name = name;
        print = new PrintStream(stream);
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String message) {
        log(Level.TRACE, message, null);
    }

    @Override
    public void trace(String message, Throwable th) {
        log(Level.TRACE, message, th);
    }

    @Override
    public void debug(String message) {
        log(Level.DEBUG, message, null);
    }

    @Override
    public void debug(String message, Throwable th) {
        log(Level.DEBUG, message, th);
    }

    @Override
    public void warn(String message) {
        log(Level.WARN, message, null);
    }

    @Override
    public void warn(String message, Throwable th) {
        log(Level.WARN, message, th);
    }

    @Override
    public void error(String message) {
        log(Level.ERROR, message, null);
    }

    @Override
    public void error(String message, Throwable th) {
        log(Level.ERROR, message, th);
    }

    private void log(Level level, String message, Throwable th) {
        print.println(level + " [" + name + "] - " + message);
        if (th != null)
            th.printStackTrace(print);
    }
}
