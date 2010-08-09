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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ModuleDependency extends Dependency {
    private final ModuleIdentifier identifier;
    private final boolean optional;
    private volatile Module cachedModule;

    ModuleDependency(final PathFilter exportFilter, final PathFilter importFilter, final ModuleIdentifier identifier, final boolean optional) {
        super(exportFilter, importFilter);
        this.identifier = identifier;
        this.optional = optional;
    }

    ModuleIdentifier getIdentifier() {
        return identifier;
    }

    boolean isOptional() {
        return optional;
    }

    void accept(final DependencyVisitor visitor) throws ModuleLoadException {
        visitor.visit(this);
    }

    Module getModule() {
        final Module cachedModule = this.cachedModule;
        if (cachedModule == null) {
            try {
                return (this.cachedModule = Module.getCurrentLoaderPrivate().preloadModule(identifier));
            } catch (ModuleLoadException e) {
                return null;
            }
        } else {
            return cachedModule;
        }
    }

    Module getModuleRequired() throws ModuleNotFoundException {
        final Module module = getModule();
        if (module == null) {
            if (! isOptional()) {
                throw new ModuleNotFoundException("Module " + identifier + " not found");
            }
        }
        return module;
    }
}
