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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;


/**
 * Module Specification.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleSpec {
    private final DependencySpec[] dependencies;
    private final Set<Module.Flag> moduleFlags;

    private final ModuleIdentifier moduleIdentifier;
    private final String mainClass;
    private final ModuleContentLoader loader;
    private final AssertionSetting assertionSetting;

    public ModuleSpec(ModuleIdentifier moduleIdentifier, DependencySpec[] dependencies, ModuleContentLoader loader, Set<Module.Flag> moduleFlags, AssertionSetting assertionSetting, String mainClass) {
        this.moduleIdentifier = moduleIdentifier;
        this.dependencies = dependencies;
        this.loader = loader;
        this.moduleFlags = moduleFlags;
        this.assertionSetting = assertionSetting;
        this.mainClass = mainClass;
    }

    public ModuleIdentifier getIdentifier() {
        return moduleIdentifier;
    }

    public DependencySpec[] getDependencies() {
        return dependencies.clone();
    }

    public String getMainClass() {
        return mainClass;
    }

    public ModuleContentLoader getContentLoader() {
        return loader;
    }

    public Set<Module.Flag> getModuleFlags() {
        return moduleFlags;
    }

    public AssertionSetting getAssertionSetting() {
        return assertionSetting;
    }

    public interface Builder {
        Builder setMainClass(final String mainClass);
        Builder setAssertionSetting(AssertionSetting assertionSetting);
        Builder addModuleFlag(Module.Flag flag);
        DependencySpec.Builder addDependency(final ModuleIdentifier moduleIdentifier);
        Builder addRoot(final String rootName, final ResourceLoader resourceLoader);
        ModuleSpec create();
        ModuleIdentifier getIdentifier();
    }

    public static Builder build(final ModuleIdentifier moduleIdentifier) {
        return new Builder() {
            private final Set<Module.Flag> moduleFlags = EnumSet.noneOf(Module.Flag.class);
            private String mainClass;
            private AssertionSetting assertionSetting = AssertionSetting.INHERIT;

            private final ModuleContentLoader.Builder moduleContentLoaderBuilder = ModuleContentLoader.build();
            private final Set<DependencySpec.Builder> dependencyBuilders = new HashSet<DependencySpec.Builder>();

            public Builder setMainClass(final String mainClass) {
                this.mainClass = mainClass;
                return this;
            }

            @Override
            public Builder setAssertionSetting(AssertionSetting assertionSetting) {
                this.assertionSetting = assertionSetting;
                return this;
            }

            @Override
            public Builder addModuleFlag(Module.Flag flag) {
                moduleFlags.add(flag);
                return this;
            }

            public DependencySpec.Builder addDependency(final ModuleIdentifier moduleIdentifier) {
                final DependencySpec.Builder dependencySpecBuilder = DependencySpec.build(moduleIdentifier);
                dependencyBuilders.add(dependencySpecBuilder);
                return dependencySpecBuilder;
            }

            public Builder addRoot(final String rootName, final ResourceLoader resourceLoader) {
                moduleContentLoaderBuilder.add(rootName, resourceLoader);
                return this;
            }

            public ModuleSpec create() {
                final ModuleContentLoader.Builder moduleContentLoaderBuilder = this.moduleContentLoaderBuilder;
                final Set<DependencySpec.Builder> dependencyBuilders = this.dependencyBuilders;
                final DependencySpec[] dependencySpecs = new DependencySpec[dependencyBuilders.size()];
                int i = 0;
                for(DependencySpec.Builder dependencyBuilder : dependencyBuilders) {
                    dependencySpecs[i++] = dependencyBuilder.create();
                }
                return new ModuleSpec(moduleIdentifier, dependencySpecs, moduleContentLoaderBuilder.create(), moduleFlags, assertionSetting, mainClass);
            }

            @Override
            public ModuleIdentifier getIdentifier() {
                return moduleIdentifier;
            }
        };
    }
}
