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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * A logger finder which attempts to locate a {@link LoggerFinder} on the
 * {@linkplain java.util.logging.LogManager log managers} class path. If not found a default finder will be used.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class ModuleLoggerFinder extends LoggerFinder {
    private static final LoggerFinder queueingFinder = new LoggerFinder() {
        @Override
        public System.Logger getLogger(final String name, final java.lang.Module module) {
            return new DelegatingSystemLogger(new QueueingSystemLogger(name, module));
        }
    };

    private static final Supplier<LoggerFinder> defaultFinder = () -> new LoggerFinder() {
        @Override
        public System.Logger getLogger(final String name, final java.lang.Module module) {
            return new JulSystemLogger(Logger.getLogger(name));
        }
    };

    private static final Map<String, System.Logger> loggers = new ConcurrentHashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Deque<SystemLogRecord> messages = new LinkedBlockingDeque<>();
    private static volatile boolean activated = false;
    // Guarded by lock
    private static volatile LoggerFinder finder;

    @Override
    public System.Logger getLogger(final String name, final java.lang.Module module) {
        if (activated) {
            return finder.getLogger(name, module);
        }
        lock.lock();
        try {
            // Check if we're activated at this point, if so we need to return a logger from the resolved finder
            if (activated) {
                // This should be set at this point
                return finder.getLogger(name, module);
            }
            return loggers.computeIfAbsent(name, s -> queueingFinder.getLogger(name, module));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Activates the module logger and replaces any old delegating {@link System.Logger system loggers} with a logger
     * from the new finder.
     *
     * @param cl the class loader to use
     */
    static void activate(final ClassLoader cl) {
        // To keep order, we must observe the lock
        lock.lock();
        try {
            if (!activated) {
                try {
                    final ServiceLoader<LoggerFinder> loader = ServiceLoader.load(LoggerFinder.class, cl == null ? ClassLoader.getSystemClassLoader() : cl);
                    for (LoggerFinder lf : loader) {
                        if (!ModuleLoggerFinder.class.equals(lf.getClass())) {
                            finder = lf;
                            break;
                        }
                    }
                    if (finder == null) {
                        finder = defaultFinder.get();
                    }
                } catch (Throwable ignore) {
                    finder = defaultFinder.get();
                }
                // Process the queued loggers and drain the contents to new loggers from the finder found
                final Iterator<Map.Entry<String, System.Logger>> iter = loggers.entrySet().iterator();
                while (iter.hasNext()) {
                    final Map.Entry<String, System.Logger> entry = iter.next();
                    final System.Logger currentLogger = entry.getValue();
                    // The current should be a delegating logger
                    final DelegatingSystemLogger delegatingSystemLogger = (DelegatingSystemLogger) currentLogger;
                    final System.Logger currentDelegate = delegatingSystemLogger.getDelegate();
                    final QueueingSystemLogger queueingSystemLogger = (QueueingSystemLogger) currentDelegate;
                    // Replace the queueing logger with a logger from the finder
                    delegatingSystemLogger.delegate.compareAndSet(currentDelegate,
                            finder.getLogger(queueingSystemLogger.name, queueingSystemLogger.module));
                    iter.remove();
                }
            }
            // Drain the queue
            SystemLogRecord record;
            while ((record = messages.pollFirst()) != null) {
                final System.Logger logger = finder.getLogger(record.name, record.module);
                try {
                    if (record.cause == null) {
                        logger.log(record.level, record.bundle, record.msg, record.params);
                    } else {
                        logger.log(record.level, record.bundle, record.msg, record.cause);
                    }
                } catch (Exception e) {
                    System.err.printf("Failed to log message: %s%n", record);
                    e.printStackTrace(System.err);
                }
            }
        } finally {
            activated = true;
            lock.unlock();
        }
    }

    private static class DelegatingSystemLogger implements System.Logger {

        private final AtomicReference<System.Logger> delegate;

        private DelegatingSystemLogger(final System.Logger delegate) {
            this.delegate = new AtomicReference<>(delegate);
        }

        @Override
        public String getName() {
            return getDelegate().getName();
        }

        @Override
        public boolean isLoggable(final Level level) {
            return getDelegate().isLoggable(level);
        }

        @Override
        public void log(final Level level, final String msg) {
            getDelegate().log(level, msg);
        }

        @Override
        public void log(final Level level, final Supplier<String> msgSupplier) {
            getDelegate().log(level, msgSupplier);
        }

        @Override
        public void log(final Level level, final Object obj) {
            getDelegate().log(level, obj);
        }

        @Override
        public void log(final Level level, final String msg, final Throwable thrown) {
            getDelegate().log(level, msg, thrown);
        }

        @Override
        public void log(final Level level, final Supplier<String> msgSupplier, final Throwable thrown) {
            getDelegate().log(level, msgSupplier, thrown);
        }

        @Override
        public void log(final Level level, final String format, final Object... params) {
            getDelegate().log(level, format, params);
        }

        @Override
        public void log(final Level level, final ResourceBundle bundle, final String msg, final Throwable thrown) {
            getDelegate().log(level, bundle, msg, thrown);
        }

        @Override
        public void log(final Level level, final ResourceBundle bundle, final String format, final Object... params) {
            getDelegate().log(level, bundle, format, params);
        }

        private System.Logger getDelegate() {
            return delegate.get();
        }
    }

    private static class JulSystemLogger implements System.Logger {
        private static final Map<System.Logger.Level, java.util.logging.Level> LEVELS = new EnumMap<>(System.Logger.Level.class);

        static {
            LEVELS.put(System.Logger.Level.ALL, java.util.logging.Level.ALL);
            LEVELS.put(System.Logger.Level.TRACE, java.util.logging.Level.FINER);
            LEVELS.put(System.Logger.Level.DEBUG, java.util.logging.Level.FINE);
            LEVELS.put(System.Logger.Level.INFO, java.util.logging.Level.INFO);
            LEVELS.put(System.Logger.Level.WARNING, java.util.logging.Level.WARNING);
            LEVELS.put(System.Logger.Level.ERROR, java.util.logging.Level.SEVERE);
            LEVELS.put(System.Logger.Level.OFF, java.util.logging.Level.OFF);
        }

        private final Logger delegate;

        private JulSystemLogger(final Logger delegate) {
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

    private static class QueueingSystemLogger implements System.Logger {
        private static final System.Logger.Level DEFAULT_LEVEL;

        static {
            System.Logger.Level level;
            if (System.getSecurityManager() == null) {
                try {
                    level = System.Logger.Level.valueOf(System.getProperty("jdk.system.logger.level", "INFO"));
                } catch (IllegalArgumentException ignore) {
                    level = System.Logger.Level.INFO;
                }
            } else {
                level = AccessController.doPrivileged((PrivilegedAction<System.Logger.Level>) () -> {
                    try {
                        return System.Logger.Level.valueOf(System.getProperty("jdk.system.logger.level", "INFO"));
                    } catch (IllegalArgumentException ignore) {
                        return System.Logger.Level.INFO;
                    }
                });
            }
            DEFAULT_LEVEL = level;
        }

        private final String name;
        private final java.lang.Module module;

        private QueueingSystemLogger(final String name, final java.lang.Module module) {
            this.name = name;
            this.module = module;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isLoggable(final System.Logger.Level level) {
            return level != Level.OFF && level.ordinal() >= DEFAULT_LEVEL.ordinal();
        }

        @Override
        public void log(final System.Logger.Level level, final ResourceBundle bundle, final String msg, final Throwable thrown) {
            messages.addLast(new SystemLogRecord(name, module, level, bundle, msg, null, thrown));
        }

        @Override
        public void log(final System.Logger.Level level, final ResourceBundle bundle, final String format, final Object... params) {
            messages.addLast(new SystemLogRecord(name, module, level, bundle, format, params, null));
        }
    }

    private static class SystemLogRecord {
        private final String name;
        private final java.lang.Module module;
        private final System.Logger.Level level;
        private final ResourceBundle bundle;
        private final String msg;
        private final Object[] params;
        private final Throwable cause;

        private SystemLogRecord(final String name, final java.lang.Module module, final System.Logger.Level level, final ResourceBundle bundle, final String msg, final Object[] params, final Throwable cause) {
            this.name = name;
            this.module = module;
            this.level = level;
            this.bundle = bundle;
            this.msg = msg;
            this.params = params == null ? null : Arrays.copyOf(params, params.length);
            this.cause = cause;
        }

        @Override
        public String toString() {
            return "SystemLogRecord(level=" + level + ", bundle=" + bundle + ", msg=" + msg + ", params=" +
                    (params == null ? "" : Arrays.toString(params)) + ", cause=" + cause + ")";
        }
    }
}