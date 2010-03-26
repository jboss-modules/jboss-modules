/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.module;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * Special class loader which adds module dependencies to the JDK class loader.  These dependencies are considered to
 * be imported by the JDK, not exported, thus they are not visible to other modules unless explicitly referenced.
 */
public final class SystemClassLoader extends ClassLoader {

    public SystemClassLoader() {
    }

    private static final Set<Class<?>> CLASS_LOADER_CLASSES;

    private static final class StackChecker extends SecurityManager {
        protected Class[] getClassContext() {
            return super.getClassContext();
        }
    }

    private static final StackChecker stackChecker = new StackChecker();

    static {
        Set<Class<?>> set = new HashSet<Class<?>>();
        set.add(Class.class);
        set.add(ClassLoader.class);
        set.add(SystemClassLoader.class);
        set.add(StackChecker.class);
        set.add(ModuleClassLoader.class);
        set.add(ImportingClassLoader.class);
        CLASS_LOADER_CLASSES = set;
    }

    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        final int idx = name.lastIndexOf('/');
        if (idx != -1) {
            final String pkgName = name.substring(0, idx);
            final URI uri = BootConfig.packageAutoLoads.get(pkgName);
            if (uri != null) {
                // check to see if the caller is the JDK
                for (Class clazz : stackChecker.getClassContext()) {
                    if (CLASS_LOADER_CLASSES.contains(clazz)) {
                        continue;
                    }
                    if (clazz.getClassLoader() == null) {
                        try {
                            return SystemModuleLoader.getInstance().loadModule(uri).getExportedClass(name);
                        } catch (ModuleNotFoundException e) {
                            throw new ClassNotFoundException(name, e);
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return super.findClass(name);
    }
}
