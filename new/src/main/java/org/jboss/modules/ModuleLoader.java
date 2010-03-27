package org.jboss.modules;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.HashMap;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public abstract class ModuleLoader
{
   private final HashMap<ModuleIdentifier, ModuleRef> moduleMap = new HashMap<ModuleIdentifier, ModuleRef>();

   public ModuleSpec loadModule(ModuleIdentifier identifier) throws ModuleNotFoundException
   {
      final ModuleSpec module = findModule(identifier);
      if(module == null)
      {
         throw new ModuleNotFoundException(identifier.toString());
      }
      return module;
   }

   private static final class ModuleRef extends SoftReference<Module>
   {
      private final URI key;

      private ModuleRef(final URI key, final Module value, final ReferenceQueue<? super Module> queue)
      {
         super(value, queue);
         this.key = key;
      }

      public URI getKey()
      {
         return key;
      }
   }

   protected abstract ModuleSpec findModule(final ModuleIdentifier moduleIdentifier);

   protected final ModuleSpec defineModule(final ModuleIdentifier moduleIdentifier, final ModuleContentLoader loader, final ModuleIdentifier[] imports, final ModuleIdentifier[] exports, final Module.Flag... flags) throws ModuleNotFoundException
   {
      synchronized(moduleMap)
      {
         final SoftReference<Module> ref = moduleMap.get(moduleIdentifier);
         if(ref != null)
         {
            final Module oldModule = ref.get();
            if(oldModule != null)
            {
               throw new RuntimeException("Module already exists:" + moduleIdentifier); // Make custom
            }
         }
         // Add logic to resolve modules

         // Add the module to the map
         //final ModuleSpec module = new ModuleSpec(loader, importModules, exportModules, moduleIdentifier, flags);
         //moduleMap.put(moduleIdentifier, new ModuleRef(moduleIdentifier, module, refQueue));
         return null;
      }
   }
}
