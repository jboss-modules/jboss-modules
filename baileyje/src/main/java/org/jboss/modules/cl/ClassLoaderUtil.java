package org.jboss.modules.cl;

import org.jboss.modules.spi.Module;
import org.jboss.modules.spi.ResourceLoader;
import org.jboss.modules.spi.ResourceSpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class ClassLoaderUtil
{
   public static String getPackageName(final String className)
   {
      int idx = className.lastIndexOf(".");
      if(idx == -1)
         return "";
      return className.substring(0, idx);
   }

   public static String getResourcePackageName(final String name)
   {
      return getPackageName(name.replace('/', '.'));
   }

   public static String getClassResourcePath(final String className)
   {
      return className.replace('.', '/') + ".class";
   }

   public static byte[] readBytes(final ResourceSpec resourceSpec)
   {
      InputStream classInput = null;
      try
      {
         try
         {
            classInput = resourceSpec.openStream();
            if(classInput != null)
            {
               return readBytes(classInput);
            }
         }
         catch(IOException e)
         {
            throw new RuntimeException("Failed to read class bytes from " + resourceSpec);
         }

      }
      finally
      {
         if(classInput != null)
         {
            try
            {
               classInput.close();
            }
            catch(IOException ignored)
            {
            }
         }
      }
      return null;
   }

   public static byte[] readBytes(final InputStream is) throws IOException
   {
      if(is == null)
         throw new IllegalArgumentException("Null input stream.");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] tmp = new byte[1024];
      int read;
      while((read = is.read(tmp)) >= 0)
      {
         baos.write(tmp, 0, read);
      }
      return baos.toByteArray();
   }

   public static class ResourceSpecUrlEnumeration implements Enumeration<URL>
   {
      private final Enumeration<ResourceSpec> resourceEnumeration;

      public ResourceSpecUrlEnumeration(final List<ResourceSpec> resourceSpecs)
      {
         this.resourceEnumeration = Collections.enumeration(resourceSpecs);
      }

      public boolean hasMoreElements()
      {
         return resourceEnumeration.hasMoreElements();
      }

      public URL nextElement()
      {
         return resourceEnumeration.nextElement().getUrl();
      }
   }
}
