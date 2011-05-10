/*
 * Boss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.io.File;

/**
 * Utility class for default module file names.
 * <p/>
 * Date: 10.05.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ModuleXmlUtil {
    public static final String DEFAULT_FILENAME = "module.xml";

    /**
     * Private constructor.
     */
    private ModuleXmlUtil() {
    }

    /**
     * Creates a file based on the directory and the module identifier.
     * <p/>
     * The {@code dir} parameter must be a directory and the {@code identifier} cannot be {@code null}.
     *
     * @param dir        the base directory where the file should be located.
     * @param identifier the module identifier.
     *
     * @return the module XML file.
     * @throws IllegalArgumentException if the {@code dir} parameter is not a directory or an argument is {@code null}.
     */
    public static File toFile(final File dir, final ModuleIdentifier identifier) {
        return toFile(dir, DEFAULT_FILENAME, identifier);
    }

    /**
     * Creates a file based on the directory and the module identifier.
     * <p/>
     * The {@code dir} parameter must be a directory and the {@code identifier} cannot be {@code null}.
     *
     * @param dir        the base directory where the file should be located.
     * @param name       the name of the XML file.
     * @param identifier the module identifier.
     *
     * @return the module XML file.
     * @throws IllegalArgumentException if the {@code dir} parameter is not a directory or an argument is {@code null}.
     */
    public static File toFile(final File dir, final String name, final ModuleIdentifier identifier) {
        if (dir == null || !dir.isDirectory()) {
            throw new IllegalArgumentException(String.format("Must be a directory. File %s is not a directory.", dir));
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("The name cannot be empty.");
        }
        if (identifier == null) {
            throw new IllegalArgumentException("The module identifier cannot be null.");
        }
        return new File(dir, baseFilename(name, identifier));
    }

    /**
     * Creates a path name from the module identifier. The name always ends with the separator character.
     * </p>
     * A {@code null} identifier will result in no separator being used.
     *
     * @param identifier the module identifier.
     * @param separator  the directory separator.
     *
     * @return a path name of the module identifier.
     * @throws IllegalArgumentException if the module identifier is {@code null}.
     */
    public static String baseDirectory(final ModuleIdentifier identifier, final String separator) {
        if (identifier == null) {
            throw new IllegalArgumentException("The module identifier cannot be null.");
        }
        final String namePath = identifier.getName().replace('.', File.separatorChar);
        final StringBuilder baseName = new StringBuilder();
        baseName.append(namePath).
                append((separator == null ? "" : separator)).
                append(identifier.getSlot()).
                append((separator == null ? "" : separator));
        return baseName.toString();
    }

    /**
     * Creates a path name from the module identifier with the default {@link java.io.File#separator} character.
     *
     * @param identifier the module identifier.
     *
     * @return a path name of the module identifier.
     * @throws IllegalArgumentException if the module identifier is {@code null}.
     * @see #baseDirectory(ModuleIdentifier, String)
     */
    public static String baseDirectory(final ModuleIdentifier identifier) {
        return baseDirectory(identifier, File.separator);
    }

    /**
     * Creates a path name to the module XML file from the module identifier. Uses the {@link #DEFAULT_FILENAME} for
     * the XML file name.
     *
     * @param identifier the module identifier.
     *
     * @return a path name to the module XML file.
     * @throws IllegalArgumentException if the module identifier is {@code null}.
     */
    public static String baseFilename(final ModuleIdentifier identifier) {
        return baseFilename(DEFAULT_FILENAME, identifier);
    }

    /**
     * Creates a path name to the module XML file from the module identifier.
     *
     * @param name       the XML file name.
     * @param identifier the module identifier.
     *
     * @return a path name to the module XML file.
     * @throws IllegalArgumentException if the module identifier is {@code null}.
     */
    public static String baseFilename(final String name, final ModuleIdentifier identifier) {
        return baseDirectory(identifier) + name;
    }

    /**
     * Creates a path name to the module XML
     *
     * @param name       the XML file name. file from the module identifier.
     * @param separator  the directory separator.
     * @param identifier the module identifier.
     *
     * @return a path name to the module XML file.
     * @throws IllegalArgumentException if the module identifier is {@code null}.
     */
    public static String baseFilename(final String name, final String separator, final ModuleIdentifier identifier) {
        return baseDirectory(identifier, separator) + name;
    }
}
