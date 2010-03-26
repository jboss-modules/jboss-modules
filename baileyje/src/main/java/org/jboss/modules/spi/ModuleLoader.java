package org.jboss.modules.spi;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public interface ModuleLoader
{
   Module loadModule(ModuleId moduleId);
}
