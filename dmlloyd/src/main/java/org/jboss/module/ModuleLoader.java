/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.module;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class   ModuleLoader {

    private final Comparator<Version> versionComparator;

    private static final ModuleLoader INITIAL = SystemModuleLoader.getInstance();

    private static final ThreadLocal<Deque<ModuleLoader>> CURRENT = new ThreadLocal<Deque<ModuleLoader>>() {
        protected Deque<ModuleLoader> initialValue() {
            return new ArrayDeque<ModuleLoader>(Collections.singleton(INITIAL));
        }
    };

    public static final class Save {
        private final int depth;

        private Save() {
            depth = CURRENT.get().size();
        }

        public void clear() {
            final Deque<ModuleLoader> deque = CURRENT.get();
            while (deque.size() > depth) {
                deque.removeLast();
            }
        }
    }

    public static Save setCurrent(ModuleLoader loader) {
        try {
            return new Save();
        } finally {
            CURRENT.get().addLast(loader);
        }
    }

    public static ModuleLoader getCurrent() {
        return CURRENT.get().peekLast();
    }

    public final Module loadModule(final URI moduleUri) throws ModuleNotFoundException {
        final Module module = findModule(moduleUri);
        if (module == null) {
            throw new ModuleNotFoundException(moduleUri.toString());
        }
        return module;
    }

    public static Module loadModuleFromCurrent(final URI moduleUri) throws ModuleNotFoundException {
        return getCurrent().loadModule(moduleUri);
    }

    private final HashMap<URI, ModuleRef> moduleMap = new HashMap<URI, ModuleRef>();

    private static final class FutureModule {
        private Module module;

        Module get() throws InterruptedException {
            synchronized (this) {
                while (module == null) {
                    wait();
                }
                return module;
            }
        }

        void set(Module module) {
            synchronized (this) {
                this.module = module;
                notifyAll();
            }
        }
    }

    private final ReferenceQueue<Module> refQueue = new ReferenceQueue<Module>();

    private static final class ModuleRef extends SoftReference<Module> {
        private final URI key;

        private ModuleRef(final URI key, final Module value, final ReferenceQueue<? super Module> queue) {
            super(value, queue);
            this.key = key;
        }

        public URI getKey() {
            return key;
        }
    }

    protected ModuleLoader(final Comparator<Version> versionComparator) {
        this.versionComparator = versionComparator;
    }

    private static void clean(Map<URI, ?> map, ReferenceQueue<Module> queue) {
        Reference<? extends Module> ref;
        while ((ref = queue.poll()) != null) {
            if (ref instanceof ModuleRef) {
                final ModuleRef moduleRef = (ModuleRef) ref;
                map.remove(moduleRef.getKey());
            }
        }
    }

    /**
     *
     * @param moduleUri the module URI
     * @return {@code null} if the module isn't found
     * @throws ModuleNotFoundException if the module is found but a dependency is missing
     */
    protected abstract Module findModule(final URI moduleUri) throws ModuleNotFoundException;

    protected final Module defineModule(final URI moduleUri, final ModuleContentLoader loader, final List<URI> imports, final List<URI> exports, ClassFilter importClassFilter, ClassFilter exportClassFilter, final Module.Flag... flags) throws ModuleNotFoundException {
        return defineModule(moduleUri, loader, imports.toArray(new URI[imports.size()]), exports.toArray(new URI[exports.size()]), importClassFilter, exportClassFilter, flags);
    }

    protected final Module defineModule(final URI moduleUri, final ModuleContentLoader loader, final URI[] imports, final URI[] exports, ClassFilter importClassFilter, ClassFilter exportClassFilter, final Module.Flag... flags) throws ModuleNotFoundException {
        synchronized (moduleMap) {
            final SoftReference<Module> ref = moduleMap.get(moduleUri);
            if (ref != null) {
                final Module oldModule = ref.get();
                if (oldModule != null) {
                    throw new ModuleAlreadyExistsException();
                }
            }
            final ModuleLoader current = getCurrent();
            final Module[] importModules = new Module[imports.length];
            for (int i = 0; i < imports.length; i++) {
                importModules[i] = current.loadModule(imports[i]);
            }
            final Module[] exportModules = new Module[exports.length];
            for (int i = 0; i < exports.length; i++) {
                exportModules[i] = current.loadModule(exports[i]);
            }
            final Module module = new Module(loader, importModules, exportModules, importClassFilter, exportClassFilter, moduleUri, flags);
            moduleMap.put(moduleUri, new ModuleRef(moduleUri, module, refQueue));
            clean(moduleMap, refQueue);
        }
    }
}
