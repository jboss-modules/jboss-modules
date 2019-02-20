package org.jboss.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class JavaSeDeps {
    static final List<DependencySpec> list;

    static {
        List<DependencySpec> deps = new ArrayList<>();
        for (String dep : Arrays.asList(
                "java.compiler",
                "java.datatransfer",
                "java.desktop",
                "java.instrument",
                "java.logging",
                "java.management",
                "java.management.rmi",
                "java.naming",
                "java.prefs",
                "java.rmi",
                "java.scripting",
                "java.security.jgss",
                "java.security.sasl",
                "java.sql",
                "java.sql.rowset",
                "java.xml",
                "java.xml.crypto"
        )) {
            deps.add(new ModuleDependencySpecBuilder().setName(dep).setExport(true).build());
        }
        list = deps;
    }
}
