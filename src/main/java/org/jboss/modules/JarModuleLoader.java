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

package org.jboss.modules;

import java.io.File;
import java.util.jar.JarFile;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class JarModuleLoader extends ModuleLoader {

    static final String[] NO_STRINGS = new String[0];
    private final ModuleLoader delegate;
    private final JarFile jarFile;
    private final String myName;

    JarModuleLoader(final ModuleLoader delegate, final JarFile jarFile) {
        super(new ModuleFinder[] { new JarModuleFinder(simpleNameOf(jarFile), jarFile) });
        this.delegate = delegate;
        this.jarFile = jarFile;
        myName = simpleNameOf(jarFile);
    }

    private static String simpleNameOf(JarFile jarFile) {
        String jarName = jarFile.getName();
        String simpleJarName = jarName.substring(jarName.lastIndexOf(File.separatorChar) + 1);
        return simpleJarName;
    }

    protected Module preloadModule(final String name) throws ModuleLoadException {
        if (name.equals(myName)) {
            return loadModuleLocal(name);
        } else {
            Module module = loadModuleLocal(name);
            if (module == null) {
                return preloadModule(name, delegate);
            } else {
                return module;
            }
        }
    }

    String getMyName() {
        return myName;
    }

    public String toString() {
        return "JAR module loader";
    }
}
