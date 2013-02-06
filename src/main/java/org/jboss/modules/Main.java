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

import __redirected.__JAXPRedirected;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.AccessController;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.LogManager;

import java.util.jar.Manifest;
import org.jboss.modules.log.JDKModuleLogger;

import static org.jboss.modules.SecurityActions.setContextClassLoader;

/**
 * The main entry point of JBoss Modules when run as a JAR on the command line.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jason T. Greene
 * @apiviz.exclude
 */
public final class Main {

    static {
        // Force initialization at the earliest possible point
        @SuppressWarnings("unused")
        long start = StartTimeHolder.START_TIME;
    }

    private static final String[] NO_STRINGS = new String[0];

    private Main() {
    }

    private static void usage() {
        System.out.println("Usage: java [-jvmoptions...] -jar " + getJarName() + ".jar [-options...] <module-spec> [args...]");
        System.out.println("       java [-jvmoptions...] -jar " + getJarName() + ".jar [-options...] -jar <jar-name> [args...]");
        System.out.println("       java [-jvmoptions...] -jar " + getJarName() + ".jar [-options...] -cp <class-path> <class-name> [args...]");
        System.out.println("       java [-jvmoptions...] -jar " + getJarName() + ".jar [-options...] -class <class-name> [args...]");
        System.out.println("where <module-spec> is a valid module specification string");
        System.out.println("and options include:");
        System.out.println("    -help         Display this message");
        System.out.println("    -modulepath <search path of directories>");
        System.out.println("    -mp <search path of directories>");
        System.out.println("                  A list of directories, separated by '" + File.pathSeparator + "', where modules may be located");
        System.out.println("                  If not specified, the value of the \"module.path\" system property is used");
        System.out.println("    -class        Specify that the final argument is a");
        System.out.println("                  class to load from the class path; not compatible with -jar");
        System.out.println("    -cp,-classpath <search path of archives or directories>");
        System.out.println("                  A search path for class files; implies -class");
        System.out.println("    -dep,-dependencies <module-spec>[,<module-spec>,...]");
        System.out.println("                  A list of module dependencies to add to the class path;");
        System.out.println("                  requires -class or -cp");
        System.out.println("    -jar          Specify that the final argument is the name of a");
        System.out.println("                  JAR file to run as a module; not compatible with -class");
        System.out.println("    -jaxpmodule <module-name>");
        System.out.println("                  The default JAXP implementation to use of the JDK");
        System.out.println("    -version      Print version and exit\n");
    }

