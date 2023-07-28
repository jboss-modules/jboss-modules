/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
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

import java.lang.System.LoggerFinder;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * A logger finder which attempts to locate a {@link LoggerFinder} on the
 * {@linkplain java.util.logging.LogManager log managers} class path. If not found a default finder will be used.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class ModuleLoggerFinder extends LoggerFinder {

    private final Supplier<LoggerFinder> defaultFinder = () -> new LoggerFinder() {
        @Override
        public System.Logger getLogger(final String name, final java.lang.Module module) {
            return new JulSystemLogger(Logger.getLogger(name));
        }
    };
    private static volatile ClassLoader classLoader;
    private volatile LoggerFinder finder;

    public ModuleLoggerFinder() {
    }


    @Override
    public System.Logger getLogger(final String name, final java.lang.Module module) {
        return resolveFinder().getLogger(name, module);
    }

    /**
     * Sets the class loader to use when attempting to find the {@link LoggerFinder}.
     *
     * @param cl the class loader to use
     */
    static void setClassLoader(final ClassLoader cl) {
        classLoader = cl;
    }

    private LoggerFinder resolveFinder() {
        LoggerFinder result = finder;
        if (result == null) {
            synchronized (this) {
                result = finder;
                if (result == null) {
                    try {
                        final ClassLoader cl = classLoader;
                        final ServiceLoader<LoggerFinder> loader = ServiceLoader.load(LoggerFinder.class, cl == null ? ClassLoader.getSystemClassLoader() : cl);
                        for (LoggerFinder lf : loader) {
                            if (!getClass().equals(lf.getClass())) {
                                finder = result = lf;
                                break;
                            }
                        }
                        if (result == null) {
                            finder = result = defaultFinder.get();
                        }
                    } catch (Throwable ignore) {
                        finder = result = defaultFinder.get();
                    }
                }
            }
        }
        return result;
    }

    private static class JulSystemLogger implements System.Logger {
        private static final Map<System.Logger.Level, java.util.logging.Level> LEVELS = new EnumMap<>(System.Logger.Level.class);
        private final java.util.logging.Logger delegate;

        static {
            LEVELS.put(System.Logger.Level.ALL, java.util.logging.Level.ALL);
            LEVELS.put(System.Logger.Level.TRACE, java.util.logging.Level.FINER);
            LEVELS.put(System.Logger.Level.DEBUG, java.util.logging.Level.FINE);
            LEVELS.put(System.Logger.Level.INFO, java.util.logging.Level.INFO);
            LEVELS.put(System.Logger.Level.WARNING, java.util.logging.Level.WARNING);
            LEVELS.put(System.Logger.Level.ERROR, java.util.logging.Level.SEVERE);
            LEVELS.put(System.Logger.Level.OFF, java.util.logging.Level.OFF);
        }

        private JulSystemLogger(final java.util.logging.Logger delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public boolean isLoggable(final System.Logger.Level level) {
            return delegate.isLoggable(LEVELS.getOrDefault(level, java.util.logging.Level.INFO));
        }

        @Override
        public void log(final System.Logger.Level level, final ResourceBundle bundle, final String msg, final Throwable thrown) {
            delegate.logrb(LEVELS.getOrDefault(level, java.util.logging.Level.INFO), bundle, msg, thrown);
        }

        @Override
        public void log(final System.Logger.Level level, final ResourceBundle bundle, final String format, final Object... params) {
            delegate.logrb(LEVELS.getOrDefault(level, java.util.logging.Level.INFO), bundle, format, params);
        }
    }
}
