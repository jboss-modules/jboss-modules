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
 * A dependency specification that represents dependencies on a module.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author Jason T. Greene
 */
public final class DependencySpec {
    List<DependencySpec.SpecifiedDependency> dependencies;

    DependencySpec(List<DependencySpec.SpecifiedDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public static DependencyBuilder build() {
        return new DependencyBuilderImpl();
    }

    public interface DependencyBuilderBase {
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
        DependencyBuilderBase addLocalDependency(final LocalDependencySpec spec);

        /**
         * Add a local dependency representing the module itself.  If not specified, the module is assumed to be the
         * last dependency (child-last).  This dependency includes all of the resource roots defined by {@link #addResourceRoot(ResourceLoader)}.
         * It is an error to add this dependency more than one time.
         *
         * @return this builder
         */
        DependencyBuilderBase addLocalDependency();

        /**
         * Add a module dependency with the given specification.
         *
         * @param dependencySpec the dependency specification (see {@link ModuleDependencySpec#build(ModuleIdentifier)})
         * @return this builder
         */
        DependencyBuilderBase addModuleDependency(ModuleDependencySpec dependencySpec);

    }

    public interface DependencyBuilder extends DependencyBuilderBase {
        /** {@inheritDoc} */
        @Override
        DependencyBuilder addLocalDependency(final LocalDependencySpec spec);

        /** {@inheritDoc} */
        @Override
        DependencyBuilder addLocalDependency();

        /** {@inheritDoc} */
        @Override
        DependencyBuilder addModuleDependency(ModuleDependencySpec dependencySpec);

        /**
         * Creates the dependency specification from this builder
         *
         * @return the dependecy spec
         */
        DependencySpec create();
    }

    static class DependencyBuilderImpl implements DependencyBuilder {
        private final List<DependencySpec.SpecifiedDependency> dependencies = new ArrayList<DependencySpec.SpecifiedDependency>(0);
        boolean localAdded;

        public DependencyBuilder addLocalDependency() {
            if (localAdded) {
                throw new IllegalStateException("Local dependency already added");
            }
            localAdded = true;
            dependencies.add(new DependencySpec.SpecifiedDependency() {
                public Dependency getDependency(final Module module) {
                    final ModuleClassLoader moduleClassLoader = module.getClassLoaderPrivate();
                    return new LocalDependency(moduleClassLoader.getExportPathFilter(), PathFilters.acceptAll(), moduleClassLoader.getLocalLoader(), moduleClassLoader.getPaths());
                }
            });
            return this;
        }

        public DependencyBuilder addLocalDependency(final LocalDependencySpec spec) {
            dependencies.add(new ImmediateSpecifiedDependency(
                    new LocalDependency(spec.getExportFilter(), spec.getImportFilter(), spec.getLocalLoader(), spec.getLoaderPaths())));
            return this;
        }

        public DependencyBuilder addModuleDependency(final ModuleDependencySpec dependencySpec) {
            final ModuleIdentifier identifier = dependencySpec.getModuleIdentifier();
            dependencies.add(new DependencySpec.SpecifiedDependency() {
                public Dependency getDependency(final Module module) {
                    return new ModuleDependency(dependencySpec.getExportFilter(), dependencySpec.getImportFilter(), identifier, dependencySpec.isOptional());
                }
            });
            return this;
        }

        public DependencySpec create() {
            if (! localAdded) {
                addLocalDependency();
            }

            return new DependencySpec(dependencies);
        }
    }

    interface SpecifiedDependency {
        Dependency getDependency(Module module);
    }

    static final class ImmediateSpecifiedDependency implements DependencySpec.SpecifiedDependency {
        private final Dependency dependency;

        ImmediateSpecifiedDependency(final Dependency dependency) {
            this.dependency = dependency;
        }

        public Dependency getDependency(final Module module) {
            return dependency;
        }
    }
}
