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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.modules.Main;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * A {@link ModuleLogger} which logs to a JDK logging category.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class JDKModuleLogger implements ModuleLogger {

    private static final Level TRACE;
    private static final Level DEBUG;
    private static final Level WARN;

    static {
        Level trace;
        Level debug;
        Level warn;
        try {
            trace = Level.parse("TRACE");
        } catch (IllegalArgumentException ignored) {
            trace = Level.FINEST;
        }
        try {
            debug = Level.parse("DEBUG");
        } catch (IllegalArgumentException ignored) {
            debug = Level.FINE;
        }
        try {
            warn = Level.parse("WARN");
        } catch (IllegalArgumentException ignored) {
            warn = Level.WARNING;
        }
        TRACE = trace;
        DEBUG = debug;
        WARN = warn;
    }

    @SuppressWarnings({ "NonConstantLogger" })
    private final Logger logger;
    @SuppressWarnings({ "NonConstantLogger" })
    private final Logger defineLogger;

    /**
     * Construct a new instance.
     *
     * @param logger the main logger to write to
     * @param defineLogger the main logger to write class-define-related trace messages to
     */
    public JDKModuleLogger(final Logger logger, final Logger defineLogger) {
        this.logger = logger;
        this.defineLogger = defineLogger;
    }

    /**
     * Construct a new instance.
     *
     * @param category the name of the logger category to write to
     */
    public JDKModuleLogger(final String category) {
        this(Logger.getLogger(category), Logger.getLogger(category + ".define"));
    }

    /**
     * Construct a new instance using the category {@code org.jboss.modules}.
     */
    public JDKModuleLogger() {
        this("org.jboss.modules");
    }

    private void doLog(final Level level, final String str) {
        doLog(level, str, null);
    }

    private void doLog(final Level level, final String str, final Throwable t) {
        try {
            final ModuleLogRecord rec = new ModuleLogRecord(level, str);
            rec.setLoggerName(logger.getName());
            if (t != null) rec.setThrown(t);
            logger.log(rec);
        } catch (Throwable ignored) {
        }
    }

    /** {@inheritDoc} */
    public void trace(final String message) {
        doLog(TRACE, message, null);
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object arg1) {
        if (logger.isLoggable(TRACE)) {
            doLog(TRACE, String.format(format, arg1), null);
        }
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object arg1, final Object arg2) {
        if (logger.isLoggable(TRACE)) {
            doLog(TRACE, String.format(format, arg1, arg2));
        }
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object arg1, final Object arg2, final Object arg3) {
        if (logger.isLoggable(TRACE)) {
            doLog(TRACE, String.format(format, arg1, arg2, arg3));
        }
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object... args) {
        if (logger.isLoggable(TRACE)) {
            doLog(TRACE, String.format(format, (Object[]) args));
        }
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String message) {
        doLog(TRACE, message, t);
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object arg1) {
        if (logger.isLoggable(TRACE)) {
            doLog(TRACE, String.format(format, arg1), t);
        }
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object arg1, final Object arg2) {
        if (logger.isLoggable(TRACE)) {
            doLog(TRACE, String.format(format, arg1, arg2), t);
        }
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object arg1, final Object arg2, final Object arg3) {
        if (logger.isLoggable(TRACE)) {
            doLog(TRACE, String.format(format, arg1, arg2, arg3), t);
        }
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object... args) {
        if (logger.isLoggable(TRACE)) {
            doLog(TRACE, String.format(format, (Object[]) args), t);
        }
    }

    /** {@inheritDoc} */
    public void greeting() {
        doLog(Level.INFO, String.format("JBoss Modules version %s", Main.getVersionString()));
    }

    /** {@inheritDoc} */
    public void moduleDefined(final ModuleIdentifier identifier, final ModuleLoader moduleLoader) {
        if (logger.isLoggable(DEBUG)) {
            doLog(DEBUG, String.format("Module %s defined by %s", identifier, moduleLoader));
        }
    }

    public void classDefineFailed(final Throwable throwable, final String className, final Module module) {
        if (defineLogger.isLoggable(WARN)) {
            doLog(WARN, String.format("Failed to define class %s in %s", className, module), throwable);
        }
    }

    public void classDefined(final String name, final Module module) {
        if (defineLogger.isLoggable(TRACE)) {
            doLog(TRACE, String.format("Defined class %s in %s", name, module));
        }
    }

    public void providerUnloadable(String name, ClassLoader loader) {
        if (defineLogger.isLoggable(TRACE)) {
            doLog(TRACE, String.format("Could not load provider %s in %s", name, loader));
        }
    }
}
