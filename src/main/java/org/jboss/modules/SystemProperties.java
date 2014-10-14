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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A context-aware system properties map.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class SystemProperties extends Properties {

    private static final long serialVersionUID = 8002159847898271570L;

    SystemProperties(final Properties defaults) {
        super(defaults);
    }

    protected Properties getDefaults() {
        return defaults;
    }

    Properties getDelegate() {
        final Class<?> callingClass = CallerContext.getCallingClass(SystemProperties.class, Properties.class, System.class);
        if (callingClass != null) {
            final Module module = Module.forClassLoader(callingClass.getClassLoader(), true);
            if (module != null) {
                return module.getProperties0();
            }
        }
        return getDefaults();
    }

    public Object setProperty(final String key, final String value) {
        return getDelegate().setProperty(key, value);
    }

    public void load(final Reader reader) throws IOException {
        getDelegate().load(reader);
    }

    public void load(final InputStream inStream) throws IOException {
        getDelegate().load(inStream);
    }

    @Deprecated
    public void save(final OutputStream out, final String comments) {
        getDelegate().save(out, comments);
    }

    public void store(final Writer writer, final String comments) throws IOException {
        getDelegate().store(writer, comments);
    }

    public void store(final OutputStream out, final String comments) throws IOException {
        getDelegate().store(out, comments);
    }

    public void loadFromXML(final InputStream in) throws IOException, InvalidPropertiesFormatException {
        getDelegate().loadFromXML(in);
    }

    public void storeToXML(final OutputStream os, final String comment) throws IOException {
        getDelegate().storeToXML(os, comment);
    }

    public void storeToXML(final OutputStream os, final String comment, final String encoding) throws IOException {
        getDelegate().storeToXML(os, comment, encoding);
    }

    public String getProperty(final String key) {
        return getDelegate().getProperty(key);
    }

    public String getProperty(final String key, final String defaultValue) {
        return getDelegate().getProperty(key, defaultValue);
    }

    public Enumeration<?> propertyNames() {
        return getDelegate().propertyNames();
    }

    public Set<String> stringPropertyNames() {
        return getDelegate().stringPropertyNames();
    }

    public void list(final PrintStream out) {
        getDelegate().list(out);
    }

    public void list(final PrintWriter out) {
        getDelegate().list(out);
    }

    public int size() {
        return getDelegate().size();
    }

    public boolean isEmpty() {
        return getDelegate().isEmpty();
    }

    public Enumeration<Object> keys() {
        return getDelegate().keys();
    }

    public Enumeration<Object> elements() {
        return getDelegate().elements();
    }

    public boolean contains(final Object value) {
        return getDelegate().contains(value);
    }

    public boolean containsValue(final Object value) {
        return getDelegate().containsValue(value);
    }

    public boolean containsKey(final Object key) {
        return getDelegate().containsKey(key);
    }

    public Object get(final Object key) {
        return getDelegate().get(key);
    }

    public Object put(final Object key, final Object value) {
        return getDelegate().put(key, value);
    }

    public Object remove(final Object key) {
        return getDelegate().remove(key);
    }

    public void putAll(final Map<?, ?> t) {
        getDelegate().putAll(t);
    }

    public void clear() {
        getDelegate().clear();
    }

    public Object clone() {
        return getDelegate().clone();
    }

    public String toString() {
        return getDelegate().toString();
    }

    public Set<Object> keySet() {
        return getDelegate().keySet();
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return getDelegate().entrySet();
    }

    public Collection<Object> values() {
        return getDelegate().values();
    }

    public boolean equals(final Object o) {
        return getDelegate().equals(o);
    }

    public int hashCode() {
        return getDelegate().hashCode();
    }
}
