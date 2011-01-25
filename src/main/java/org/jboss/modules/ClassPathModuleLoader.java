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

import java.net.URL;
import java.net.URLClassLoader;

/**
 * <p>
 * A {@link ModuleLoader <code>ModuleLoader</code>} for loading {@link Module <code>Modules</code>} from a repository
 * located on the classpath.
 * </p>
 * <p>
 * Per default <code>ClassPathModuleLoader</code> uses the <i>application class loader</i> (also called <i>system class
 * loader</i>) and expects <code>Modules</code> to be found beneath the &quot;modules/&quot;-directory on the class
 * path. Both of these setting, however, may be overridden at instantiation time.
 * </p>
 *
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 */
public final class ClassPathModuleLoader extends ModuleLoader {

    private class ClassPathResourceLoaderFactory implements ResourceLoaderFactory {

        @Override
        public ResourceLoader create(ModuleIdentifier moduleIdentifier, URL moduleRoot, String resourceRootName,
                                     String resourceRootPath) throws IllegalArgumentException,
                NoResourceLoaderForResourceRootException {
            final String resourceRootOnClassPath = toPathOnClassPath(moduleIdentifier) + resourceRootPath;
            // TODO: We should really check if the resource root passed in is indeed valid.
            return ResourceLoaders.createClassPathResourceLoader(
                    resourceRootName, resourceRootOnClassPath, ClassPathModuleLoader.this.classLoaderDelegate);
        }
    }

    private static final String DEFAULT_REPOSITORY_ROOT_ON_CLASSPATH = "modules/";

    private final ClassLoader classLoaderDelegate;

    private final ModuleXmlParser moduleXmlParser;

    private final String repositoryRootOnClassPath;

    /**
     * @param classLoaderDelegate
     * @param repositoryRootOnClassPath
     * @throws IllegalArgumentException
     */
    public ClassPathModuleLoader(final ClassLoader classLoaderDelegate, final String repositoryRootOnClassPath) throws IllegalArgumentException {
        if (classLoaderDelegate == null) throw new IllegalArgumentException("classLoaderDelegate is null");
        this.classLoaderDelegate = classLoaderDelegate;
        this.repositoryRootOnClassPath = repositoryRootOnClassPath != null ? repositoryRootOnClassPath : DEFAULT_REPOSITORY_ROOT_ON_CLASSPATH;
        this.moduleXmlParser = new ModuleXmlParser(this.new ClassPathResourceLoaderFactory());
    }

    /**
     * @param classLoaderDelegate
     * @throws IllegalArgumentException
     */
    public ClassPathModuleLoader(final ClassLoader classLoaderDelegate) throws IllegalArgumentException {
        this(classLoaderDelegate, null);
    }

    /**
     * Instantiates a <code>ClassPathModuleLoader</code> that uses the application classloader for loading
     * <code>Modules</code>.
     *
     * @param repositoryRootOnClassPath
     */
    public ClassPathModuleLoader(final String repositoryRootOnClassPath) {
        this(Main.class.getClassLoader(), repositoryRootOnClassPath);
    }

    /**
     * Instantiates a <code>ClassPathModuleLoader</code> that uses the application classloader for loading
     * <code>Modules</code>.
     */
    public ClassPathModuleLoader() {
        this(Main.class.getClassLoader(), null);
    }

    /**
     * @see org.jboss.modules.ModuleLoader#findModule(org.jboss.modules.ModuleIdentifier)
     */
    @Override
    protected ModuleSpec findModule(ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        final String moduleRootOnClassPath = toPathOnClassPath(moduleIdentifier);
        final URL moduleRootUrl = toClassPathUrl(moduleRootOnClassPath);
        if (moduleRootUrl == null) {
            throw new ModuleNotFoundException("No module root found at [" + moduleRootOnClassPath + "] on classpath");
        }

        final String moduleInfoOnClassPath = moduleRootOnClassPath + "module.xml";
        final URL moduleInfoUrl = toClassPathUrl(moduleInfoOnClassPath);
        if (moduleInfoUrl == null) {
            throw new ModuleNotFoundException("No 'module.xml' found at [" + moduleInfoOnClassPath + "] on classpath");
        }

        return this.moduleXmlParser.parse(moduleIdentifier, moduleRootUrl, moduleInfoUrl);
    }

    private String toPathOnClassPath(final ModuleIdentifier moduleIdentifier) {
        final StringBuilder builder = new StringBuilder(this.repositoryRootOnClassPath);
        builder.append(moduleIdentifier.getName().replace('.', '/'));
        builder.append('/').append(moduleIdentifier.getSlot());
        builder.append('/');
        return builder.toString();
    }

    private URL toClassPathUrl(final String pathOnClassPath) {
        return this.classLoaderDelegate.getResource(pathOnClassPath);
    }

    /**
     * @see org.jboss.modules.ModuleLoader#toString()
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("classpath module loader @").append(Integer.toHexString(hashCode()));
        if (this.classLoaderDelegate instanceof URLClassLoader) {
            b.append(" (URLs: ");
            final URL[] classPathUrls = URLClassLoader.class.cast(this.classLoaderDelegate).getURLs();
            for (final URL classPathUrl : classPathUrls) {
                b.append(classPathUrl.toString()).append(',');
            }
            b.deleteCharAt(b.length() - 1);
            b.append(')');
        }
        return b.toString();
    }
}

