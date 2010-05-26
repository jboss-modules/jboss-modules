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

package org.jboss.modules;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Special class loader which knows how to load modules for certain packages.
 */
public final class SystemClassLoader extends ClassLoader {

    private static final Map<String, ModuleIdentifier> packageAutoLoads;

    static {
        packageAutoLoads = AccessController.doPrivileged(new PrivilegedAction<Map<String, ModuleIdentifier>>() {
            public Map<String, ModuleIdentifier> run() {
                final String automapFileName = System.getProperty("module.automap.filename");
                if (automapFileName == null) {
                    return Collections.emptyMap();
                }
                final File file = new File(automapFileName);
                if (file.exists()) {
                    final Properties props = new Properties();
                    try {
                        props.load(new InputStreamReader(new FileInputStream(file)));
                    } catch (IOException e) {
                        throw new IOError(e);
                    }
                    final Map<String, ModuleIdentifier> map = new HashMap<String, ModuleIdentifier>();
                    for (String name : props.stringPropertyNames()) {
                        map.put(name, ModuleIdentifier.fromString(props.getProperty(name)));
                    }
                    return map;
                } else {
                    return Collections.emptyMap();
                }
            }
        });
    }

    public SystemClassLoader(final ClassLoader parent) {
        super(parent);
    }

    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        final int idx = name.lastIndexOf('.');
        if (idx != -1) {
            final String pkgName = name.substring(0, idx);
            final ModuleIdentifier identifier = packageAutoLoads.get(pkgName);
            if (identifier != null) {
                // it's an auto-loaded package.  Load it via the current module loader
                ModuleLoader moduleLoader = InitialModuleLoader.INSTANCE;
                try {
                    return moduleLoader.loadModule(identifier).getExportedClass(name);
                } catch (ModuleLoadException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        }
        return super.findClass(name);
    }
}
