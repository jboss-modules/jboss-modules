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
 * A local loader which can enumerate its contents.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface IterableLocalLoader extends LocalLoader {

    /**
     * Enumerate all the resources under the given path.  The given path name is relative to the root
     * of the resource loader.  If the path "escapes" the root via {@code ..}, such segments will be consumed.
     * If the path is absolute, it will be converted to a relative path by dropping the leading {@code /}.
     *
     * @param startPath the path to search under
     * @param recursive {@code true} to recursively descend into subdirectories, {@code false} to only read this path
     * @return the resource iterator (possibly empty)
     */
    Iterator<Resource> iterateResources(String startPath, boolean recursive);
}
