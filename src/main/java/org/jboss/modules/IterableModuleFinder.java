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
import java.util.Iterator;

/**
 * A module finder which is iterable.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface IterableModuleFinder extends ModuleFinder {

    /**
     * This method returns an empty iterator and should not be used by new code.
     *
     * @param baseIdentifier the identifier to start with, or {@code null} to iterate all modules
     * @param recursive {@code true} to find recursively nested modules, {@code false} to only find immediately nested
     * modules
     * @return an iterator for the modules in this module finder
     * @deprecated Use {@link #iterateModules(String, boolean, ModuleLoader)} instead.
     */
    @Deprecated
    default Iterator<ModuleIdentifier> iterateModules(ModuleIdentifier baseIdentifier, boolean recursive) {
        return Collections.emptyIterator();
    }

    /**
     * Iterate the modules which can be located via this module finder.
     *
     * @param baseName the identifier to start with, or {@code null} to iterate all modules; ignored if this module
     * loader does not have a concept of nested modules
     * @param recursive {@code true} to find recursively nested modules, {@code false} to only find immediately nested
     * modules; ignored if this module finder does not have a concept of nested modules
     * @return an iterator for the modules in this module finder
     * @deprecated Use {@link #iterateModules(String, boolean, ModuleLoader)} instead.
     */
    @Deprecated
    default Iterator<String> iterateModules(String baseName, boolean recursive) {
        return iterateModules(baseName, recursive, null);
    }

    default Iterator<String> iterateModules(String baseName, boolean recursive, final ModuleLoader delegateLoader) {
        return IteratorUtils.transformingIterator(iterateModules(baseName == null ? null : ModuleIdentifier.fromString(baseName), recursive), ModuleIdentifier::toString);
    }
}
