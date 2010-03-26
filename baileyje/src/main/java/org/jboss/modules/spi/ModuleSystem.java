package org.jboss.modules.spi;

import org.jboss.modules.cl.ModuleClassLoader;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public interface ModuleSystem
{
   Module getModule(final String group, final String name, final String version);
   void addModule(Module module);
   void addModuleLoader(ModuleLoader loader);
}
