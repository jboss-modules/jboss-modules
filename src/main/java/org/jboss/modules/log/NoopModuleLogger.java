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

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
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
    public void moduleDefined(final ModuleIdentifier identifier, final ModuleLoader moduleLoader) {
    }

    @Override
    public void classDefineFailed(final Throwable throwable, final String className, final Module module) {
    }

    @Override
    public void classDefined(final String name, final Module module) {
    }
}
