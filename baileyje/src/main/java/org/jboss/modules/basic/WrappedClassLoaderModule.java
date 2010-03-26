package org.jboss.modules.basic;

import org.jboss.modules.cl.DefaultClassSpec;
import org.jboss.modules.cl.DefaultResourceSpec;
import org.jboss.modules.spi.ClassSpec;
import org.jboss.modules.spi.Export;
import org.jboss.modules.spi.Import;
import org.jboss.modules.spi.ModuleId;
import org.jboss.modules.spi.ResourceSpec;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class WrappedClassLoaderModule extends BasicModule
{
   private final ClassLoader classLoader;

   public WrappedClassLoaderModule(final ModuleId moduleId, final List<Export> exports, final List<Import> imports, ClassLoader classLoader)
   {
      super(moduleId, exports, imports, null);
      this.classLoader = classLoader;
   }

   @Override
   public ResourceSpec getResource(final String name)
   {
      URL resourceUrl = classLoader.getResource(name);
      if(resourceUrl != null)
         return new DefaultResourceSpec(this, null, resourceUrl);
      return null;
   }

   @Override
   public List<ResourceSpec> getResources(final String name)
   {
      List<ResourceSpec> resourceSpecs = new LinkedList<ResourceSpec>();
      try
      {
         Enumeration<URL> resources = classLoader.getResources(name);

         if(resources != null)
            while(resources.hasMoreElements())
            {
               URL resourceUrl = resources.nextElement();
               resourceSpecs.add(new DefaultResourceSpec(this, null, resourceUrl));
            }
      }
      catch(IOException e)
      {
         // Ignored
      }
      return resourceSpecs;
   }

   @Override
   public ClassSpec<?> loadClass(final String name)
   {
      try
      {
         Class<?> loadedClass = classLoader.loadClass(name);
         return new DefaultClassSpec(name, loadedClass, this, null);
      }
      catch(ClassNotFoundException e)
      {
         // Ignored
      }
      return null;
   }
}

