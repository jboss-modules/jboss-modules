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
