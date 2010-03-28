package org.jboss.modules;

import java.io.File;
import java.util.jar.JarFile;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class LocalModuleLoader extends ModuleLoader {

    private final File repoRoot;

    public LocalModuleLoader(final File repoRoot) {
        this.repoRoot = repoRoot;
    }

    @Override
    protected Module findModule(final ModuleIdentifier moduleIdentifier) throws ModuleNotFoundException {
        final File moduleRoot = getModuleRoot(moduleIdentifier);
        if (!moduleRoot.exists())
            throw new ModuleNotFoundException("Module " + moduleIdentifier + " does not exist in repository " + repoRoot);

        final File moduleXml = new File(moduleRoot, "module.xml");

        ModuleSpec moduleSpec;
        try {
            moduleSpec = parseModuleInfoFile(moduleIdentifier, moduleRoot, moduleXml);
        } catch (Exception e) {
            throw new ModuleNotFoundException(moduleIdentifier.toString(), e);
        }

        return defineModule(moduleSpec);
    }

    private File getModuleRoot(final ModuleIdentifier moduleIdentifier) {
        return new File(repoRoot,
                String.format("%s/%s/%s/", moduleIdentifier.getGroup(), moduleIdentifier.getArtifact(), moduleIdentifier.getVersion()));
    }

    private ModuleSpec parseModuleInfoFile(final ModuleIdentifier moduleIdentifier, final File moduleRoot, final File moduleInfoFile) throws Exception {
        // Create module spec
        // Parse the dependencies for DependencySpec
        // Parse resource-root for ResourceLoaders and use createResourceLoader

        return null;
    }

    private ResourceLoader createResourceLoader(final ModuleIdentifier moduleIdentifier, final File moduleRoot, final String rootName, final String resourcePath) throws Exception {

        final File resourceFile = new File(moduleRoot, resourcePath);
        if (!resourceFile.exists())
            throw new ModuleNotFoundException("Failed to create resource loader for " + resourceFile); // TODO throw a better exception

        if(resourceFile.isFile())
            return new JarFileResourceLoader(moduleIdentifier, new JarFile(resourceFile), rootName);
        else
            return new FileResourceLoader(moduleIdentifier, rootName, resourceFile);
    }
}
