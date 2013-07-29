/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The linkage state of a module.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class Linkage {

    private static final Dependency[] NO_DEPENDENCIES = new Dependency[0];
    private static final DependencySpec[] NO_DEPENDENCY_SPECS = new DependencySpec[0];

    enum State {
        NEW,
        UNLINKED,
        LINKING,
        LINKED,
        ;
    }

    private final DependencySpec[] dependencySpecs;
    private final Dependency[] dependencies;

    private final State state;
    private final Map<String, List<LocalLoader>> allPaths;

    Linkage(final State state) {
        this(NO_DEPENDENCY_SPECS, NO_DEPENDENCIES, state, Collections.<String, List<LocalLoader>>emptyMap());
    }

    Linkage(final DependencySpec[] dependencySpecs, final Dependency[] dependencies, final State state) {
        this(dependencySpecs, dependencies, state, Collections.<String, List<LocalLoader>>emptyMap());
    }

    Linkage(final DependencySpec[] dependencySpecs, final Dependency[] dependencies, final State state, final Map<String, List<LocalLoader>> allPaths) {
        this.dependencySpecs = dependencySpecs;
        this.dependencies = dependencies;
        this.state = state;
        this.allPaths = allPaths;
    }

    Map<String, List<LocalLoader>> getPaths() {
        return allPaths;
    }

    State getState() {
        return state;
    }

    Dependency[] getDependencies() {
        return dependencies;
    }

    DependencySpec[] getDependencySpecs() {
        return dependencySpecs;
    }

    static final Linkage NONE = new Linkage(State.NEW);
}
