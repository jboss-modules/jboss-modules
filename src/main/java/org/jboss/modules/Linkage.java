/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

    enum State {
        NEW,
        UNLINKED,
        LINKING,
        LINKED,
        ;
    }

    private final Dependency[] sourceList;
    private final State state;
    private final Map<String, List<LocalLoader>> allPaths;
    private final Map<String, List<LocalLoader>> exportedPaths;

    Linkage(final Dependency[] sourceList, final State state) {
        this(sourceList, state, Collections.<String, List<LocalLoader>>emptyMap(), Collections.<String, List<LocalLoader>>emptyMap());
    }

    Linkage(final Dependency[] sourceList, final State state, final Map<String, List<LocalLoader>> allPaths, final Map<String, List<LocalLoader>> exportedPaths) {
        this.sourceList = sourceList;
        this.state = state;
        this.allPaths = allPaths;
        this.exportedPaths = exportedPaths;
    }

    Map<String, List<LocalLoader>> getAllPaths() {
        return allPaths;
    }

    Map<String, List<LocalLoader>> getExportedPaths() {
        return exportedPaths;
    }

    Map<String, List<LocalLoader>> getPaths(boolean export) {
        return export ? exportedPaths : allPaths;
    }

    State getState() {
        return state;
    }

    Dependency[] getSourceList() {
        return sourceList;
    }

    static final Linkage NONE = new Linkage(NO_DEPENDENCIES, State.NEW, Collections.<String, List<LocalLoader>>emptyMap(), Collections.<String, List<LocalLoader>>emptyMap());
}
