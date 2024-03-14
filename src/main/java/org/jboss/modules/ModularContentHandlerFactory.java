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

import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ModularContentHandlerFactory implements ContentHandlerFactory {
    private static final PrivilegedAction<String> CONTENT_MODULES_LIST_ACTION = new PropertyReadAction("jboss.content.handler.modules");

    private static final List<Module> modules;

    static {
        CopyOnWriteArrayList<Module> list = new CopyOnWriteArrayList<>();
        final SecurityManager sm = System.getSecurityManager();
        final String urlModulesList;
        if (sm != null) {
            urlModulesList = AccessController.doPrivileged(CONTENT_MODULES_LIST_ACTION);
        } else {
            urlModulesList = CONTENT_MODULES_LIST_ACTION.run();
        }
        if (urlModulesList != null) {
            final List<Module> moduleList = new ArrayList<>();
            int f = 0;
            int i;
            do {
                i = urlModulesList.indexOf('|', f);
                final String moduleId = (i == -1 ? urlModulesList.substring(f) : urlModulesList.substring(f, i)).trim();
                if (! moduleId.isEmpty()) {
                    try {
                        Module module = Module.getBootModuleLoader().loadModule(moduleId);
                        moduleList.add(module);
                    } catch (RuntimeException | ModuleLoadException e) {
                        // skip it
                    }
                }
                f = i + 1;
            } while (i != -1);
            list.addAll(moduleList);
        }
        modules = list;
    }

    static final ModularContentHandlerFactory INSTANCE = new ModularContentHandlerFactory();

    static void addHandlerModule(Module module) {
        modules.add(module);
    }

    public ContentHandler createContentHandler(final String mimeType) {
        for (Module module : modules) {
            ServiceLoader<ContentHandlerFactory> loader = module.loadService(ContentHandlerFactory.class);
            for (ContentHandlerFactory factory : loader) try {
                final ContentHandler handler = factory.createContentHandler(mimeType);
                if (handler != null) {
                    return handler;
                }
            } catch (RuntimeException e) {
                // ignored
            }
        }
        return null;
    }
}
