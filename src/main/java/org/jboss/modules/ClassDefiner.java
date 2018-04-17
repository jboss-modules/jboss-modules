/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.nio.ByteBuffer;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

import static java.security.AccessController.doPrivileged;

/**
 * A hook for frameworks which need to define additional classes to a module's class loader.
 */
public final class ClassDefiner {
    private static final ClassDefiner instance = new ClassDefiner();
    private static final RuntimePermission createClassLoaderPermission = new RuntimePermission("createClassLoader");

    private ClassDefiner() {}

    /**
     * Define a class using the module and protection domain of an existing class.
     *
     * @param originalClass the existing class (must not be {@code null})
     * @param className the new class name (must not be {@code null})
     * @param classBytes the new class bytes (must not be {@code null})
     * @return the defined class (not {@code null})
     */
    public Class<?> defineClass(Class<?> originalClass, String className, ByteBuffer classBytes) {
        return defineClass(originalClass, className, getProtectionDomain(originalClass), classBytes);
    }

    /**
     * Define a class using the module and protection domain of an existing class.
     *
     * @param originalClass the existing class (must not be {@code null})
     * @param className the new class name (must not be {@code null})
     * @param classBytes the new class bytes (must not be {@code null})
     * @param off the offset into the {@code classBytes} array
     * @param len the number of bytes to use from the {@code classBytes} array
     * @return the defined class (not {@code null})
     */
    public Class<?> defineClass(Class<?> originalClass, String className, byte[] classBytes, int off, int len) {
        return defineClass(originalClass, className, getProtectionDomain(originalClass), classBytes, off, len);
    }

    /**
     * Define a class using the module and protection domain of an existing class.
     *
     * @param originalClass the existing class (must not be {@code null})
     * @param className the new class name (must not be {@code null})
     * @param classBytes the new class bytes (must not be {@code null})
     * @return the defined class (not {@code null})
     */
    public Class<?> defineClass(Class<?> originalClass, String className, byte[] classBytes) {
        return defineClass(originalClass, className, getProtectionDomain(originalClass), classBytes, 0, classBytes.length);
    }

    /**
     * Define a class using the module of an existing class.
     *
     * @param originalClass the existing class (must not be {@code null})
     * @param className the new class name (must not be {@code null})
     * @param protectionDomain the protection domain of the new class (must not be {@code null})
     * @param classBytes the new class bytes (must not be {@code null})
     * @return the defined class (not {@code null})
     */
    public Class<?> defineClass(Class<?> originalClass, String className, ProtectionDomain protectionDomain, ByteBuffer classBytes) {
        final Module module = Module.forClass(originalClass);
        if (module == null) throw new IllegalArgumentException("Original " + originalClass + " does not have a module");
        return module.getClassLoaderPrivate().defineClassInternal(className, classBytes, protectionDomain);
    }

    /**
     * Define a class using the module of an existing class.
     *
     * @param originalClass the existing class (must not be {@code null})
     * @param className the new class name (must not be {@code null})
     * @param protectionDomain the protection domain of the new class (must not be {@code null})
     * @param classBytes the new class bytes (must not be {@code null})
     * @param off the offset into the {@code classBytes} array
     * @param len the number of bytes to use from the {@code classBytes} array
     * @return the defined class (not {@code null})
     */
    public Class<?> defineClass(Class<?> originalClass, String className, ProtectionDomain protectionDomain, byte[] classBytes, int off, int len) {
        final Module module = Module.forClass(originalClass);
        if (module == null) throw new IllegalArgumentException("Original " + originalClass + " does not have a module");
        return module.getClassLoaderPrivate().defineClassInternal(className, classBytes, off, len, protectionDomain);
    }

    /**
     * Define a class using the module of an existing class.
     *
     * @param originalClass the existing class (must not be {@code null})
     * @param className the new class name (must not be {@code null})
     * @param protectionDomain the protection domain of the new class (must not be {@code null})
     * @param classBytes the new class bytes (must not be {@code null})
     * @return the defined class (not {@code null})
     */
    public Class<?> defineClass(Class<?> originalClass, String className, ProtectionDomain protectionDomain, byte[] classBytes) {
        return defineClass(originalClass, className, protectionDomain, classBytes, 0, classBytes.length);
    }

    /**
     * Define a class.
     *
     * @param module the module to define the class to (must not be {@code null})
     * @param className the new class name (must not be {@code null})
     * @param protectionDomain the protection domain of the new class (must not be {@code null})
     * @param classBytes the new class bytes (must not be {@code null})
     * @return the defined class (not {@code null})
     */
    public Class<?> defineClass(Module module, String className, ProtectionDomain protectionDomain, ByteBuffer classBytes) {
        return module.getClassLoaderPrivate().defineClassInternal(className, classBytes, protectionDomain);
    }

    /**
     * Define a class.
     *
     * @param module the module to define the class to (must not be {@code null})
     * @param className the new class name (must not be {@code null})
     * @param protectionDomain the protection domain of the new class (must not be {@code null})
     * @param classBytes the new class bytes (must not be {@code null})
     * @param off the offset into the {@code classBytes} array
     * @param len the number of bytes to use from the {@code classBytes} array
     * @return the defined class (not {@code null})
     */
    public Class<?> defineClass(Module module, String className, ProtectionDomain protectionDomain, byte[] classBytes, int off, int len) {
        return module.getClassLoaderPrivate().defineClassInternal(className, classBytes, off, len, protectionDomain);
    }

    /**
     * Define a class.
     *
     * @param module the module to define the class to (must not be {@code null})
     * @param className the new class name (must not be {@code null})
     * @param protectionDomain the protection domain of the new class (must not be {@code null})
     * @param classBytes the new class bytes (must not be {@code null})
     * @return the defined class (not {@code null})
     */
    public Class<?> defineClass(Module module, String className, ProtectionDomain protectionDomain, byte[] classBytes) {
        return defineClass(module, className, protectionDomain, classBytes, 0, classBytes.length);
    }

    /**
     * Get the class definer instance.  The caller must have the {@code createClassLoader} {@link RuntimePermission}.
     *
     * @return the singleton class definer instance (not {@code null})
     */
    public static ClassDefiner getInstance() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(createClassLoaderPermission);
        }
        return instance;
    }

    private ProtectionDomain getProtectionDomain(final Class<?> clazz) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return doPrivileged((PrivilegedAction<ProtectionDomain>) clazz::getProtectionDomain);
        } else {
            return clazz.getProtectionDomain();
        }
    }
}
