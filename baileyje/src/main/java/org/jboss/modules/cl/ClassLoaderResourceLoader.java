package org.jboss.modules.cl;

import org.jboss.modules.spi.Module;
import org.jboss.modules.spi.ResourceLoader;
import org.jboss.modules.spi.ResourceSpec;

import java.net.URL;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class ClassLoaderResourceLoader implements ResourceLoader
{
   private final ClassLoader classLoader;

   public ClassLoaderResourceLoader(final ClassLoader classLoader)
   {
      this.classLoader = classLoader;
   }

   public ResourceSpec findResource(final Module module, final String path)
   {
      URL resourceUrl = classLoader.getResource(path);
      if(resourceUrl == null)
         return null;
      return new DefaultResourceSpec(module, this, resourceUrl);
   }
}
