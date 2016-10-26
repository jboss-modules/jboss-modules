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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ClassLoaderLocalLoader implements LocalLoader {

    static final ClassLoaderLocalLoader SYSTEM = new ClassLoaderLocalLoader(ClassLoaderLocalLoader.class.getClassLoader());

    private final ClassLoader classLoader;

    /**
     * Construct a new instance.
     *
     * @param classLoader the classloader to which we delegate
     */
    ClassLoaderLocalLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    // Public members

    public Class<?> loadClassLocal(final String name, final boolean resolve) {
        try {
            return Class.forName(name, resolve, classLoader);
        } catch (ClassNotFoundException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                //unlikely
                throw (RuntimeException) cause;
            }
            return null;
        }
    }

    public Package loadPackageLocal(final String name) {
        return JDKSpecific.getPackage(classLoader, name);
    }

    public List<Resource> loadResourceLocal(final String name) {
        final Enumeration<URL> urls;
        ClassLoader classLoader = this.classLoader;
        try {
            if (classLoader == null) {
                urls = JDKSpecific.getPlatformResources(name);
            } else {
                urls = classLoader.getResources(name);
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
        final List<Resource> list = new ArrayList<Resource>();
        while (urls.hasMoreElements()) {
            list.add(new URLResource(urls.nextElement()));
        }
        return list;
    }
}
