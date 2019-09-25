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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import sun.reflect.Reflection;

/**
 * JDK-specific classes which are replaced for different JDK major versions.  This one is for Java 8 only.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JDKSpecific {

    // === private fields and data ===

    private static Hack hack = AccessController.doPrivileged(new PrivilegedAction<Hack>() {
        @Override
        public Hack run() {
            return new Hack();
        }
    });

    private static final MethodHandle getPackageMH;
    private static final boolean hasGetCallerClass;
    private static final int callerOffset;

    static {
        try {
            getPackageMH = MethodHandles.lookup().unreflect(AccessController.doPrivileged(new PrivilegedAction<Method>() {
                public Method run() {
                    for (Method method : ClassLoader.class.getDeclaredMethods()) {
                        if (method.getName().equals("getPackage")) {
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            if (parameterTypes.length == 1 && parameterTypes[0] == String.class) {
                                method.setAccessible(true);
                                return method;
                            }
                        }
                    }
                    throw new IllegalStateException("No getPackage method found on ClassLoader");
                }
            }));
        } catch (IllegalAccessException e) {
            final IllegalAccessError error = new IllegalAccessError(e.getMessage());
            error.setStackTrace(e.getStackTrace());
            throw error;
        }
        boolean result = false;
        int offset = 0;
        try {
            //noinspection deprecation
            result = Reflection.getCallerClass(1) == JDKSpecific.class || Reflection.getCallerClass(2) == JDKSpecific.class;
            //noinspection deprecation
            offset = Reflection.getCallerClass(1) == Reflection.class ? 2 : 1;

        } catch (Throwable ignored) {}
        hasGetCallerClass = result;
        callerOffset = offset;
    }

    // === the actual JDK-specific API ===

    static JarFile getJarFile(final File name, final boolean verify) throws IOException {
        return new JarFile(name, verify);
    }

    static Class<?> getCallingUserClass() {
        // 0 == this class
        // 1 == immediate caller in jboss-modules
        // 2 == user caller
        Class<?>[] stack = hack.getClassContext();
        int i = 3;
        while (stack[i] == stack[2]) {
            // skip nested calls front the same class
            if (++i >= stack.length)
                return null;
        }

        return stack[i];
    }

    static Class<?> getCallingClass() {
        // 0 == this class
        // 1 == immediate caller in jboss-modules
        // 2 == user caller
        if (hasGetCallerClass) {
            return Reflection.getCallerClass(2 + callerOffset);
        } else {
            return hack.getClassContext()[2 + callerOffset];
        }
    }

    static boolean isParallelCapable(ConcurrentClassLoader cl) {
        return ConcurrentClassLoader.getLockForClass(cl, "$TEST$") != cl;
    }

    static Package getPackage(ClassLoader cl, String packageName) {
        try {
            return (Package) getPackageMH.invoke(cl, packageName);
        } catch (RuntimeException | Error e2) {
            throw e2;
        } catch (Throwable throwable) {
            throw new UndeclaredThrowableException(throwable);
        }
    }

    static Set<String> getJDKPaths() {
        final Set<String> pathSet = new FastCopyHashSet<>(1024);
        final Set<String> jarSet = new FastCopyHashSet<>(1024);
        final String sunBootClassPath = AccessController.doPrivileged(new PropertyReadAction("sun.boot.class.path"));
        final String javaClassPath = AccessController.doPrivileged(new PropertyReadAction("java.class.path"));
        JDKPaths.processClassPathItem(sunBootClassPath, jarSet, pathSet);
        JDKPaths.processClassPathItem(javaClassPath, jarSet, pathSet);
        pathSet.add("org/jboss/modules");
        pathSet.add("org/jboss/modules/filter");
        pathSet.add("org/jboss/modules/log");
        pathSet.add("org/jboss/modules/management");
        pathSet.add("org/jboss/modules/ref");
        return Collections.unmodifiableSet(pathSet);
    }

    static LocalLoader getSystemLocalLoader() {
        return new ClassLoaderLocalLoader(getPlatformClassLoader());
    }

    static ClassLoader getPlatformClassLoader() {
        return JDKSpecific.class.getClassLoader();
    }

    static URL getSystemResource(final String name) {
        final ClassLoader classLoader = getPlatformClassLoader();
        return classLoader != null ? classLoader.getResource(name) : ClassLoader.getSystemResource(name);
    }

    static Enumeration<URL> getSystemResources(final String name) throws IOException {
        final ClassLoader classLoader = getPlatformClassLoader();
        return classLoader != null ? classLoader.getResources(name) : ClassLoader.getSystemResources(name);
    }

    static InputStream getSystemResourceAsStream(final String name) {
        final ClassLoader classLoader = getPlatformClassLoader();
        return classLoader != null ? classLoader.getResourceAsStream(name) : ClassLoader.getSystemResourceAsStream(name);
    }

    static Class<?> getSystemClass(final ConcurrentClassLoader caller, final String className) throws ClassNotFoundException {
        final ClassLoader platformClassLoader = JDKSpecific.getPlatformClassLoader();
        return platformClassLoader != null ? platformClassLoader.loadClass(className) : caller.findSystemClassInternal(className);
    }

    static void addInternalPackages(final List<String> list) {
        list.add("sun.reflect.");
        list.add("jdk.internal.reflect.");
    }

    static String getModuleNameOf(final Class<?> clazz) {
        return null;
    }

    static String getModuleVersionOf(final Class<?> clazz) {
        return null;
    }

    // === nested util stuff, non-API ===

    static final class Hack extends SecurityManager {
        @Override
        protected Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }
}
