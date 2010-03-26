package org.jboss.modules.cl;

import org.jboss.modules.spi.ClassSpec;
import org.jboss.modules.spi.Module;
import org.jboss.modules.spi.ResourceLoader;

/**
 * DefaultClassSpec -
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class DefaultClassSpec implements ClassSpec
{
   private final String className;
   private final Module owningModule;
   private final ResourceLoader loader;
   private final Class<?> type;

   public DefaultClassSpec(final String className, final Class<?> type, final Module owningModule, final ResourceLoader resourceLoader)
   {
      this.className = className;
      this.type = type;
      this.owningModule = owningModule;
      this.loader = resourceLoader;
   }

   public String getName()
   {
      return className;
   }

   public Module getModule()
   {
      return owningModule;
   }

   public ResourceLoader getLoader()
   {
      return loader;
   }

   public Class<?> getType()
   {
      return type;
   }
}
