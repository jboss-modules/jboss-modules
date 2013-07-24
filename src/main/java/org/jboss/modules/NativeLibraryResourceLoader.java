/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import java.security.AccessController;
import java.util.Locale;

/**
 * A base class for resource loaders which can load native libraries.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class NativeLibraryResourceLoader extends AbstractResourceLoader {
    static {
        final PropertyReadAction osNameReadAction = new PropertyReadAction("os.name");
        final PropertyReadAction osArchReadAction = new PropertyReadAction("os.arch");
        final SecurityManager sm = System.getSecurityManager();
        final String sysName;
        final String sysArch;
        if (sm != null) {
            sysName = AccessController.doPrivileged(osNameReadAction).toUpperCase(Locale.US);
            sysArch = AccessController.doPrivileged(osArchReadAction).toUpperCase(Locale.US);
        } else {
            sysName = osNameReadAction.run().toUpperCase(Locale.US);
            sysArch = osArchReadAction.run().toUpperCase(Locale.US);
        }
        final String realName;
        final String realArch;
        if (sysName.startsWith("LINUX")) {
            realName = "linux";
        } else if (sysName.startsWith("MAC OS")) {
            realName = "macosx";
        } else if (sysName.startsWith("WINDOWS")) {
            realName = "win";
        } else if (sysName.startsWith("OS/2")) {
            realName = "os2";
        } else if (sysName.startsWith("SOLARIS") || sysName.startsWith("SUNOS")) {
            realName = "solaris";
        } else if (sysName.startsWith("MPE/IX")) {
            realName = "mpeix";
        } else if (sysName.startsWith("HP-UX")) {
            realName = "hpux";
        } else if (sysName.startsWith("AIX")) {
            realName = "aix";
        } else if (sysName.startsWith("OS/390")) {
            realName = "os390";
        } else if (sysName.startsWith("FREEBSD")) {
            realName = "freebsd";
        } else if (sysName.startsWith("OPENBSD")) {
            realName = "openbsd";
        } else if (sysName.startsWith("NETBSD")) {
            realName = "netbsd";
        } else if (sysName.startsWith("IRIX")) {
            realName = "irix";
        } else if (sysName.startsWith("DIGITAL UNIX")) {
            realName = "digitalunix";
        } else if (sysName.startsWith("OSF1")) {
            realName = "osf1";
        } else if (sysName.startsWith("OPENVMS")) {
            realName = "openvms";
        } else {
            realName = "unknown";
        }
        if (sysArch.startsWith("SPARCV9") || sysArch.startsWith("SPARC64")) {
            realArch = "sparcv9";
        } else if (sysArch.startsWith("SPARC")) {
            realArch = "sparc";
        } else if (sysArch.startsWith("X86_64") || sysArch.startsWith("AMD64")) {
            realArch = "x86_64";
        } else if (sysArch.startsWith("I386") || sysArch.startsWith("I586") || sysArch.startsWith("I686") || sysArch.startsWith("X86")) {
            realArch = "i686";
        } else if (sysArch.startsWith("PPC64")) {
            realArch = "ppc64";
        } else if (sysArch.startsWith("PPC") || sysArch.startsWith("POWER")) {
            realArch = "ppc";
        } else if (sysArch.startsWith("ARM")) {
            realArch = "arm";
        } else if (sysArch.startsWith("AARCH64") || sysArch.startsWith("ARM64")) {
            realArch = "aarch64";
        } else if (sysArch.startsWith("PA_RISC2.0W")) {
            realArch = "parisc64";
        } else if (sysArch.startsWith("PA_RISC") || sysArch.startsWith("PA-RISC")) {
            realArch = "parisc";
        } else if (sysArch.startsWith("IA64")) {
            // HP-UX reports IA64W for 64-bit Itanium and IA64N when running
            // in 32-bit mode.
            realArch = sysArch.toLowerCase(Locale.US);
        } else if (sysArch.startsWith("ALPHA")) {
            realArch = "alpha";
        } else if (sysArch.startsWith("MIPS")) {
            realArch = "mips";
        } else {
            realArch = "unknown";
        }
        ARCH_NAME = realName + "-" + realArch;
    }

    private static final String ARCH_NAME;

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
        final File file = new File(root, ARCH_NAME + File.separatorChar + System.mapLibraryName(name));
        return file.exists() ? file.getAbsolutePath() : null;
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
        return ARCH_NAME;
    }
}
