/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
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

/**
 * A logger finder which attempts to locate a {@link LoggerFinder} on the
 * {@linkplain java.util.logging.LogManager log managers} class path. If not found a default finder will be used.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class ModuleLoggerFinder {

    /**
     * Sets the class loader to use when attempting to find the {@link LoggerFinder}.
     *
     * @param cl the class loader to use
     */
    static void setClassLoader(final ClassLoader cl) {
        // Do nothing, this is only used for JDK 9+
    }
}
