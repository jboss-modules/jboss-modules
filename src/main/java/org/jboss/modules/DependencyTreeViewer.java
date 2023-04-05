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

    private static void print(PrintWriter out, String prefix, ModuleSpec spec, FastCopyHashSet<String> visited, File... roots) {
        if (spec instanceof AliasModuleSpec) {
            final AliasModuleSpec aliasModuleSpec = (AliasModuleSpec) spec;
            out.print(" -> ");
            final String aliasTarget = aliasModuleSpec.getAliasName();
            out.println(aliasTarget);
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
            final DependencySpec[] dependencies = filtered(ModuleDependencySpec[].class, concreteModuleSpec.getConcreteModuleVersionAndDependency().getDependencies());
            for (int i = 0, dependenciesLength = dependencies.length; i < dependenciesLength; i++) {
                print(out, prefix, dependencies[i], visited, i == dependenciesLength - 1, roots);
            }
        } else {
            out.println();
        }
    }

    private static void print(PrintWriter out, String prefix, DependencySpec spec, FastCopyHashSet<String> visited, final boolean last, final File... roots) {
        if (spec instanceof ModuleDependencySpec) {
            final ModuleDependencySpec moduleDependencySpec = (ModuleDependencySpec) spec;
            final String name = moduleDependencySpec.getName();
            out.print(prefix);
            out.print(last ? '└' : '├');
            out.print('─');
            out.print(' ');
            out.print(name);
            if (moduleDependencySpec.isOptional()) {
                out.print(" (optional)");
            }
            final PathFilter exportFilter = moduleDependencySpec.getExportFilter();
            if (! exportFilter.equals(PathFilters.rejectAll())) {
                out.print(" (exported)");
            }
            if (visited.add(name)) {
                print(out, prefix + (last ? "   " : "│  "), name, visited, roots);
            } else {
                out.println();
            }
        }
    }

    private static void print(PrintWriter out, String prefix, String name, FastCopyHashSet<String> visited, final File... roots) {
        final ModuleSpec moduleSpec;
        try {
            moduleSpec = LocalModuleFinder.parseModuleXmlFile(name, null, roots);
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
     * @deprecated Use {@link #print(PrintWriter, String, File...)} instead.
     */
    @Deprecated
    public static void print(PrintWriter out, ModuleIdentifier identifier, final File... roots) {
        print(out, identifier.toString(), roots);
    }

    /**
     * Print the dependency tree for the given module with the given module root list.
     *
     * @param out the output stream to use
     * @param name the name of the module to examine
     * @param roots the module roots to search
     */
    public static void print(PrintWriter out, String name, final File... roots) {
        out.print(name);
        print(out, "", name, new FastCopyHashSet<String>(), roots);
        out.flush();
    }
}
