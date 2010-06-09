/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

/**
 * Contract establishing the ability to include and exclude paths.
 *
 * @author John Bailey
 */
public interface ExportFilterable {
    /**
     * Add a path glob to be included
     *
     * @param path the path glob
     */
    void addExportInclude(String path);

    /**
     * Add a path glob to be excluded
     *
     * @param path the path glob
     */
    void addExportExclude(String path);

    /**
     * Get an ExportFilter for this resource
     *
     * @return the export filter
     */
    ExportFilter getExportFilter();
}
