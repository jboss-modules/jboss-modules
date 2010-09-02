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

import java.util.ArrayList;
import java.util.List;


/**
 * A {@code Module} specification which is used by a {@code ModuleLoader} to define new modules.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleSpec {

    private final ModuleIdentifier moduleIdentifier;
    private final String mainClass;
    private final AssertionSetting assertionSetting;
    private final ResourceLoader[] resourceLoaders;
    private final SpecifiedDependency[] dependencies;
    private final LocalLoader fallbackLoader;

    private ModuleSpec(final ModuleIdentifier moduleIdentifier, final String mainClass, final AssertionSetting assertionSetting, final ResourceLoader[] resourceLoaders, final SpecifiedDependency[] dependencies, final LocalLoader fallbackLoader) {
        this.moduleIdentifier = moduleIdentifier;
        this.mainClass = mainClass;
        this.assertionSetting = assertionSetting;
        this.resourceLoaders = resourceLoaders;
        this.dependencies = dependencies;
        this.fallbackLoader = fallbackLoader;
    }

    /**
     * Get the module identifier for the module which is specified by this object.
     *
     * @return the module identifier
     */
    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    String getMainClass() {
        return mainClass;
    }

    AssertionSetting getAssertionSetting() {
        return assertionSetting;
    }

    ResourceLoader[] getResourceLoaders() {
        return resourceLoaders;
    }

    SpecifiedDependency[] getDependencies() {
        return dependencies;
    }

    LocalLoader getFallbackLoader() {
        return fallbackLoader;
    }

    /**
     * A builder for new module specifications.
     */
    public interface Builder {

        /**
         * Set the main class for this module, or {@code null} for none.
         *
         * @param mainClass the main class name
         * @return this builder
         */
        Builder setMainClass(String mainClass);

        /**
         * Set the default assertion setting for this module.
         *
         * @param assertionSetting the assertion setting
         * @return this builder
         */
        Builder setAssertionSetting(AssertionSetting assertionSetting);

        /**
         * Add a non-module local dependency.  See the documentation for {@link LocalLoader} for more information
         * about non-module dependencies.
         * <p>
         * It is recommended that rather than including non-module dependencies in one or more module definitions, a
         * module should be defined to encapsulate the non-module dependency, so that other modules can depend on it as
         * a module.
         *
         * @param spec the specification for the local dependency
         * @return this builder
         */
        Builder addLocalDependency(final LocalDependencySpec spec);

        /**
         * Add a local dependency representing the module itself.  If not specified, the module is assumed to be the
         * last dependency (child-last).  This dependency includes all of the resource roots defined by {@link #addResourceRoot(ResourceLoader)}.
         * It is an error to add this dependency more than one time.
         *
         * @return this builder
         */
        Builder addLocalDependency();

        /**
         * Add a module dependency with the given specification.
         *
         * @param dependencySpec the dependency specification (see {@link ModuleDependencySpec#build(ModuleIdentifier)})
         * @return this builder
         */
        Builder addModuleDependency(ModuleDependencySpec dependencySpec);

        /**
         * Add a local resource root, from which this module will load class definitions and resources.
         *
         * @param resourceLoader the resource loader for the root
         * @return this builder
         */
        Builder addResourceRoot(ResourceLoader resourceLoader);

        /**
         * Create the module specification from this builder.
         *
         * @return the module specification
         */
        ModuleSpec create();

        /**
         * Get the identifier of the module being defined by this builder.
         *
         * @return the module identifier
         */
        ModuleIdentifier getIdentifier();
    }

    /**
     * Get a builder for a new module specification.
     *
     * @param moduleIdentifier the module identifier
     * @return the builder
     */
    public static Builder build(final ModuleIdentifier moduleIdentifier) {
        return new Builder() {
            private boolean localAdded;
            private String mainClass;
            private AssertionSetting assertionSetting = AssertionSetting.INHERIT;
            private final List<ResourceLoader> resourceLoaders = new ArrayList<ResourceLoader>(0);
            private final List<SpecifiedDependency> dependencies = new ArrayList<SpecifiedDependency>(0);
            private LocalLoader fallbackLoader;

            public Builder setFallbackDependency(final LocalLoader fallbackLoader) {
                this.fallbackLoader = fallbackLoader;
                return this;
            }

            public Builder addLocalDependency(final LocalDependencySpec spec) {
                dependencies.add(new ImmediateSpecifiedDependency(
                        new LocalDependency(spec.getExportFilter(), spec.getImportFilter(), spec.getLocalLoader(), spec.getLoaderPaths())));
                return this;
            }

            public Builder addLocalDependency() {
                if (localAdded) {
                    throw new IllegalStateException("Local dependency already added");
                }
                localAdded = true;
                dependencies.add(new SpecifiedDependency() {
                    public Dependency getDependency(final Module module) {
                        final ModuleClassLoader moduleClassLoader = module.getClassLoaderPrivate();
                        return new LocalDependency(moduleClassLoader.getExportPathFilter(), PathFilters.acceptAll(), moduleClassLoader.getLocalLoader(), moduleClassLoader.getPaths());
                    }
                });
                return this;
            }

            public Builder addModuleDependency(final ModuleDependencySpec dependencySpec) {
                final ModuleIdentifier identifier = dependencySpec.getModuleIdentifier();
                dependencies.add(new SpecifiedDependency() {
                    public Dependency getDependency(final Module module) {
                        return new ModuleDependency(dependencySpec.getExportFilter(), dependencySpec.getImportFilter(), identifier, dependencySpec.isOptional());
                    }
                });
                return this;
            }

            @Override
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
            public Builder addResourceRoot(final ResourceLoader resourceLoader) {
                resourceLoaders.add(resourceLoader);
                return this;
            }

            @Override
            public ModuleSpec create() {
                if (! localAdded) {
                    addLocalDependency();
                }
                return new ModuleSpec(moduleIdentifier, mainClass, assertionSetting, resourceLoaders.toArray(new ResourceLoader[resourceLoaders.size()]), dependencies.toArray(new SpecifiedDependency[dependencies.size()]), fallbackLoader);
            }

            @Override
            public ModuleIdentifier getIdentifier() {
                return moduleIdentifier;
            }
        };
    }

    interface SpecifiedDependency {
        Dependency getDependency(Module module);
    }

    static final class ImmediateSpecifiedDependency implements SpecifiedDependency {
        private final Dependency dependency;

        ImmediateSpecifiedDependency(final Dependency dependency) {
            this.dependency = dependency;
        }

        public Dependency getDependency(final Module module) {
            return dependency;
        }
    }
}
