package org.jboss.modules;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
* @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
*/
final class Paths<T> {
    private final Map<String, List<T>> allPaths;
    private final Map<String, List<T>> exportedPaths;

    Paths(final Map<String, List<T>> allPaths, final Map<String, List<T>> exportedPaths) {
        this.allPaths = allPaths;
        this.exportedPaths = exportedPaths;
    }

    Map<String, List<T>> getAllPaths() {
        return allPaths;
    }

    Map<String, List<T>> getExportedPaths() {
        return exportedPaths;
    }

    @SuppressWarnings({ "unchecked" })
    static final Paths NONE = new Paths(Collections.<String, List<Object>>emptyMap(), Collections.<String, List<Object>>emptyMap());

    @SuppressWarnings({ "unchecked" })
    static <T> Paths<T> none() {
        return (Paths<T>) NONE;
    }
}
