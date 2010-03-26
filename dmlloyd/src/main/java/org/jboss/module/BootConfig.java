/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.module;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class BootConfig {
    static final String[] NO_STRINGS = new String[0];

    static final Map<String, URI> packageAutoLoads;
    static final String[] moduleBootPath;

    static final String MODULE_CONFIG_FILE_PROP = "jboss.module.configfile";
    static final String MODULE_BOOT_PATH_PROP = "jboss.module.path";
    static final String JDK_PACKAGE_BLACKLIST_PROP = "jboss.module.jdk.package.blacklist";

    static final String CURRENT_ARCH;
    static final Set<String> COMPATIBLE_ARCHS;

    static final String CURRENT_JRE;
    

    static {
        final String arch = System.getProperty("os.arch", "noarch");
        CURRENT_ARCH = arch;
        // todo figure out compatible archs
        COMPATIBLE_ARCHS = new HashSet<String>(Arrays.asList(arch, "noarch"));

        /*---------------------
        * Module path
        *---------------------*/
        final String modulePath = System.getProperty(MODULE_BOOT_PATH_PROP);
        moduleBootPath = modulePath == null ? NO_STRINGS : modulePath.split(Pattern.quote(File.pathSeparator));

        /*---------------------
         * Config file
         *---------------------*/
        final String pathName = System.getProperty(MODULE_CONFIG_FILE_PROP);
        if (pathName == null) {
            // defaults
            packageAutoLoads = Collections.emptyMap();
        } else {
            final Map<String, URI> autoloads = new HashMap<String, URI>();
            
            packageAutoLoads = autoloads;
        }
    }

    private BootConfig() {
    }
}
