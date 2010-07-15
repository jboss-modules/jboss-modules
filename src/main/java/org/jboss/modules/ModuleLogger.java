/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

/**
 * A simple Logger abstraction.
 *
 * @author thomas.diesler@jboss.com
 * @since 13-Jul-2010
 */
public interface ModuleLogger {

    enum Level {

        TRACE,
        DEBUG,
        WARN,
        ERROR
    }

    boolean isTraceEnabled();

    void trace(String message);

    void trace(String message, Throwable th);

    void debug(String message);

    void debug(String message, Throwable th);

    void warn(String message);

    void warn(String message, Throwable th);

    void error(String message);

    void error(String message, Throwable th);
}
