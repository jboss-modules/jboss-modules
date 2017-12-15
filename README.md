This is the JBoss Modules project.

JBoss Modules is a standalone implementation of a modular (non-hierarchical) class loading and execution environment for Java. In other words, rather than a single class loader which loads all JARs into a flat class path, each library becomes a module which only links against the exact modules it depends on, and nothing more. It implements a thread-safe, fast, and highly concurrent delegating class loader model, coupled to an extensible module resolution system, which combine to form a unique, simple and powerful system for application execution and distribution.

JBoss Modules is designed to work with any existing library or application without changes, and its simple naming and resolution strategy is what makes that possible. Unlike OSGi, JBoss Modules does not implement a container; rather, it is a thin bootstrap wrapper for executing an application in a modular environment. The moment your application takes control, the modular environment is ready to load and link modules as needed. Furthermore, modules are never loaded (not even for resolution purposes) until required by a dependency, meaning that the performance of a modular application depends only on the number of modules actually used (and when they are used), rather than the total number of modules in the system. And, they may be unloaded by the user at any time.

## Documentation

All documentation lives at https://jboss-modules.github.io/jboss-modules/manual/

## Issue tracker

All issues can be reported at https://issues.jboss.org/browse/MODULES

## Code

All code can be found at https://github.com/jboss-modules/jboss-modules

## License

All code distributed under [ASL 2.0](LICENSE.txt) and [XPP3](XPP3-LICENSE.txt).
