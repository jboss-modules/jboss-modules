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

import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 * A {@link ModuleLogger} implementation that does not log.
 *
 * @author thomas.diesler@jboss.com
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class NoopModuleLogger implements ModuleLogger {

    private static ModuleLogger instance = new NoopModuleLogger();

    public static ModuleLogger getInstance() {
        return instance;
    }

    @Override
    public void trace(String message) {
    }

    @Override
    public void trace(final String format, final Object arg1) {
    }

    @Override
    public void trace(final String format, final Object arg1, final Object arg2) {
    }

    @Override
    public void trace(final String format, final Object arg1, final Object arg2, final Object arg3) {
    }

    @Override
    public void trace(final String format, final Object... args) {
    }

    @Override
    public void trace(final Throwable t, final String message) {
    }

    @Override
    public void trace(final Throwable t, final String format, final Object arg1) {
    }

    @Override
    public void trace(final Throwable t, final String format, final Object arg1, final Object arg2) {
    }

    @Override
    public void trace(final Throwable t, final String format, final Object arg1, final Object arg2, final Object arg3) {
    }

    @Override
    public void trace(final Throwable t, final String format, final Object... args) {
    }

    @Override
    public void greeting() {
    }

    @Override
    public void moduleDefined(final String name, final ModuleLoader moduleLoader) {
    }

    @Override
    public void classDefineFailed(final Throwable throwable, final String className, final Module module) {
    }

    @Override
    public void classDefined(final String name, final Module module) {
    }
}
