package org.jboss.modules.cl;

import org.jboss.modules.spi.Module;
import org.jboss.modules.spi.ResourceLoader;
import org.jboss.modules.spi.ResourceSpec;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class DefaultResourceSpec implements ResourceSpec
{
   private final Module module;
   private final ResourceLoader loader;
   private final URL resourceUrl;

   public DefaultResourceSpec(final Module module, final ResourceLoader loader, final URL resourceUrl)
   {
      this.module = module;
      this.loader = loader;
      this.resourceUrl = resourceUrl;
   }

   public URL getUrl()
   {
      return resourceUrl;
   }

   public InputStream openStream() throws IOException
   {
      return resourceUrl.openStream();
   }

   public ResourceLoader getLoader()
   {
      return loader;
   }

   public Module getModule()
   {
      return module;
   }
}
