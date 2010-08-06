/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.logging.LogManager;

public final class Main {

    private Main() {
    }

    public static String getJarName() {
        return "UNSET";
    }

    public static String getVersionString() {
        return "TRUNK SNAPSHOT";
    }

    private static void usage() {
        System.out.println("Usage: java [-jvmoptions...] -jar " + getJarName() + ".jar [-options...] <module-spec> [args...]\n");
        System.out.println("where options include:");
        System.out.println("    -help         Display this message");
        System.out.println("    -modulepath <search path of directories>");
        System.out.println("    -mp <search path of directories>");
        System.out.println("                  A list of directories, separated by '" + File.pathSeparator + "', where modules may be located");
        System.out.println("                  If not specified, the value of the \"module.path\" system property is used");
        System.out.println("    -logmodule <module-name>");
        System.out.println("                  The module to use to load the system logmanager");
        System.out.println("    -version      Print version and exit\n");
        System.out.println("and module-spec is a valid module specification string");
    }

    public static void main(String[] args) throws Throwable {
        final int argsLen = args.length;
        String[] moduleArgs = null;
        String modulePath = null;
        ModuleIdentifier moduleIdentifier = null;
        ModuleIdentifier logManagerModuleIdentifier = null;
        for (int i = 0, argsLength = argsLen; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if (arg.charAt(0) == '-') {
                    // it's an option
                    if ("-version".equals(arg)) {
                        System.out.println("Module loader " + getVersionString());
                        return;
                    } else if ("-help".equals(arg)) {
                        usage();
                        return;
                    } else if ("-modulepath".equals(arg) || "-mp".equals(arg)) {
                        if (modulePath != null) {
                            System.err.println("Module path may only be specified once");
                            System.exit(1);
                        }
                        modulePath = args[++i];
                        System.setProperty("module.path", modulePath);
                    } else if ("-logmodule".equals(arg)) {
                        logManagerModuleIdentifier = ModuleIdentifier.fromString(args[++i]);
                    } else {
                        System.err.printf("Invalid option '%s'\n", arg);
                        usage();
                        System.exit(1);
                    }
                } else {
                    // it's the module specification
                    moduleIdentifier = ModuleIdentifier.fromString(arg);
                    int cnt = argsLen - i - 1;
                    moduleArgs = new String[cnt];
                    System.arraycopy(args, i + 1, moduleArgs, 0, cnt);
                    break;
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.printf("Argument expected for option %s\n", arg);
                usage();
                System.exit(1);
            }
        }
        // run the module
        if (moduleIdentifier == null) {
            System.err.println("No module specified");
            usage();
            System.exit(1);
        }
        final ModuleLoader loader = InitialModuleLoader.INSTANCE;
        if (logManagerModuleIdentifier != null) {
            final ModuleClassLoader classLoader = ModuleClassLoader.forModule(logManagerModuleIdentifier);
            final InputStream stream = classLoader.getResourceAsStream("META-INF/services/java.util.logging.LogManager");
            if (stream != null) {
                try {
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    String name = null;
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final int i = line.indexOf('#');
                        if (i != -1) {
                            line = line.substring(0, i);
                        }
                        line = line.trim();
                        if (line.length() == 0) continue;
                        name = line;
                        break;
                    }
                    if (name != null) {
                        System.setProperty("java.util.logging.manager", name);
                        final ClassLoader old = setContextClassLoader(classLoader);
                        try {
                            if (LogManager.getLogManager().getClass() == LogManager.class) {
                                System.err.println("WARNING: Failed to load the specified logmodule " + logManagerModuleIdentifier);
                            } else {
                                Module.setModuleLogger(new JDKModuleLogger());
                            }
                        } finally {
                            setContextClassLoader(old);
                        }
                    } else {
                        System.err.println("WARNING: No log manager services defined in specified logmodule " + logManagerModuleIdentifier);
                    }
                } finally {
                    try {
                        stream.close();
                    } catch (IOException ignored) {
                        // ignore
                    }
                }
            } else {
                System.err.println("WARNING: No log manager service descriptor found in specified logmodule " + logManagerModuleIdentifier);
            }
        }
        final Module module;
        try {
            module = loader.loadModule(moduleIdentifier);
        } catch (ModuleNotFoundException e) {
            e.printStackTrace(System.err);
            System.exit(1);
            return;
        }
        try {
            module.run(moduleArgs);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
        return;
    }

    private static ClassLoader setContextClassLoader(final ClassLoader classLoader) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return doSetContextClassLoader(classLoader);
                }
            });
        }
        return doSetContextClassLoader(classLoader);
    }

    private static ClassLoader doSetContextClassLoader(final ClassLoader classLoader) {
        try {
            return Thread.currentThread().getContextClassLoader();
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }
}