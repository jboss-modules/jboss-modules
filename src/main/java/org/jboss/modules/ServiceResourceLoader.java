/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 */
final class ServiceResourceLoader extends AbstractResourceLoader {
    private final Map<String, URLConnectionResource> servicesMap;

    ServiceResourceLoader(final Map<String, URLConnectionResource> map) {
        servicesMap = map;
    }

    @Override
    public Resource getResource(final String name) {
        if (name.startsWith("META-INF/services/")) {
            return servicesMap.get(name.substring("META-INF/services/".length()));
        }
        return null;
    }

    @Override
    public Collection<String> getPaths() {
        return Collections.singleton("META-INF/services");
    }

    private static final String PREFIX = "data:text/plain;charset=UTF-8,";

    static URLConnectionResource createResource(List<String> implNames) {
        int len = PREFIX.length();
        Iterator<String> iterator = implNames.iterator();
        if (iterator.hasNext()) {
            len = iterator.next().length();
            while (iterator.hasNext()) {
                len += 1 + iterator.next().length();
            }
        }
        if (len == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(len);
        sb.append(PREFIX);
        iterator = implNames.iterator();
        if (iterator.hasNext()) {
            sb.append(iterator.next());
            while (iterator.hasNext()) {
                sb.append('\n');
                sb.append(iterator.next());
            }
        }
        try {
            return new URLConnectionResource(new URL(sb.toString()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create URL", e);
        }
    }
}
