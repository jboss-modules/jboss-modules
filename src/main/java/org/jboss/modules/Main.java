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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.LogManager;

import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.modules.log.JDKModuleLogger;
import org.jboss.modules.log.StreamModuleLogger;

import static java.security.AccessController.doPrivileged;
import static org.jboss.modules.SecurityActions.setContextClassLoader;

/**
 * The main entry point of JBoss Modules when run as a JAR on the command line.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
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

    static volatile Instrumentation instrumentation;

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
        System.out.println("    -mp, -modulepath <search path of directories>");
        System.out.println("                  A list of directories, separated by '" + File.pathSeparator + "', where modules may be located");
        System.out.println("                  If not specified, the value of the \"module.path\" system property is used");
        System.out.println("    -class        Specify that the final argument is a");
        System.out.println("                  class to load from the class path; not compatible with -jar");
        System.out.println("    -cp,-classpath <search path of archives or directories>");
        System.out.println("                  A search path for class files; implies -class");
        System.out.println("    -dep,-dependencies <module-spec>[,<module-spec>,...]");
        System.out.println("                  A list of module dependencies to add to the class path;");
        System.out.println("                  requires -class or -cp");
        System.out.println("    -deptree      Print the dependency tree of the given module instead of running it");
        System.out.println("    -debuglog     Enable debug mode output to System.out during bootstrap before any logging manager is installed");
        System.out.println("    -jar          Specify that the final argument is the name of a");
        System.out.println("                  JAR file to run as a module; not compatible with -class");
        System.out.println("    -javaagent:agent.jar");
        System.out.println("                  Add a Java agent that can load modules");
        System.out.println("    -secmgr       Run with a security manager installed; not compatible with -secmgrmodule");
        System.out.println("    -secmgrmodule <module-spec>");
        System.out.println("                  Run with a security manager module; not compatible with -secmgr");
        System.out.println("    -add-provider <module-name>[/<class-name>]");
        System.out.println("                  Add a security provider of the given module and class (can be given more than once)");
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
        boolean depTree = false;
        String nameArgument = null;
        boolean defaultSecMgr = false;
        String secMgrModule = null;
        boolean debuglog = false;
        final List<String> agentJars = new ArrayList<>();
        final List<String> addedProviders = new ArrayList<>();
        for (int i = 0; i < argsLen; i++) {
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
                    } else if ("-deptree".equals(arg)) {
                        if (depTree) {
                            System.err.println("-deptree may only be specified once");
                            System.exit(1);
                        }
                        if (jar) {
                            System.err.println("-deptree may not be specified with -jar");
                            System.exit(1);
                        }
                        if (classDefined) {
                            System.err.println("-deptree may not be specified with -class");
                            System.exit(1);
                        }
                        if (classpathDefined) {
                            System.err.println("-deptree may not be specified with -classpath");
                            System.exit(1);
                        }
                        depTree = true;
                    } else if ("-debuglog".equals(arg)) {
                        debuglog = true;
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
                        if (depTree) {
                            System.err.println("-deptree may not be specified with -jar");
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
                        if (depTree) {
                            System.err.println("-deptree may not be specified with -classpath");
                            System.exit(1);
                        }
                        classpathDefined = true;
                        classpath = args[++i];
                        doPrivileged(new PropertyWriteAction("java.class.path", classpath));
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
                        if (depTree) {
                            System.err.println("-deptree may not be specified with -class");
                            System.exit(1);
                        }
                        classDefined = true;
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
                        secMgrModule = args[++ i];
                    } else if ("-add-provider".equals(arg)) {
                        addedProviders.add(args[++i]);
                    } else if (arg.startsWith("-javaagent:")) {
                        agentJars.add(arg.substring(11));
                    } else {
                        System.err.printf("Invalid option '%s'\n", arg);
                        usage();
                        System.exit(1);
                    }
                } else {
                    // it's the module specification
                    nameArgument = arg;
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
        if (nameArgument == null) {
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

        if (depTree) {
            DependencyTreeViewer.print(new PrintWriter(System.out), nameArgument, LocalModuleFinder.getRepoRoots(true));
            System.exit(0);
        }

        if(debuglog) {
            // Install the StreamModuleLogger on System.out to capture bootstrap messages
            Module.setModuleLogger(new StreamModuleLogger(System.out));
        }

        final ModuleLoader loader;
        final ModuleLoader environmentLoader;
        environmentLoader = DefaultBootModuleLoaderHolder.INSTANCE;
        final Path rootPath = Paths.get("").toAbsolutePath();
        final String moduleName;
        final String className;
        if (jar) {
            loader = new ModuleLoader(new FileSystemClassPathModuleFinder(environmentLoader));
            moduleName = rootPath.resolve(nameArgument).normalize().toString();
            className = null;
        } else if (classpathDefined || classDefined) {
            final String[] items;
            if (classpath != null) {
                AccessController.doPrivileged(new PropertyWriteAction("java.class.path", classpath));
                items = classpath.split(Pattern.quote(File.pathSeparator));
            } else {
                items = NO_STRINGS;
            }
            for (int i = 0; i < items.length; i++) {
                items[i] = rootPath.resolve(items[i]).normalize().toString();
            }
            loader = new DelegatingModuleLoader(environmentLoader, new ClassPathModuleFinder(environmentLoader, items, deps, nameArgument));
            moduleName = "<classpath>";
            className = null;
        } else {
            loader = environmentLoader;
            final int idx = nameArgument.lastIndexOf('/');
            if (idx != -1) {
                moduleName = nameArgument.substring(0, idx);
                className = nameArgument.substring(idx + 1);
            } else {
                moduleName = nameArgument;
                className = null;
            }
        }
        Module.initBootModuleLoader(environmentLoader);

        if (moduleName == null) {
            return;
        }

        final Module module;
        try {
            module = loader.loadModule(moduleName);
        } catch (ModuleNotFoundException e) {
            e.printStackTrace(System.err);
            System.exit(1);
            return;
        }
        final String ourJavaVersion = doPrivileged(new PropertyReadAction("java.specification.version", "1.6"));
        final String requireJavaVersion = module.getProperty("jboss.require-java-version", ourJavaVersion);
        final Pattern versionPattern = Pattern.compile("(?:1\\.)?(\\d+)");
        final Matcher requireMatcher = versionPattern.matcher(requireJavaVersion);
        final Matcher ourMatcher = versionPattern.matcher(ourJavaVersion);
        if (requireMatcher.matches() && ourMatcher.matches() && Integer.parseInt(requireMatcher.group(1)) > Integer.parseInt(ourMatcher.group(1))) {
            System.err.printf("This application requires Java specification version %s or later to run (this Java virtual machine implements specification version %s)%n", requireJavaVersion, ourJavaVersion);
            System.exit(1);
        }

        ModularURLStreamHandlerFactory.addHandlerModule(module);
        ModularContentHandlerFactory.addHandlerModule(module);

        // at this point, having a security manager already installed will prevent correct operation.

        final SecurityManager existingSecMgr = System.getSecurityManager();
        if (existingSecMgr != null) {
            System.err.println("An existing security manager was detected.  You must use the -secmgr switch to start with a security manager.");
            System.exit(1);
            return; // not reached
        }

        try {
            final Iterator<Policy> iterator = module.loadService(Policy.class).iterator();
            if (iterator.hasNext()) {
                Policy.setPolicy(iterator.next());
            }
        } catch (Exception ignored) {}

        // configure policy so that if SM is enabled, modules can still function
        final ModulesPolicy policy = new ModulesPolicy(Policy.getPolicy());
        Policy.setPolicy(policy);

        if (secMgrModule != null) {
            final Module loadedModule;
            try {
                loadedModule = environmentLoader.loadModule(secMgrModule);
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

        if (defaultSecMgr) {
            final Iterator<SecurityManager> iterator = module.loadService(SecurityManager.class).iterator();
            if (iterator.hasNext()) {
                System.setSecurityManager(iterator.next());
            } else {
                System.setSecurityManager(new SecurityManager());
            }
        }

        final ModuleClassLoader bootClassLoader = module.getClassLoaderPrivate();
        setContextClassLoader(bootClassLoader);

        final String serviceName = getServiceName(bootClassLoader, "java.util.prefs.PreferencesFactory");
        if (serviceName != null) {
            final String old = System.setProperty("java.util.prefs.PreferencesFactory", serviceName);
            try {
                Preferences.systemRoot();
            } finally {
                if (old == null) {
                    System.clearProperty("java.util.prefs.PreferencesFactory");
                } else {
                    System.setProperty("java.util.prefs.PreferencesFactory", old);
                }
            }
        }

        final String logManagerName = getServiceName(bootClassLoader, "java.util.logging.LogManager");
        if (logManagerName != null) {
            System.setProperty("java.util.logging.manager", logManagerName);
            if (LogManager.getLogManager().getClass() == LogManager.class) {
                System.err.println("WARNING: Failed to load the specified log manager class " + logManagerName);
            } else {
                Module.setModuleLogger(new JDKModuleLogger());
            }
        }

        if (! agentJars.isEmpty()) {
            final Instrumentation instrumentation = Main.instrumentation;
            if (instrumentation == null) {
                // we have to self-attach (todo later)
                System.err.println("Not started in agent mode (self-attach not supported yet)");
                usage();
                System.exit(1);
            }
            final ModuleLoader agentLoader = new ModuleLoader(new FileSystemClassPathModuleFinder(loader));
            for (String agentJarArg : agentJars) {
                final String agentJar;
                final String agentArgs;
                final int i = agentJarArg.indexOf('=');
                if (i > 0) {
                    agentJar = agentJarArg.substring(0, i);
                    if (agentJarArg.length() > (i + 1)) {
                        agentArgs = agentJarArg.substring(i + 1);
                    } else {
                        agentArgs = "";
                    }
                } else {
                    agentJar = agentJarArg;
                    agentArgs = "";
                }

                final Module agentModule;
                try {
                    agentModule = agentLoader.loadModule(new File(agentJar).getAbsolutePath());
                } catch (ModuleLoadException ex) {
                    System.err.printf("Cannot load agent JAR %s: %s", agentJar, ex);
                    System.exit(1);
                    throw new IllegalStateException();
                }
                final ModuleClassLoader classLoader = agentModule.getClassLoaderPrivate();
                final InputStream is = classLoader.getResourceAsStream("META-INF/MANIFEST.MF");
                final Manifest manifest;
                if (is == null) {
                    System.err.printf("Agent JAR %s has no manifest", agentJar);
                    System.exit(1);
                    throw new IllegalStateException();
                }
                try {
                    manifest = new Manifest();
                    manifest.read(is);
                    is.close();
                } catch (IOException e) {
                    try {
                        is.close();
                    } catch (IOException e2) {
                        e2.addSuppressed(e);
                        throw e2;
                    }
                    throw e;
                }
                // Note that this does not implement agent invocation as defined on
                // https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html. This is also not
                // done on the system class path which means some agents that rely on that may not work well here.
                final Attributes attributes = manifest.getMainAttributes();
                final String preMainClassName = attributes.getValue("Premain-Class");
                if (preMainClassName != null) {
                    final Class<?> preMainClass = Class.forName(preMainClassName, true, classLoader);
                    Object[] premainArgs;
                    Method premain;
                    try {
                        premain = preMainClass.getDeclaredMethod("premain", String.class, Instrumentation.class);
                        premainArgs = new Object[] {agentArgs, instrumentation};
                    } catch (NoSuchMethodException ignore) {
                        // If the method is not found we should check for the string only method
                        premain = preMainClass.getDeclaredMethod("premain", String.class);
                        premainArgs = new Object[] {agentArgs};
                    } catch (Exception e) {
                        System.out.printf("Failed to find premain method: %s", e);
                        System.exit(1);
                        throw new IllegalStateException();
                    }
                    try {
                        premain.invoke(null, premainArgs);
                    } catch (InvocationTargetException e) {
                        System.out.printf("Execution of premain method failed: %s", e.getCause());
                        System.exit(1);
                        throw new IllegalStateException();
                    }
                } else {
                    System.out.printf("Agent JAR %s has no premain method", agentJar);
                    System.exit(1);
                    throw new IllegalStateException();
                }
            }
        }

        final String mbeanServerBuilderName = getServiceName(bootClassLoader, "javax.management.MBeanServerBuilder");
        if (mbeanServerBuilderName != null) {
            System.setProperty("javax.management.builder.initial", mbeanServerBuilderName);
            // Initialize the platform mbean server
            ManagementFactory.getPlatformMBeanServer();
        }

        for (String addedProvider : addedProviders) {
            final int idx = addedProvider.indexOf('/');
            if (idx != -1) {
                final String provModule = addedProvider.substring(0, idx);
                final String provClazz = addedProvider.substring(idx + 1);
                final Class<? extends Provider> providerClass;
                try {
                    providerClass = Class.forName(provClazz, false, environmentLoader.loadModule(provModule).getClassLoaderPrivate()).asSubclass(Provider.class);
                    // each provider needs permission to install itself
                    doPrivileged(new AddProviderAction(providerClass.getConstructor().newInstance()), getProviderContext(providerClass));
                } catch (Exception e) {
                    Module.getModuleLogger().trace(e, "Failed to initialize a security provider");
                }
            } else {
                final ModuleClassLoader classLoader = environmentLoader.loadModule(addedProvider).getClassLoaderPrivate();
                final ServiceLoader<Provider> providerServiceLoader = ServiceLoader.load(Provider.class, classLoader);
                final Iterator<Provider> iterator = providerServiceLoader.iterator();
                for (;;) try {
                    if (! (iterator.hasNext())) {
                        Module.getModuleLogger().trace("Module \"%s\" did not contain a security provider service", addedProvider);
                        break;
                    }
                    final Provider provider = iterator.next();
                    final Class<? extends Provider> providerClass = provider.getClass();
                    // each provider needs permission to install itself
                    doPrivileged(new AddProviderAction(provider), getProviderContext(providerClass));
                } catch (ServiceConfigurationError | RuntimeException e) {
                    Module.getModuleLogger().trace(e, "Failed to initialize a security provider");
                }
            }
        }

        final ServiceLoader<Provider> providerServiceLoader = ServiceLoader.load(Provider.class, bootClassLoader);
        Iterator<Provider> iterator = providerServiceLoader.iterator();
        for (;;) try {
            if (! (iterator.hasNext())) break;
            final Provider provider = iterator.next();
            final Class<? extends Provider> providerClass = provider.getClass();
            // each provider needs permission to install itself
            doPrivileged(new AddProviderAction(provider), getProviderContext(providerClass));
        } catch (ServiceConfigurationError | RuntimeException e) {
            Module.getModuleLogger().trace(e, "Failed to initialize a security provider");
        }

        ModuleLoader.installMBeanServer();

        final ArrayList<String> argsList = new ArrayList<>(moduleArgs.length);
        Collections.addAll(argsList, moduleArgs);

        final ServiceLoader<PreMain> preMainServiceLoader = ServiceLoader.load(PreMain.class, bootClassLoader);
        for (PreMain preMain : preMainServiceLoader) {
            preMain.run(argsList);
        }

        try {
            final String[] argsArray = argsList.toArray(NO_STRINGS);
            if (className != null) module.run(className, argsArray); else module.run(argsArray);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
        return;
    }

    private static AccessControlContext getProviderContext(final Class<? extends Provider> providerClass) {
        return new AccessControlContext(new ProtectionDomain[] { providerClass.getProtectionDomain() });
    }

    private static String getServiceName(ClassLoader classLoader, String className) throws IOException {
        try (final InputStream stream = classLoader.getResourceAsStream("META-INF/services/" + className)) {
            if ( stream == null ) return null;
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
            return null;
        }
    }

    private static final String JAR_NAME;
    private static final String VERSION_STRING;

    static {
        Properties versionProps = new Properties();
        String jarName = "(unknown)";
        String versionString = "(unknown)";
        try (InputStream stream = Main.class.getResourceAsStream("version.properties")) {
            if (stream != null) try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                versionProps.load(reader);
                jarName = versionProps.getProperty("jarName", jarName);
                versionString = versionProps.getProperty("version", versionString);
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

    static final class AddProviderAction implements PrivilegedAction<Void> {
        private final Provider provider;

        AddProviderAction(final Provider provider) {
            this.provider = provider;
        }

        public Void run() {
            Security.addProvider(provider);
            return null;
        }
    }
}
