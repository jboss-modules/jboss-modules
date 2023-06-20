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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A base class for resource loaders which can load native libraries.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class NativeLibraryResourceLoader extends AbstractResourceLoader {

    /**
     * Separate class for native platform ID which is only loaded when native libs are loaded.
     */
    static class Identification {
        static final String OS_ID;
        static final String CPU_ID;
        static final String ARCH_NAME;
        static final String[] NATIVE_SEARCH_PATHS;
        static final Pattern OS_RELEASE_VERSION_ID_PATTERN = Pattern.compile("([0-9]+).*");
        static final Pattern DISTRIBUTION_RELEASE_VERSION_PATTERN = Pattern.compile(".*\\s([0-9]+).*");
        static final Pattern MAC_VERSION_PATTERN = Pattern.compile("([0-9]+\\.[0-9]+)\\.[0-9]+");
        static final String OS_RELEASE_FILE = "/etc/os-release";
        static final String FEDORA_RELEASE_FILE = "/etc/fedora-release";
        static final String REDHAT_RELEASE_FILE = "/etc/redhat-release";
        static final String ID = "ID=";
        static final String VERSION_ID = "VERSION_ID=";
        static final String RHEL = "rhel";
        static final String FEDORA = "fedora";

        static {
            final Object[] strings = AccessController.doPrivileged(new PrivilegedAction<>() {
                public Object[] run() {
                    // First, identify the operating system.
                    boolean knownOs = true;
                    String osName;
                    String osVersion = null;
                    // let the user override it.
                    osName = System.getProperty("jboss.modules.os-name");
                    if (osName == null) {
                        String sysOs = System.getProperty("os.name");
                        if (sysOs == null) {
                            osName = "unknown";
                            knownOs = false;
                        } else {
                            sysOs = sysOs.toUpperCase(Locale.US);
                            if (sysOs.startsWith("LINUX")) {
                                osName = "linux";
                                osVersion = getLinuxOSVersion();
                            } else if (sysOs.startsWith("MAC OS")) {
                                osName = "macosx";
                                String sysVersion = System.getProperty("os.version");
                                Matcher m = MAC_VERSION_PATTERN.matcher(sysVersion);
                                if (m.matches()) {
                                    osVersion = m.group(1);
                                }
                            } else if (sysOs.startsWith("WINDOWS")) {
                                osName = "win";
                            } else if (sysOs.startsWith("OS/2")) {
                                osName = "os2";
                            } else if (sysOs.startsWith("SOLARIS") || sysOs.startsWith("SUNOS")) {
                                osName = "solaris";
                            } else if (sysOs.startsWith("MPE/IX")) {
                                osName = "mpeix";
                            } else if (sysOs.startsWith("HP-UX")) {
                                osName = "hpux";
                            } else if (sysOs.startsWith("AIX")) {
                                osName = "aix";
                            } else if (sysOs.startsWith("OS/390")) {
                                osName = "os390";
                            } else if (sysOs.startsWith("OS/400")) {
                                osName = "os400";
                            } else if (sysOs.startsWith("FREEBSD")) {
                                osName = "freebsd";
                            } else if (sysOs.startsWith("OPENBSD")) {
                                osName = "openbsd";
                            } else if (sysOs.startsWith("NETBSD")) {
                                osName = "netbsd";
                            } else if (sysOs.startsWith("IRIX")) {
                                osName = "irix";
                            } else if (sysOs.startsWith("DIGITAL UNIX")) {
                                osName = "digitalunix";
                            } else if (sysOs.startsWith("OSF1")) {
                                osName = "osf1";
                            } else if (sysOs.startsWith("OPENVMS")) {
                                osName = "openvms";
                            } else if (sysOs.startsWith("IOS")) {
                                osName = "iOS";
                            } else {
                                osName = "unknown";
                                knownOs = false;
                            }
                        }
                    }
                    // Next, our CPU ID and its compatible variants.
                    boolean knownCpu = true;
                    ArrayList<String> cpuNames = new ArrayList<>();

                    String cpuName = System.getProperty("jboss.modules.cpu-name");
                    if (cpuName == null) {
                        String sysArch = System.getProperty("os.arch");
                        if (sysArch == null) {
                            cpuName = "unknown";
                            knownCpu = false;
                        } else {
                            boolean hasEndian = false;
                            boolean hasHardFloatABI = false;
                            sysArch = sysArch.toUpperCase(Locale.US);
                            if (sysArch.startsWith("SPARCV9") || sysArch.startsWith("SPARC64")) {
                                cpuName = "sparcv9";
                            } else if (sysArch.startsWith("SPARC")) {
                                cpuName = "sparc";
                            } else if (sysArch.startsWith("X86_64") || sysArch.startsWith("AMD64")) {
                                cpuName = "x86_64";
                            } else if (sysArch.startsWith("I386") || sysArch.startsWith("I486") || sysArch.startsWith("I586") || sysArch.startsWith("I686") || sysArch.startsWith("X86") || sysArch.contains("IA32")) {
                                cpuName = "i686";
                            } else if (sysArch.startsWith("X32")) {
                                cpuName = "x32";
                            } else if (sysArch.startsWith("PPC64")) {
                                cpuName = "ppc64";
                            } else if (sysArch.startsWith("PPC") || sysArch.startsWith("POWER")) {
                                cpuName = "ppc";
                            } else if (sysArch.startsWith("ARMV7A") || sysArch.contains("AARCH32")) {
                                hasEndian = true;
                                hasHardFloatABI = true;
                                cpuName = "armv7a";
                            } else if (sysArch.startsWith("AARCH64") || sysArch.startsWith("ARM64") || sysArch.startsWith("ARMV8") || sysArch.startsWith("PXA9") || sysArch.startsWith("PXA10")) {
                                hasEndian = true;
                                cpuName = "aarch64";
                            } else if (sysArch.startsWith("PXA27")) {
                                hasEndian = true;
                                cpuName = "armv5t-iwmmx";
                            } else if (sysArch.startsWith("PXA3")) {
                                hasEndian = true;
                                cpuName = "armv5t-iwmmx2";
                            } else if (sysArch.startsWith("ARMV4T") || sysArch.startsWith("EP93")) {
                                hasEndian = true;
                                cpuName = "armv4t";
                            } else if (sysArch.startsWith("ARMV4") || sysArch.startsWith("EP73")) {
                                hasEndian = true;
                                cpuName = "armv4";
                            } else if (sysArch.startsWith("ARMV5T") || sysArch.startsWith("PXA") || sysArch.startsWith("IXC") || sysArch.startsWith("IOP") || sysArch.startsWith("IXP") || sysArch.startsWith("CE")) {
                                hasEndian = true;
                                String isaList = System.getProperty("sun.arch.isalist");
                                if (isaList != null) {
                                    if (isaList.toUpperCase(Locale.US).contains("MMX2")) {
                                        cpuName = "armv5t-iwmmx2";
                                    } else if (isaList.toUpperCase(Locale.US).contains("MMX")) {
                                        cpuName = "armv5t-iwmmx";
                                    } else {
                                        cpuName = "armv5t";
                                    }
                                } else {
                                    cpuName = "armv5t";
                                }
                            } else if (sysArch.startsWith("ARMV5")) {
                                hasEndian = true;
                                cpuName = "armv5";
                            } else if (sysArch.startsWith("ARMV6")) {
                                hasEndian = true;
                                hasHardFloatABI = true;
                                cpuName = "armv6";
                            } else if (sysArch.startsWith("PA_RISC2.0W")) {
                                cpuName = "parisc64";
                            } else if (sysArch.startsWith("PA_RISC") || sysArch.startsWith("PA-RISC")) {
                                cpuName = "parisc";
                            } else if (sysArch.startsWith("IA64")) {
                                // HP-UX reports IA64W for 64-bit Itanium and IA64N when running
                                // in 32-bit mode.
                                cpuName = sysArch.toLowerCase(Locale.US);
                            } else if (sysArch.startsWith("ALPHA")) {
                                cpuName = "alpha";
                            } else if (sysArch.startsWith("MIPS")) {
                                cpuName = "mips";
                            } else {
                                knownCpu = false;
                                cpuName = "unknown";
                            }

                            boolean be = false;
                            boolean hf = false;

                            if (knownCpu && hasEndian && "big".equals(System.getProperty("sun.cpu.endian", "little"))) {
                                be = true;
                            }

                            if (knownCpu && hasHardFloatABI) {
                                String archAbi = System.getProperty("sun.arch.abi");
                                if (archAbi != null) {
                                    if (archAbi.toUpperCase(Locale.US).contains("HF")) {
                                        hf = true;
                                    }
                                } else {
                                    String libPath = System.getProperty("java.library.path");
                                    if (libPath != null && libPath.toUpperCase(Locale.US).contains("GNUEABIHF")) {
                                        hf = true;
                                    }
                                }
                                if (hf) cpuName += "-hf";
                            }

                            if (knownCpu) {
                                switch (cpuName) {
                                    case "i686": cpuNames.add("i686");
                                    case "i586": cpuNames.add("i586");
                                    case "i486": cpuNames.add("i486");
                                    case "i386": cpuNames.add("i386");
                                        break;
                                    case "armv7a": cpuNames.add("armv7a"); if (hf) break;
                                    case "armv6":  cpuNames.add("armv6"); if (hf) break;
                                    case "armv5t": cpuNames.add("armv5t");
                                    case "armv5":  cpuNames.add("armv5");
                                    case "armv4t": cpuNames.add("armv4t");
                                    case "armv4":  cpuNames.add("armv4");
                                        break;
                                    case "armv5t-iwmmx2": cpuNames.add("armv5t-iwmmx2");
                                    case "armv5t-iwmmx":  cpuNames.add("armv5t-iwmmx");
                                                          cpuNames.add("armv5t");
                                                          cpuNames.add("armv5");
                                                          cpuNames.add("armv4t");
                                                          cpuNames.add("armv4");
                                        break;
                                    default: cpuNames.add(cpuName);
                                        break;
                                }
                                if (hf || be) for (int i = 0; i < cpuNames.size(); i++) {
                                    String name = cpuNames.get(i);
                                    if (be) name += "-be";
                                    if (hf) name += "-hf";
                                    cpuNames.set(i, name);
                                }
                                cpuName = cpuNames.get(0);
                            }
                        }
                    } else {
                        cpuNames.add(cpuName);
                    }

                    // Finally, search paths.
                    final int cpuCount = cpuNames.size();
                    final int searchPathsSize = osVersion != null ? cpuCount * 2 : cpuCount;
                    String[] searchPaths = new String[searchPathsSize];
                    if (knownOs && knownCpu) {
                        // attempt OS-version specific category first
                        String osNameAndVersion = osVersion != null ? osName + "-" + osVersion : osName;
                        for (int i = 0; i < cpuCount; i++) {
                            final String name = cpuNames.get(i);
                            searchPaths[i] = osNameAndVersion + "-" + name;
                        }
                        // fallback to general category
                        if (osVersion != null) {
                            int j = cpuCount;
                            for (int i = 0; i < cpuCount; i++) {
                                final String name = cpuNames.get(i);
                                searchPaths[j++] = osName + "-" + name;
                            }
                        }
                    } else {
                        searchPaths = new String[0];
                    }

                    return new Object[] {
                        osName,
                        cpuName,
                        osName + "-" + cpuName,
                        searchPaths
                    };
                }
            });
            OS_ID = strings[0].toString();
            CPU_ID = strings[1].toString();
            ARCH_NAME = strings[2].toString();
            NATIVE_SEARCH_PATHS = (String[]) strings[3];
        }

        private static String getLinuxOSVersionFromOSReleaseFile() {
            try (InputStream releaseFile = new FileInputStream(OS_RELEASE_FILE)) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(releaseFile, StandardCharsets.UTF_8)) {
                    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                        String currentLine;
                        String id = null;
                        String versionId = null;
                        while ((id == null || versionId == null) && (currentLine = reader.readLine()) != null) {
                            final String trimmed = currentLine.trim();
                            if (trimmed.startsWith(ID)) {
                                int equalsIndex = trimmed.indexOf('=');
                                id = trimmed.substring(equalsIndex + 1).replaceAll("\"", "");
                            } else if (trimmed.startsWith(VERSION_ID)) {
                                int equalsIndex = trimmed.indexOf('=');
                                versionId = trimmed.substring(equalsIndex + 1).replaceAll("\"", "");
                            }
                        }
                        if (id != null && versionId != null) {
                            String abbreviatedVersionId = versionId;
                            Matcher m = OS_RELEASE_VERSION_ID_PATTERN.matcher(versionId);
                            if (m.matches()) {
                                abbreviatedVersionId = m.group(1);
                            }
                            return id + abbreviatedVersionId;
                        }
                        return null;
                    }
                }
            } catch (Exception e) {
               return null;
            }
        }

        private static String getLinuxOSVersionFromDistributionFile(String distributionFile) {
            try (InputStream releaseFile = new FileInputStream(distributionFile)) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(releaseFile, StandardCharsets.UTF_8)) {
                    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                        String currentLine;
                        String id = null;
                        String abbreviatedVersionId = null;
                        if ((currentLine = reader.readLine()) != null) {
                            final String trimmed = currentLine.trim();
                            if (trimmed.startsWith("Red Hat Enterprise Linux")) {
                                id = RHEL;
                            } else if (trimmed.startsWith("Fedora")) {
                                id = FEDORA;
                            }
                            Matcher m = DISTRIBUTION_RELEASE_VERSION_PATTERN.matcher(trimmed);
                            if (m.matches()) {
                                abbreviatedVersionId = m.group(1);
                            }
                        }
                        if (id != null && abbreviatedVersionId != null) {
                            return id + abbreviatedVersionId;
                        }
                        return null;
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }

        private static String getLinuxOSVersion() {
            String osVersion = getLinuxOSVersionFromOSReleaseFile();
            if (osVersion == null) {
                osVersion = getLinuxOSVersionFromDistributionFile(FEDORA_RELEASE_FILE);
                if (osVersion == null) {
                    osVersion = getLinuxOSVersionFromDistributionFile(REDHAT_RELEASE_FILE);
                }
            }
            return osVersion;
        }
    }

    /**
     * The filesystem root of the resource loader.
     */
    private final File root;

    /**
     * Construct a new instance.
     *
     * @param root the filesystem root of the resource loader
     */
    public NativeLibraryResourceLoader(final File root) {
        this.root = root;
    }

    /** {@inheritDoc} */
    public String getLibrary(final String name) {
        final String mappedName = System.mapLibraryName(name);
        final File root = this.root;
        File testFile;
        for (String path : Identification.NATIVE_SEARCH_PATHS) {
            testFile = new File(new File(root, path), mappedName);
            if (testFile.exists()) {
                return testFile.getAbsolutePath();
            }
        }
        return null;
    }

    public URI getLocation() {
        return root.toURI();
    }

    /**
     * Get the filesystem root of the resource loader.
     *
     * @return the filesystem root of the resource loader
     */
    public File getRoot() {
        return root;
    }

    /**
     * Get the detected architecture name for this platform.
     *
     * @return the architecture name
     */
    public static String getArchName() {
        return Identification.ARCH_NAME;
    }
}
