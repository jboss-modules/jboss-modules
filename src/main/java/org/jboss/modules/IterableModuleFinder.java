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

import java.util.Iterator;

/**
 * A module finder which is iterable.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface IterableModuleFinder extends ModuleFinder {

    /**
     * Iterate the modules which can be located via this module finder.
     *
     * @param baseIdentifier the identifier to start with, or {@code null} to iterate all modules
     * @param recursive {@code true} to find recursively nested modules, {@code false} to only find immediately nested modules
     * @return an iterator for the modules in this module finder
     */
    Iterator<ModuleIdentifier> iterateModules(ModuleIdentifier baseIdentifier, boolean recursive);
}
