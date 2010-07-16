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
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DependencySpec {
    private final ModuleIdentifier moduleIdentifier;
    private final boolean export;
    private final  boolean optional;
    private final String[] exportIncludes;
    private final String[] exportExcludes;

    public DependencySpec(ModuleIdentifier moduleIdentifier, boolean export, boolean optional, String[] exportIncludes, String[] exportExcludes) {
        this.moduleIdentifier = moduleIdentifier;
        this.export = export;
        this.optional = optional;
        this.exportIncludes = exportIncludes;
        this.exportExcludes = exportExcludes;
    }

    public boolean isExport() {
        return export;
    }

    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    public boolean isOptional() {
        return optional;
    }

    public ExportFilter getExportFilter() {
        return new ExportFilterImpl(exportIncludes, exportExcludes);
    }

    public interface Builder extends ExportFilterable<Builder> {
        Builder setExport(final boolean export);
        Builder setOptional(final boolean optional);
        Builder addExportInclude(final String path);
        Builder addExportExclude(final String path);
        DependencySpec create();
    }

    public static Builder build(final ModuleIdentifier moduleIdentifier) {
        return new Builder() {
            private boolean export;
            private boolean optional;
            private final List<String> exportIncludes = new ArrayList<String>();
            private final List<String> exportExcludes = new ArrayList<String>();

            public Builder setExport(final boolean export) {
                this.export = export;
                return this;
            }

            public Builder setOptional(final boolean optional) {
                this.optional = optional;
                return this;
            }

            public Builder addExportInclude(final String path) {
                exportIncludes.add(path);
                return this;
            }

            public Builder addExportExclude(final String path) {
                exportExcludes.add(path);
                return this;
            }

            @Override
            public DependencySpec create() {
                return new DependencySpec(moduleIdentifier, export, optional, exportIncludes.toArray(new String[exportIncludes.size()]), exportExcludes.toArray(new String[exportExcludes.size()]));
            }
        };
    }
}
