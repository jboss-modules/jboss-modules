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
        this.allPaths = PathUtils.deduplicateLists(allPaths);
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
