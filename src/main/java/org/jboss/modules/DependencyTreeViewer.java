/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

/**
 * A dependency tree viewer utility.  Prints out the dependency tree for a module.
 */
public final class DependencyTreeViewer {
    private static <I, O extends I> O[] filtered(Class<O[]> oType, I... inputs) {
        final I[] newArray = Arrays.copyOf(inputs, inputs.length);
        int o = 0;
        for (int i = 0; i < inputs.length; i ++) {
            if (oType.getComponentType().isInstance(inputs[i])) {
                newArray[o++] = (O) inputs[i];
            }
        }
        return Arrays.copyOf(newArray, o, oType);
    }

    private static void print(PrintWriter out, String prefix, ModuleSpec spec, FastCopyHashSet<ModuleIdentifier> visited, File... roots) {
        if (spec instanceof AliasModuleSpec) {
            final AliasModuleSpec aliasModuleSpec = (AliasModuleSpec) spec;
            out.print(" -> ");
            final ModuleIdentifier aliasTarget = aliasModuleSpec.getAliasTarget();
            out.print(aliasTarget);
            if (visited.add(aliasTarget)) {
                try {
                    final ModuleSpec moduleSpec = LocalModuleFinder.parseModuleXmlFile(aliasTarget, null, roots);
                    print(out, prefix, moduleSpec, visited);
                } catch (IOException e) {
                    out.println(e);
                } catch (ModuleLoadException e) {
                    out.println(e);
                }
            }
        } else if (spec instanceof ConcreteModuleSpec) {
            out.println();
            final ConcreteModuleSpec concreteModuleSpec = (ConcreteModuleSpec) spec;
            final DependencySpec[] dependencies = filtered(ModuleDependencySpec[].class, concreteModuleSpec.getDependencies());
            for (int i = 0, dependenciesLength = dependencies.length; i < dependenciesLength; i++) {
                print(out, prefix, dependencies[i], visited, i == dependenciesLength - 1, roots);
            }
        } else {
            out.println();
        }
    }

    private static void print(PrintWriter out, String prefix, DependencySpec spec, FastCopyHashSet<ModuleIdentifier> visited, final boolean last, final File... roots) {
        if (spec instanceof ModuleDependencySpec) {
            final ModuleDependencySpec moduleDependencySpec = (ModuleDependencySpec) spec;
            final ModuleIdentifier identifier = moduleDependencySpec.getIdentifier();
            out.print(prefix);
            out.print(last ? '└' : '├');
            out.print('─');
            out.print(' ');
            out.print(identifier);
            if (moduleDependencySpec.isOptional()) {
                out.print(" (optional)");
            }
            final PathFilter exportFilter = moduleDependencySpec.getExportFilter();
            if (! exportFilter.equals(PathFilters.rejectAll())) {
                out.print(" (exported)");
            }
            if (visited.add(identifier)) {
                print(out, prefix + (last ? "   " : "│  "), identifier, visited, roots);
            } else {
                out.println();
            }
        }
    }

    private static void print(PrintWriter out, String prefix, ModuleIdentifier identifier, FastCopyHashSet<ModuleIdentifier> visited, final File... roots) {
        final ModuleSpec moduleSpec;
        try {
            moduleSpec = LocalModuleFinder.parseModuleXmlFile(identifier, null, roots);
            if (moduleSpec == null) {
                out.println(" (not found)");
            } else {
                print(out, prefix, moduleSpec, visited, roots);
            }
        } catch (IOException e) {
            out.print(" (");
            out.print(e);
            out.println(")");
        } catch (ModuleLoadException e) {
            out.print(" (");
            out.print(e);
            out.println(")");
        }
    }

    /**
     * Print the dependency tree for the given module with the given module root list.
     *
     * @param out the output stream to use
     * @param identifier the identifier of the module to examine
     * @param roots the module roots to search
     */
    public static void print(PrintWriter out, ModuleIdentifier identifier, final File... roots) {
        out.print(identifier);
        print(out, "", identifier, new FastCopyHashSet<ModuleIdentifier>(), roots);
        out.flush();
    }
}
