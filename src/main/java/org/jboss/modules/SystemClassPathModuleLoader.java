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

/**
 * The module loader for the system/application class path module.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SystemClassPathModuleLoader extends ModuleLoader {

    static final SystemClassPathModuleLoader INSTANCE = new SystemClassPathModuleLoader();

    /**
     * Construct a new instance.
     */
    public SystemClassPathModuleLoader() {
        super(false, false);
    }

    /**
     * Get the system module loader.
     *
     * @return the system module loader
     */
    public static SystemClassPathModuleLoader getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    protected ModuleSpec findModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        if (! moduleIdentifier.equals(ModuleIdentifier.SYSTEM)) {
            return null;
        }

        final SystemLocalLoader systemLocalLoader = SystemLocalLoader.getInstance();
        final ModuleSpec.Builder builder = ModuleSpec.build(ModuleIdentifier.SYSTEM);
        builder.addDependency(DependencySpec.createLocalDependencySpec(systemLocalLoader, systemLocalLoader.getPathSet(), true));
        return builder.create();
    }

    public String toString() {
        return "System Module Loader";
    }
}
