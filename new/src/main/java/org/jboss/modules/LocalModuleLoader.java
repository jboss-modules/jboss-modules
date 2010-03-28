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

package org.jboss.modules;

import java.io.File;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class LocalModuleLoader extends ModuleLoader {

    private final File repoRoot;

    public LocalModuleLoader(final File repoRoot) {
        this.repoRoot = repoRoot;
    }

    @Override
    protected Module findModule(final ModuleIdentifier moduleIdentifier) throws ModuleNotFoundException {
        final File moduleRoot = getModuleRoot(moduleIdentifier);
        if (!moduleRoot.exists())
            throw new ModuleNotFoundException("Module " + moduleIdentifier + " does not exist in repository " + repoRoot);

        final File moduleXml = new File(moduleRoot, "module.xml");

        ModuleSpec moduleSpec;
        try {
            moduleSpec = parseModuleInfoFile(moduleIdentifier, moduleRoot, moduleXml);
        } catch (Exception e) {
            throw new ModuleNotFoundException(moduleIdentifier.toString(), e);
        }

        return defineModule(moduleSpec);
    }

    private File getModuleRoot(final ModuleIdentifier moduleIdentifier) {
        return new File(repoRoot, toPathString(moduleIdentifier));
    }

    private static String toPathString(ModuleIdentifier moduleIdentifier) {
        final StringBuilder builder = new StringBuilder();
        builder.append(moduleIdentifier.getGroup().replace('.', File.separatorChar));
        builder.append(File.separatorChar).append(moduleIdentifier.getArtifact());
        builder.append(File.separatorChar).append(moduleIdentifier.getVersion());
        builder.append(File.separatorChar);
        return builder.toString();
    }

    private ModuleSpec parseModuleInfoFile(final ModuleIdentifier moduleIdentifier, final File moduleRoot, final File moduleInfoFile) throws Exception {
        return ModuleXmlParser.parse(moduleIdentifier, moduleRoot, moduleInfoFile);
    }
}
