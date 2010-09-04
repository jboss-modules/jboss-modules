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

import java.io.File;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public final class LocalModuleLoader extends ModuleLoader {

    private final File[] repoRoots;

    public LocalModuleLoader(final File[] repoRoots) {
        super(0);
        this.repoRoots = repoRoots;
    }

    @Override
    protected ModuleSpec findModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        final File moduleRoot = getModuleRoot(moduleIdentifier);
        if (moduleRoot == null)
            throw new ModuleNotFoundException("Module " + moduleIdentifier + " is not found");

        final File moduleXml = new File(moduleRoot, "module.xml");
        return parseModuleInfoFile(moduleIdentifier, moduleRoot, moduleXml);
    }

    private File getModuleRoot(final ModuleIdentifier moduleIdentifier) {
        final String child = toPathString(moduleIdentifier);
        for (File root : repoRoots) {
            final File file = new File(root, child);
            if (file.exists() && new File(file, "module.xml").exists()) return file;
        }
        return null;
    }

    private static String toPathString(ModuleIdentifier moduleIdentifier) {
        final StringBuilder builder = new StringBuilder();
        builder.append(moduleIdentifier.getName().replace('.', File.separatorChar));
        builder.append(File.separatorChar);
        return builder.toString();
    }

    private ModuleSpec parseModuleInfoFile(final ModuleIdentifier moduleIdentifier, final File moduleRoot, final File moduleInfoFile) throws ModuleLoadException {
        return ModuleXmlParser.parse(moduleIdentifier, moduleRoot, moduleInfoFile);
    }
}
