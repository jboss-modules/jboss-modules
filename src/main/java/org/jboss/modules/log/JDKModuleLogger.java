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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.modules.Main;
import org.jboss.modules.Module;
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
     * @deprecated Use {@link #JDKModuleLogger(String)} instead.
     */
    @Deprecated
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

    private void doLog(final Logger logger, final Level level, final String str, final Throwable t) {
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
        doLog(logger, TRACE, message, null);
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object arg1) {
        if (logger.isLoggable(TRACE)) {
            doLog(logger, TRACE, String.format(format, arg1), null);
        }
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object arg1, final Object arg2) {
        if (logger.isLoggable(TRACE)) {
            doLog(logger, TRACE, String.format(format, arg1, arg2), null);
        }
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object arg1, final Object arg2, final Object arg3) {
        if (logger.isLoggable(TRACE)) {
            doLog(logger, TRACE, String.format(format, arg1, arg2, arg3), null);
        }
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object... args) {
        if (logger.isLoggable(TRACE)) {
            doLog(logger, TRACE, String.format(format, (Object[]) args), null);
        }
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String message) {
        doLog(logger, TRACE, message, t);
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object arg1) {
        if (logger.isLoggable(TRACE)) {
            doLog(logger, TRACE, String.format(format, arg1), t);
        }
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object arg1, final Object arg2) {
        if (logger.isLoggable(TRACE)) {
            doLog(logger, TRACE, String.format(format, arg1, arg2), t);
        }
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object arg1, final Object arg2, final Object arg3) {
        if (logger.isLoggable(TRACE)) {
            doLog(logger, TRACE, String.format(format, arg1, arg2, arg3), t);
        }
    }

    /** {@inheritDoc} */
    public void trace(final Throwable t, final String format, final Object... args) {
        if (logger.isLoggable(TRACE)) {
            doLog(logger, TRACE, String.format(format, (Object[]) args), t);
        }
    }

    /** {@inheritDoc} */
    public void greeting() {
        doLog(logger, Level.INFO, String.format("JBoss Modules version %s", Main.getVersionString()), null);
    }

    /** {@inheritDoc} */
    public void moduleDefined(final String name, final ModuleLoader moduleLoader) {
        if (logger.isLoggable(DEBUG)) {
            doLog(logger, DEBUG, String.format("Module %s defined by %s", name, moduleLoader), null);
        }
    }

    public void classDefineFailed(final Throwable throwable, final String className, final Module module) {
        if (defineLogger.isLoggable(WARN)) {
            doLog(defineLogger, WARN, String.format("Failed to define class %s in %s", className, module), throwable);
        }
    }

    public void classDefined(final String name, final Module module) {
        if (defineLogger.isLoggable(TRACE)) {
            doLog(defineLogger, TRACE, String.format("Defined class %s in %s", name, module), null);
        }
    }
}
