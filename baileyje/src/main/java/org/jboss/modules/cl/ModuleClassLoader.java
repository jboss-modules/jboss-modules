package org.jboss.modules.cl;

import org.jboss.logging.Logger;
import org.jboss.modules.cl.ClassLoaderUtil.*;
import org.jboss.modules.spi.ClassSpec;
import org.jboss.modules.spi.Module;
import org.jboss.modules.spi.ResourceLoader;
import org.jboss.modules.spi.ResourceSpec;
import org.jboss.modules.spi.Wire;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.jboss.modules.cl.ClassLoaderUtil.*;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public final class ModuleClassLoader extends ClassLoader
{
   private static final Logger log = Logger.getLogger("org.jboss.modules");

   private final Module module;
   private final List<ResourceLoader> resourceLoaders;
   private final Map<String, ClassSpec<?>> loadedClasses = new HashMap<String, ClassSpec<?>>();


   public ModuleClassLoader(final Module module, final List<ResourceLoader> resourceLoaders)
   {
      super(null);
      this.module = module;
      this.resourceLoaders = resourceLoaders;
   }

   @Override
   protected Class<?> loadClass(final String className, final boolean resolve) throws ClassNotFoundException
   {
      final ClassSpec<?> classSpec = loadClassSpec(className);
      if(classSpec == null)
         throw new ClassNotFoundException(className);

      if(resolve) resolveClass(classSpec.getType());
      return classSpec.getType();
   }

   public ClassSpec<?> loadClassSpec(String className)
   {
      if(Thread.holdsLock(this) && Thread.currentThread() != LoaderThreadHolder.LOADER_THREAD)
      {
         // Only the classloader thread may take this lock; use a condition to relinquish it
         final LoadRequest req = new LoadRequest(className, this);
         final Queue<LoadRequest> queue = LoaderThreadHolder.REQUEST_QUEUE;
         synchronized(LoaderThreadHolder.REQUEST_QUEUE)
         {
            queue.add(req);
            queue.notify();
         }
         boolean intr = false;
         try
         {
            while(!req.done) try
            {
               wait();
            } catch(InterruptedException e)
            {
               intr = true;
            }
         } finally
         {
            if(intr) Thread.currentThread().interrupt();
         }

         return req.result;
      }
      else
      {
         // no deadlock risk!  Either the lock isn't held, or we're inside the class loader thread.
         return findClassSpec(className);
      }
   }

   protected ClassSpec<?> findClassSpec(String className)
   {
      // See if we have loaded this already
      ClassSpec<?> loadedClassSpec = loadedClasses.get(className);

      if(loadedClassSpec == null)
         loadedClassSpec = findClassInImports(className);

      if(loadedClassSpec == null)
         loadedClassSpec = findClassLocal(className);

      return loadedClassSpec;
   }

   protected ClassSpec<?> findClassInImports(String className)
   {
      String packageName = getPackageName(className);
      for(Wire wire : getWiresForPackage(packageName))
      {
         final Module importedModule = wire.getExportingModule();
         // Try to load from the module
         log.debugf("Trying to get class [%s] from module [%s]", className, importedModule);
         ClassSpec<?> importedClass = importedModule.loadClass(className);
         if(importedClass != null)
            return importedClass;
      }
      return null;
   }

   private ClassSpec<?> findClassLocal(final String className)
   {
      // Maybe we got beat to the punch by another caller
      ClassSpec<?> classSpec = loadedClasses.get(className);
      if(classSpec != null)
         return classSpec;

      // Ok.  Time to check our own loaders
      final String classResourcePath = getClassResourcePath(className);
      for(ResourceLoader resourceLoader : resourceLoaders)
      {
         ResourceSpec resourceSpec = resourceLoader.findResource(module, classResourcePath);
         if(resourceSpec == null)
            continue;

         byte[] byteCode = readBytes(resourceSpec);
         if(byteCode != null)
         {
            Class<?> loadedClass = defineClass(className, byteCode, 0, byteCode.length);
            classSpec = new DefaultClassSpec(className, loadedClass, module, resourceLoader);
            break;
         }
      }
      if(classSpec != null)
         loadedClasses.put(className, classSpec);
      return classSpec;
   }

   @Override
   protected URL findResource(final String resourceName)
   {
      ResourceSpec resourceSpec = findResourceSpec(resourceName);
      if(resourceSpec != null)
         return resourceSpec.getUrl();
      return null;
   }

   public ResourceSpec findResourceSpec(final String resourceName)
   {
      ResourceSpec importedResource = findResourceInImports(resourceName);
      if(importedResource != null)
         return importedResource;

      // Now see if we have it
      return findResourceLocal(resourceName);
   }

   protected ResourceSpec findResourceInImports(final String resourceName)
   {
      String packageName = getResourcePackageName(resourceName);
      for(Wire wire : getWiresForPackage(packageName))
      {
         final Module importedModule = wire.getExportingModule();
         // Try to load from the module
         log.debugf("Trying to get resource [%s] from module [%s]", resourceName, importedModule);
         ResourceSpec importedResource = importedModule.getResource(resourceName);
         if(importedResource != null)
            return importedResource;
      }
      return null;
   }

   private ResourceSpec findResourceLocal(final String resourceName)
   {
      for(ResourceLoader resourceLoader : resourceLoaders)
      {
         final ResourceSpec resourceSpec = resourceLoader.findResource(module, resourceName);
         if(resourceSpec != null)
            return resourceSpec;
      }
      return null;
   }

   @Override
   protected Enumeration<URL> findResources(final String resourceName) throws IOException
   {
      List<ResourceSpec> resourceSpecs = findResourceSpecs(resourceName);
      if(resourceSpecs == null)
         return null;

      return new ResourceSpecUrlEnumeration(resourceSpecs);
   }

   public List<ResourceSpec> findResourceSpecs(final String resourceName)
   {
      final List<ResourceSpec> resources = new LinkedList<ResourceSpec>();

      final List<ResourceSpec> importedResources = findResourcesInImports(resourceName);
      if(importedResources != null)
         resources.addAll(importedResources);

      final List<ResourceSpec> localResources = findResourcesLocal(resourceName);
      if(localResources != null)
         resources.addAll(localResources);

      return resources;
   }

   protected List<ResourceSpec> findResourcesInImports(final String resourceName)
   {
      final List<ResourceSpec> resources = new LinkedList<ResourceSpec>();
      String packageName = getResourcePackageName(resourceName);

      for(Wire wire : getWiresForPackage(packageName))
      {
         final Module importedModule = wire.getExportingModule();
         // Try to load from the module
         log.debugf("Trying to get resources [%s] from module [%s]", resourceName, importedModule);
         List<ResourceSpec> importedResources = importedModule.getResources(resourceName);
         if(importedResources != null)
            resources.addAll(importedResources);
      }
      return resources;
   }

   protected List<ResourceSpec> findResourcesLocal(final String resourceName)
   {
      final List<ResourceSpec> resourceSpecs = new LinkedList<ResourceSpec>();
      for(ResourceLoader resourceLoader : resourceLoaders)
      {
         final ResourceSpec resourceSpec = resourceLoader.findResource(module, resourceName);
         if(resourceSpec != null)
            resourceSpecs.add(resourceSpec);
      }
      return resourceSpecs;
   }

   private List<Wire> getWiresForPackage(final String packageName)
   {
      final List<Wire> wires = new LinkedList<Wire>();
      for(Wire wire : module.getWires())
      {
         if(wire.supportsPackage(packageName))
         {
            wires.add(wire);
         }
      }
      return wires;
   }

   @Override
   public String toString()
   {
      return "ModuleClassLoader{" + "module=" + module + '}';
   }


   static class LoadRequest
   {
      private final String className;
      private final ModuleClassLoader requester;
      private ClassSpec<?> result;
      private boolean done;

      public LoadRequest(final String className, final ModuleClassLoader requester)
      {
         this.className = className;
         this.requester = requester;
      }
   }

   static class LoaderThread extends Thread
   {
      @Override
      public void run()
      {
         final Queue<LoadRequest> queue = LoaderThreadHolder.REQUEST_QUEUE;
         for(; ;)
         {
            try
            {
               LoadRequest request;
               synchronized(queue)
               {
                  while((request = queue.poll()) == null)
                  {
                     try
                     {
                        queue.wait();

                     } catch(InterruptedException e)
                     {
                     }
                  }
               }

               final ModuleClassLoader loader = request.requester;

               ClassSpec<?> result = null;

               synchronized(loader)
               {
                  try
                  {
                     result = loader.findClassSpec(request.className);
                  }
                  finally
                  {
                     // no matter what, the requester MUST be notified
                     // todo - separate exception type if an error occurs?
                     request.result = result;
                     request.done = true;
                     loader.notifyAll();
                  }
               }
            } catch(Throwable t)
            {
               // ignore
            }
         }
      }
   }
}