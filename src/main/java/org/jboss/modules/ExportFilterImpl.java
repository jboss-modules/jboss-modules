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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of ExportFilter.  Uses glob based includes and excludes to determine whether to export.  
 *
 * @author John E. Bailey
 */
public class ExportFilterImpl implements ExportFilter {
    private static final Pattern GLOB_PATTERN = Pattern.compile("(\\*\\*?)|(\\?)|(\\\\.)|(/+)|([^*?]+)");
    private static final Pattern META_INF_PATTERN = getGlobPattern("META-INF");
    private static final Pattern META_INF_CHILD_PATTERN = getGlobPattern("META-INF/**");

    private final Pattern[] includes;
    private final Pattern[] excludes;

    public ExportFilterImpl(final String[] includes, final String[] excludes) {
        this.includes = new Pattern[includes.length];
        int i = 0;
        for(String includeGlob : includes) {
            this.includes[i++] = getGlobPattern(includeGlob);
        }
        this.excludes = new Pattern[excludes.length + 2];
        i = 0;
        for(String excludeGlob : excludes) {
            this.excludes[i++] = getGlobPattern(excludeGlob);
        }
        this.excludes[i++] = META_INF_PATTERN;
        this.excludes[i++] = META_INF_CHILD_PATTERN;
    }

    /**
     * Determine whether a path should be exported.
     *
     * @param path the path to check
     * @return true if the path should be exported, false if not
     */
    public boolean shouldExport(final String path) {
        for(Pattern includePattern : includes) {
            if(matches(includePattern, path)) {
                return true;
            }
        }
        for(Pattern excludePatterns : excludes) {
            if(matches(excludePatterns, path)) {
                return false;
            }
        }
        return true;
    }

    private boolean matches(final Pattern pattern, final String path) {
        final Matcher matcher = pattern.matcher(path);
        return matcher.matches();
    }

    /**
     * Get a regular expression pattern which matches any path names which match the given glob.  The glob patterns
     * function similarly to {@code ant} file patterns.  Valid metacharacters in the glob pattern include:
     * <ul>
     * <li><code>"\"</code> - escape the next character (treat it literally, even if it is itself a recognized metacharacter)</li>
     * <li><code>"?"</code> - match any non-slash character</li>
     * <li><code>"*"</code> - match zero or more non-slash characters</li>
     * <li><code>"**"</code> - match zero or more characters, including slashes</li>
     * <li><code>"/"</code> - match one or more slash characters.  Consecutive {@code /} characters are collapsed down into one.</li>
     * </ul>
     * In addition, like {@code ant}, if the pattern ends with a {@code /}, then an implicit <code>"**"</code> will be appended.
     * <p/>
     * <b>See also:</b> <a href="http://ant.apache.org/manual/dirtasks.html#patterns">"Patterns" in the Ant Manual</a>
     *
     * @param glob the glob to match
     *
     * @return the pattern
     */
    private static Pattern getGlobPattern(final String glob) {
        StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append("^");
        final Matcher m = GLOB_PATTERN.matcher(glob);
        boolean lastWasSlash = false;
        while (m.find()) {
            lastWasSlash = false;
            String grp;
            if ((grp = m.group(1)) != null) {
                // match a * or **
                if (grp.length() == 2) {
                    // it's a **
                    patternBuilder.append(".*");
                } else {
                    // it's a *
                    patternBuilder.append("[^/]*");
                }
            } else if ((grp = m.group(2)) != null) {
                // match a '?' glob pattern; any non-slash character
                patternBuilder.append("[^/]");
            } else if ((grp = m.group(3)) != null) {
                // backslash-escaped value
                patternBuilder.append(grp.charAt(1));
            } else if ((grp = m.group(4)) != null) {
                // match any number of / chars
                patternBuilder.append("/+");
                lastWasSlash = true;
            } else {
                // some other string
                patternBuilder.append(Pattern.quote(m.group()));
            }
        }
        if (lastWasSlash) {
            // ends in /, append **
            patternBuilder.append(".*");
        }
        patternBuilder.append("$");
        return Pattern.compile(patternBuilder.toString());
    }
}
