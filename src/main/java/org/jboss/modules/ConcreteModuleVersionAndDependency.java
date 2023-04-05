package org.jboss.modules;

public class ConcreteModuleVersionAndDependency {
    //Move method/field refactoring
    private final String mainClass;
    private final DependencySpec[] dependencies;
    private final Version version;

    ConcreteModuleVersionAndDependency(final String mainClass, final DependencySpec[] dependencies, final Version version)
    {
        this.mainClass = mainClass;
        this.dependencies = dependencies;
        this.version = version;
    }

    public String getMainClass() {
        return mainClass;
    }

    DependencySpec[] getDependenciesInternal() {
        return dependencies;
    }

    public DependencySpec[] getDependencies() {
        return dependencies.length == 0 ? dependencies : dependencies.clone();
    }

    public Version getVersion() {
        return version;
    }
}
