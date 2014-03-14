/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.modules.security;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;

/**
 * Protected privileged actions for this package.
 *
 * @author Josef Cacek
 */
class SecurityActions {

    /**
     * Returns result of the getClassLoader() call on the given module. If Java Security Manager is enabled, then the
     * getClassLoader() call is made in a doPrivileged block.
     *
     * @param module
     * @see AccessController#doPrivileged(PrivilegedAction)
     * @return module.getClassLoader() call result
     */
    static ModuleClassLoader getModuleClassLoader(final Module module) {
        if (System.getSecurityManager() == null) {
            return module.getClassLoader();
        }
        return AccessController.doPrivileged(new PrivilegedAction<ModuleClassLoader>() {
            public ModuleClassLoader run() {
                return module.getClassLoader();
            }
        });
    }
}
