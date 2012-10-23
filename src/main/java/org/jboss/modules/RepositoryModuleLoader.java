/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.modules;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * {@link ModuleLoader} implementation which loads from a local module repository. If no matching module is found
 * locally, an attempt will be made to resolve module resources from a (possibly remote) repository accessible via a
 * root {@link URL}.
 *
 * <br />
 * <br />
 *
 * The default location of the local module repository is located on the filesystem at
 * <code>$USER_HOME/.jboss/modules/repo</code>, unless overridden by explicitly-providing a location via
 * {@link RepositoryModuleLoader#create(URL, File)}. New {@link RepositoryModuleLoader} instances with the default local
 * repository may be created using {@link RepositoryModuleLoader#create(URL)}.
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 */
public final class RepositoryModuleLoader extends ModuleLoader {

    private static final Logger log = Logger.getLogger(RepositoryModuleLoader.class.getName());
    private static final String DEFAULT_NAME_REPOSITORY = ".jboss" + File.separatorChar + "modules"
        + File.separatorChar + "repo";
    private static final File USER_HOME_DIR = new File(SecurityActions.getSystemProperty("user.home"));
    private static final File DEFAULT_LOCAL_REPO = new File(USER_HOME_DIR, DEFAULT_NAME_REPOSITORY);
    private static final String NAME_MODULES_DESCRIPTOR = "module.xml";
    private static final String ELEMENT_NAME_RESOURCE_ROOT = "resource-root";
    private static final String ELEMENT_NAME_DEPENDENCIES = "dependencies";
    private static final String ELEMENT_NAME_MODULE = "module";
    private static final String ATTRIBUTE_NAME_PATH = "path";
    private static final String ATTRIBUTE_NAME_NAME = "name";
    private static final String SUFFIX_INDEX = ".index";

    /**
     * Root of the (possibly) remote repository from which to fetch modules
     */
    private final URL rootUrl;

    /**
     * Root of the local module repository
     */
    final File localRepoRoot;

    /**
     * Delegate used to load modules from the local module repository
     */
    private final LocalModuleLoader localDelegate;

    /**
     * Creates a new instance using the specified root {@link URL} for the backing repository and the specified
     * {@link File} root for the local repository. Both arguments are required, and the local repository root must both
     * exist and be a directory, else {@link IllegalArgumentException} will be raised.
     *
     * @param rootUrl
     * @param localRepoRoot
     */
    private RepositoryModuleLoader(final URL rootUrl, final File localRepoRoot) throws IllegalArgumentException {
        if (rootUrl == null) {
            throw new IllegalArgumentException("Root URL must be specified");
        }
        if (localRepoRoot == null) {
            throw new IllegalArgumentException("Local Repository Root must be specified");
        }
        if (!localRepoRoot.exists()) {
            throw new IllegalArgumentException("Local Repository Root must exist: " + localRepoRoot);
        }
        if (!localRepoRoot.isDirectory()) {
            throw new IllegalArgumentException("Local Repository Root must be a directory: " + localRepoRoot);
        }
        this.localRepoRoot = localRepoRoot;
        this.rootUrl = rootUrl;
        this.localDelegate = new LocalModuleLoader(new File[] { localRepoRoot.getAbsoluteFile() });
    }

    /**
     * Creates a new {@link RepositoryModuleLoader} instance using the specified root URL from which to fetch modules
     * and the specified local repository root to cache downloaded modules.
     *
     * @param rootUrl
     * @param localRepoRoot
     * @return
     * @throws IllegalArgumentException
     *             If either argument is not specified, or if the local repository root does not exist or is not a
     *             directory
     */
    public static RepositoryModuleLoader create(final URL rootUrl, final File localRepoRoot)
        throws IllegalArgumentException {
        return new RepositoryModuleLoader(rootUrl, localRepoRoot);
    }

    /**
     * Creates a new {@link RepositoryModuleLoader} instance using the specified root URL from which to fetch modules
     * and the default local repository root (USER_HOME_DIR/.jboss/modules/repo) to cache downloaded modules.
     *
     * @param rootUrl
     * @return
     * @throws IllegalArgumentException
     *             If the root {@link URL} is not specified
     */
    public static RepositoryModuleLoader create(final URL rootUrl) throws IllegalArgumentException {
        // Create default local repo if it doesn't exist
        if (!DEFAULT_LOCAL_REPO.exists() && !DEFAULT_LOCAL_REPO.mkdirs()) {
            throw new RuntimeException("Could not create default local repository: "
                + DEFAULT_LOCAL_REPO.getAbsolutePath());

        }
        return new RepositoryModuleLoader(rootUrl, DEFAULT_LOCAL_REPO);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modules.ModuleLoader#toString()
     */
    @Override
    public String toString() {
        return RepositoryModuleLoader.class.getSimpleName() + " with root URL: " + this.rootUrl
            + " and using local module repository root " + localRepoRoot.getAbsolutePath();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modules.ModuleLoader#preloadModule(org.jboss.modules.ModuleIdentifier)
     */
    @Override
    protected Module preloadModule(final ModuleIdentifier identifier) throws ModuleLoadException {

        Module module;
        try {
            module = ModuleLoader.preloadModule(identifier, localDelegate);
        } catch (final ModuleNotFoundException mnfe) {
            // Fetch and try again
            this.fetchModuleAssets(identifier);
            module = ModuleLoader.preloadModule(identifier, localDelegate);
        }

        // Return
        return module;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modules.ModuleLoader#findModule(org.jboss.modules.ModuleIdentifier)
     */
    @Override
    protected ModuleSpec findModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        // We never load the module, the local delegate does via preloadModule
        // FIXME This mechanism really breaks the stated purpose of the SPI, raise the issue to DML
        throw new UnsupportedOperationException("Should never be reached, we use a delegate loader");
    }

    /**
     * Obtains all module resources for the given (required) {@link ModuleIdentifier}. Will recurse to obtain dependent
     * module assets as well.
     *
     * @param moduleIdentifier
     */
    private void fetchModuleAssets(final ModuleIdentifier moduleIdentifier) {

        assert moduleIdentifier != null : "Module identifier is required";

        // Fetch the module.xml into the local modules repo
        final String relativePath = toHttpPathString(moduleIdentifier);
        final File localRepoModuleRoot = new File(localRepoRoot, toLocalDiskPathString(moduleIdentifier));
        final File localModulesFile;
        try {
            localModulesFile = this.downloadToLocalRepository(relativePath, NAME_MODULES_DESCRIPTOR,
                localRepoModuleRoot);
        } catch (final FileNotFoundException fnfe) {
            throw new RuntimeException("Could not find " + NAME_MODULES_DESCRIPTOR, fnfe);
        }

        // Parse out the name of the resource root and any dependencies
        String resourceRootPath = null;
        final Set<String> dependencies = new HashSet<String>();
        final XMLStreamReader reader;
        try {
            reader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(localModulesFile));
        } catch (final FileNotFoundException fnfe) {
            throw new RuntimeException("Could not find the file we've just written: "
                + localModulesFile.getAbsolutePath());
        } catch (final XMLStreamException xmlse) {
            throw new RuntimeException(xmlse);
        } catch (final FactoryConfigurationError fce) {
            throw new RuntimeException(fce);
        }
        try {
            boolean inDependenciesSection = false;
            readerLoop: while (reader.hasNext()) {
                final int next = reader.next();
                switch (next) {
                    case XMLStreamReader.START_ELEMENT:
                        String elementName = reader.getLocalName();
                        if (ELEMENT_NAME_RESOURCE_ROOT.equals(elementName)) {
                            final int numAttributes = reader.getAttributeCount();
                            for (int i = 0; i < numAttributes; i++) {
                                final String attribute = reader.getAttributeLocalName(i);
                                if (ATTRIBUTE_NAME_PATH.equals(attribute)) {
                                    resourceRootPath = reader.getAttributeValue(i);
                                }
                            }
                            continue;
                        }

                        if (ELEMENT_NAME_DEPENDENCIES.equals(elementName)) {
                            inDependenciesSection = true;
                            continue;
                        }
                        if (ELEMENT_NAME_MODULE.equals(elementName) && inDependenciesSection) {
                            final int numAttributes = reader.getAttributeCount();
                            for (int i = 0; i < numAttributes; i++) {
                                final String attribute = reader.getAttributeLocalName(i);
                                if (ATTRIBUTE_NAME_NAME.equals(attribute)) {
                                    dependencies.add(reader.getAttributeValue(i));
                                }
                            }
                        }
                        continue;
                    case XMLStreamReader.END_ELEMENT:
                        elementName = reader.getLocalName();
                        if (ELEMENT_NAME_DEPENDENCIES.equals(elementName)) {
                            break readerLoop;
                        }

                }
            }
        } catch (final XMLStreamException xmlse) {
            throw new RuntimeException("Encountered error reading from " + localModulesFile.getAbsolutePath(), xmlse);
        }

        if (resourceRootPath == null) {
            // Could be to system path, so we're done here
            return;
        }

        // Get all dependencies recursively
        for (final String dependency : dependencies) {
            final ModuleIdentifier moduleId = ModuleIdentifier.fromString(dependency);
            this.fetchModuleAssets(moduleId);
        }

        // Download the resource root
        try {
            this.downloadToLocalRepository(relativePath, resourceRootPath, localRepoModuleRoot);
        } catch (final FileNotFoundException fnfe) {
            throw new RuntimeException("Specified resource root could not be found: " + resourceRootPath);
        }
        // Download the index file
        final String indexFileName = resourceRootPath + SUFFIX_INDEX;
        try {
            this.downloadToLocalRepository(relativePath, indexFileName, localRepoModuleRoot);
        } catch (final FileNotFoundException fnfe) {
            // Ignore, must be no index
            if (log.isLoggable(Level.FINEST)) {
                log.finest("No index file found: " + indexFileName + "; skipping.");
            }
        }
    }

    /**
     * Performs the download of the remote file name located in the relative remote path to the specified parent
     * directory. All arguments are required.
     *
     * @param relativeRemotePath
     * @param remoteFileName
     * @param parentDir
     * @return
     * @throws FileNotFoundException
     *             If the requested resource could not be found in the remote path with the remote file name
     */
    private File downloadToLocalRepository(final String relativeRemotePath, final String remoteFileName,
        final File parentDir) throws FileNotFoundException {

        assert relativeRemotePath != null;
        assert remoteFileName != null;
        assert parentDir != null;

        final URL relativePathURL;
        final URL fullURL;
        try {
            relativePathURL = new URL(rootUrl, relativeRemotePath);
            fullURL = new URL(relativePathURL, remoteFileName);
        } catch (final MalformedURLException murle) {
            throw new RuntimeException(murle);
        }
        final File targetFile = new File(parentDir, remoteFileName);
        final InputStream in;
        try {
            if (log.isLoggable(Level.INFO)) {
                log.info("Writing: " + fullURL + " as " + targetFile);
            }
            in = fullURL.openStream();
        } catch (final IOException ioe) {
            throw new RuntimeException("Could not get stream to " + fullURL.toExternalForm(), ioe);
        }
        if (in == null) {
            throw new FileNotFoundException("Could not find requested file at URL: " + fullURL.toExternalForm());
        }
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IllegalStateException("Could not create parent directory: " + parentDir);
        }
        OutputStream out = null;
        try {
            try {
                out = new FileOutputStream(targetFile);
            } catch (final FileNotFoundException fnfe) {
                throw new RuntimeException(fnfe);
            }
            final byte[] buffer = new byte[512]; // Relatively smaller buffer as we don't know how far away the
                                                 // source is
            try {
                int readBytes = 0;
                while ((readBytes = in.read(buffer)) != -1) {
                    out.write(buffer, 0, readBytes);
                    if (log.isLoggable(Level.INFO)) {
                        // Only way to show incremental progress without flooding the log with unnecessary lines is to
                        // write straight to console
                        System.out.print('.');
                    }
                }
                if (log.isLoggable(Level.INFO)) {
                    System.out.println();
                }
            } catch (final IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } finally {
            try {
                if (out != null) {
                    out.close();
                    if (log.isLoggable(Level.INFO)) {
                        log.info("Wrote " + targetFile.getAbsolutePath());
                    }
                }
            } catch (final IOException ioe) {
                // Swallow
            }
        }

        // Return
        return targetFile;
    }

    private static String toHttpPathString(final ModuleIdentifier moduleIdentifier) {
        return toPathString(moduleIdentifier, '/');
    }

    private static String toLocalDiskPathString(final ModuleIdentifier moduleIdentifier) {
        return toPathString(moduleIdentifier, File.separatorChar);
    }

    private static String toPathString(final ModuleIdentifier moduleIdentifier, final char replacementChar) {
        final StringBuilder builder = new StringBuilder(40);
        builder.append(moduleIdentifier.getName().replace('.', replacementChar));
        builder.append(replacementChar).append(moduleIdentifier.getSlot());
        builder.append(replacementChar);
        return builder.toString();
    }
}
