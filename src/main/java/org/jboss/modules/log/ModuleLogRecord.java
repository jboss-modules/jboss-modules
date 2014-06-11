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

import java.util.logging.LogRecord;
import java.util.logging.Level;

/**
 * A log record class which knows how to divine the proper class and method name.
 */
class ModuleLogRecord extends LogRecord {

    private static final long serialVersionUID = 542119905844866161L;
    private boolean resolved;
    private static final String LOGGER_CLASS_NAME = JDKModuleLogger.class.getName();

    ModuleLogRecord(final Level level, final String msg) {
        super(level, msg);
    }

    public String getSourceClassName() {
        if (! resolved) {
            resolve();
        }
        return super.getSourceClassName();
    }

    public void setSourceClassName(final String sourceClassName) {
        resolved = true;
        super.setSourceClassName(sourceClassName);
    }

    public String getSourceMethodName() {
        if (! resolved) {
            resolve();
        }
        return super.getSourceMethodName();
    }

    public void setSourceMethodName(final String sourceMethodName) {
        resolved = true;
        super.setSourceMethodName(sourceMethodName);
    }

    private void resolve() {
        resolved = true;
        final StackTraceElement[] stack = new Throwable().getStackTrace();
        boolean found = false;
        for (StackTraceElement element : stack) {
            final String className = element.getClassName();
            if (found) {
                if (! LOGGER_CLASS_NAME.equals(className)) {
                    setSourceClassName(className);
                    setSourceMethodName(element.getMethodName());
                    return;
                }
            } else {
                found = LOGGER_CLASS_NAME.equals(className);
            }
        }
        setSourceClassName("<unknown>");
        setSourceMethodName("<unknown>");
    }

    protected Object writeReplace() {
        final LogRecord replacement = new LogRecord(getLevel(), getMessage());
        replacement.setResourceBundle(getResourceBundle());
        replacement.setLoggerName(getLoggerName());
        replacement.setMillis(getMillis());
        replacement.setParameters(getParameters());
        replacement.setResourceBundleName(getResourceBundleName());
        replacement.setSequenceNumber(getSequenceNumber());
        replacement.setSourceClassName(getSourceClassName());
        replacement.setSourceMethodName(getSourceMethodName());
        replacement.setThreadID(getThreadID());
        replacement.setThrown(getThrown());
        return replacement;
    }
}
