/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

/**
 * A class loader that may be named.  On Java 9 and later, the name will be propagated up to the JVM.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class NamedClassLoader extends ClassLoader {
    static {
        if (! ClassLoader.registerAsParallelCapable()) {
            throw new Error("Failed to register " + NamedClassLoader.class.getName() + " as parallel-capable");
        }
    }

    private final String name;

    /**
     * Construct a new instance.
     *
     * @param parent the parent class loader (may be {@code null} to indicate that the platform class loader should be used)
     * @param name the name, or {@code null} if the class loader has no name
     */
    protected NamedClassLoader(final ClassLoader parent, final String name) {
        super(parent);
        this.name = name;
    }

    /**
     * Construct a new instance.
     *
     * @param name the name, or {@code null} if the class loader has no name
     */
    protected NamedClassLoader(final String name) {
        this.name = name;
    }

    /**
     * Get the name of this class loader.
     *
     * @return the name of this class loader, or {@code null} if it is unnamed
     */
    public String getName() {
        return name;
    }
}
