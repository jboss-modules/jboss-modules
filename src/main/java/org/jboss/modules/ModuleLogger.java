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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ModuleLogger {

    void trace(String message);

    void trace(String format, Object arg1);

    void trace(String format, Object arg1, Object arg2);

    void trace(String format, Object arg1, Object arg2, Object arg3);

    void trace(String format, Object... args);

    void trace(Throwable t, String message);

    void trace(Throwable t, String format, Object arg1);

    void trace(Throwable t, String format, Object arg1, Object arg2);

    void trace(Throwable t, String format, Object arg1, Object arg2, Object arg3);

    void trace(Throwable t, String format, Object... args);

    void greeting();

    void moduleDefined(ModuleIdentifier identifier, final ModuleLoader moduleLoader);
}