    /**
     * Run JBoss Modules.
     *
     * @param args the command-line arguments
     *
     * @throws Throwable if an error occurs
     */
    public static void main(String[] args) throws Throwable {
        final int argsLen = args.length;
        String deps = null;
        String[] moduleArgs = NO_STRINGS;
        String modulePath = null;
        String classpath = null;
        boolean jar = false;
        boolean classpathDefined = false;
        boolean classDefined = false;
        String moduleIdentifierOrExeName = null;
        ModuleIdentifier jaxpModuleIdentifier = null;
        boolean defaultSecMgr = false;
        String secMgrModule = null;
        for (int i = 0, argsLength = argsLen; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if (arg.charAt(0) == '-') {
                    // it's an option
                    if ("-version".equals(arg)) {
                        System.out.println("JBoss Modules version " + getVersionString());
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
                    } else if ("-config".equals(arg)) {
                        System.err.println("Config files are no longer supported.  Use the -mp option instead");
                        System.exit(1);
                    } else if ("-jaxpmodule".equals(arg)) {
                        jaxpModuleIdentifier = ModuleIdentifier.fromString(args[++i]);
                    } else if ("-jar".equals(arg)) {
                        if (jar) {
                            System.err.println("-jar flag may only be specified once");
                            System.exit(1);
                        }
                        if (classpathDefined) {
                            System.err.println("-cp/-classpath may not be specified with -jar");
                            System.exit(1);
                        }
                        if (classDefined) {
                            System.err.println("-class may not be specified with -jar");
                            System.exit(1);
                        }
                        jar = true;
                    } else if ("-cp".equals(arg) || "-classpath".equals(arg)) {
                        if (classpathDefined) {
                            System.err.println("-cp or -classpath may only be specified once.");
                            System.exit(1);
                        }
                        if (classDefined) {
                            System.err.println("-class may not be specified with -cp/classpath");
                            System.exit(1);
                        }
                        if (jar) {
                            System.err.println("-cp/-classpath may not be specified with -jar");
                            System.exit(1);
                        }
                        classpathDefined = true;
                        classpath = args[++i];
                        AccessController.doPrivileged(new PropertyWriteAction("java.class.path", classpath));
                    } else if ("-dep".equals(arg) || "-dependencies".equals(arg)) {
                        if (deps != null) {
                            System.err.println("-dep or -dependencies may only be specified once.");
                            System.exit(1);
                        }
                        deps = args[++i];
                    } else if ("-class".equals(arg)) {
                        if (classDefined) {
                            System.err.println("-class flag may only be specified once");
                            System.exit(1);
                        }
                        if (classpathDefined) {
                            System.err.println("-class may not be specified with -cp/classpath");
                            System.exit(1);
                        }
                        if (jar) {
                            System.err.println("-class may not be specified with -jar");
                            System.exit(1);
                        }
                        classDefined = true;
                    } else if ("-logmodule".equals(arg)) {
                        System.err.println("WARNING: -logmodule is deprecated. Please use the system property 'java.util.logging.manager' or the 'java.util.logging.LogManager' service loader.");
                        i++;
                    } else if ("-secmgr".equals(arg)) {
                        if (defaultSecMgr) {
                            System.err.println("-secmgr may only be specified once");
                            System.exit(1);
                        }
                        if (secMgrModule != null) {
                            System.err.println("-secmgr may not be specified when -secmgrmodule is given");
                            System.exit(1);
                        }
                        defaultSecMgr = true;
                    } else if ("-secmgrmodule".equals(arg)) {
                        if (secMgrModule != null) {
                            System.err.println("-secmgrmodule may only be specified once");
                            System.exit(1);
                        }
                        if (defaultSecMgr) {
                            System.err.println("-secmgrmodule may not be specified when -secmgr is given");
                            System.exit(1);
                        }
                        secMgrModule = args[++i];
                    } else {
                        System.err.printf("Invalid option '%s'\n", arg);
                        usage();
                        System.exit(1);
                    }
                } else {
                    // it's the module specification
                    moduleIdentifierOrExeName = arg;
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

        if (deps != null && ! classDefined && ! classpathDefined) {
            System.err.println("-deps may only be specified when -cp/-classpath or -class is in use");
            System.exit(1);
        }

        // run the module
        if (moduleIdentifierOrExeName == null) {
            if (classDefined || classpathDefined) {
                System.err.println("No class name specified");
            } else if (jar) {
                System.err.println("No JAR specified");
            } else {
                System.err.println("No module specified");
            }
            usage();
            System.exit(1);
        }
        final ModuleLoader loader;
        final ModuleLoader environmentLoader;
        environmentLoader = DefaultBootModuleLoaderHolder.INSTANCE;
        final ModuleIdentifier moduleIdentifier;
        if (jar) {
            loader = new JarModuleLoader(environmentLoader, new JarFile(moduleIdentifierOrExeName));
            moduleIdentifier = ((JarModuleLoader) loader).getMyIdentifier();
        } else if (classpathDefined || classDefined) {
            loader = new ClassPathModuleLoader(environmentLoader, moduleIdentifierOrExeName, classpath, deps);
            moduleIdentifier = ModuleIdentifier.CLASSPATH;
        } else {
            loader = environmentLoader;
            moduleIdentifier = ModuleIdentifier.fromString(moduleIdentifierOrExeName);
        }
        Module.initBootModuleLoader(loader);
        if (jaxpModuleIdentifier != null) {
            __JAXPRedirected.changeAll(jaxpModuleIdentifier, Module.getBootModuleLoader());
        } else {
            __JAXPRedirected.changeAll(moduleIdentifier, Module.getBootModuleLoader());
        }

        if (secMgrModule != null) {
            final Module loadedModule;
            try {
                loadedModule = loader.loadModule(ModuleIdentifier.fromString(secMgrModule));
            } catch (ModuleNotFoundException e) {
                e.printStackTrace(System.err);
                System.exit(1);
                return;
            }
            final Iterator<SecurityManager> iterator = ServiceLoader.load(SecurityManager.class, loadedModule.getClassLoaderPrivate()).iterator();
            if (iterator.hasNext()) {
                System.setSecurityManager(iterator.next());
            } else {
                System.err.println("No security manager found in module " + secMgrModule);
                System.exit(1);
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

        ModularURLStreamHandlerFactory.addHandlerModule(module);
        ModularContentHandlerFactory.addHandlerModule(module);

        if (defaultSecMgr) {
            final Iterator<SecurityManager> iterator = ServiceLoader.load(SecurityManager.class, module.getClassLoaderPrivate()).iterator();
            if (iterator.hasNext()) {
                System.setSecurityManager(iterator.next());
            } else {
                System.setSecurityManager(new SecurityManager());
            }
        }

        final ModuleClassLoader bootClassLoader = module.getClassLoaderPrivate();
        setContextClassLoader(bootClassLoader);

        final String logManagerName = getServiceName(bootClassLoader, "java.util.logging.LogManager");
        if (logManagerName != null) {
            System.setProperty("java.util.logging.manager", logManagerName);
            if (LogManager.getLogManager().getClass() == LogManager.class) {
                System.err.println("WARNING: Failed to load the specified log manager class " + logManagerName);
            } else {
                Module.setModuleLogger(new JDKModuleLogger());
            }
        }

        final String mbeanServerBuilderName = getServiceName(bootClassLoader, "javax.management.MBeanServerBuilder");
        if (mbeanServerBuilderName != null) {
            System.setProperty("javax.management.builder.initial", mbeanServerBuilderName);
            // Initialize the platform mbean server
            ManagementFactory.getPlatformMBeanServer();
        }

        try {
            ModuleLoader.installMBeanServer();
            module.run(moduleArgs);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
        return;
    }

    private static String getServiceName(ClassLoader classLoader, String className) throws IOException {
        final InputStream stream = classLoader.getResourceAsStream("META-INF/services/" + className);
        if (stream != null) {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null) {
                    final int i = line.indexOf('#');
                    if (i != -1) {
                        line = line.substring(0, i);
                    }
                    line = line.trim();
                    if (line.length() == 0) continue;
                    return line;
                }

            } finally {
                try {
                    stream.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
        return null;
    }

    private static final String JAR_NAME;
    private static final String VERSION_STRING;

    static {
        final Enumeration<URL> resources;
        String jarName = "(unknown)";
        String versionString = "(unknown)";
        try {
            final ClassLoader classLoader = Main.class.getClassLoader();
            resources = classLoader == null ? ModuleClassLoader.getSystemResources("META-INF/MANIFEST.MF") : classLoader.getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                final InputStream stream = url.openStream();
                if (stream != null) try {
                    final Manifest manifest = new Manifest(stream);
                    final Attributes mainAttributes = manifest.getMainAttributes();
                    if (mainAttributes != null && "JBoss Modules".equals(mainAttributes.getValue("Specification-Title"))) {
                        jarName = mainAttributes.getValue("Jar-Name");
                        versionString = mainAttributes.getValue("Jar-Version");
                    }
                } finally {
                    try { stream.close(); } catch (Throwable ignored) {}
                }
            }
        } catch (IOException ignored) {
        }
        JAR_NAME = jarName;
        VERSION_STRING = versionString;
    }

    /**
     * Get the name of the JBoss Modules JAR.
     *
     * @return the name
     */
    public static String getJarName() {
        return JAR_NAME;
    }

    /**
     * Get the version string of JBoss Modules.
     *
     * @return the version string
     */
    public static String getVersionString() {
        return VERSION_STRING;
    }
}
