package org.jboss.modules.spi;

import java.util.List;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public interface Module
{
   ModuleId getModuleId();
   List<Export> getExports();
   List<Import> getImports();
   List<Wire> getWires();
   boolean isResolved();
   ClassSpec<?> loadClass(String name);
   ResourceSpec getResource(String name);
   List<ResourceSpec> getResources(String name); 
}
