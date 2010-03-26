package org.jboss.modules.basic.system;

import org.jboss.logging.Logger;
import org.jboss.modules.basic.BasicExport;
import org.jboss.modules.basic.BasicImport;
import org.jboss.modules.basic.BasicModuleId;
import org.jboss.modules.basic.BasicWire;
import org.jboss.modules.basic.WrappedClassLoaderModule;
import org.jboss.modules.spi.Export;
import org.jboss.modules.spi.Import;
import org.jboss.modules.spi.Module;
import org.jboss.modules.spi.ModuleId;
import org.jboss.modules.spi.ModuleLoader;
import org.jboss.modules.spi.ModuleSystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class BasicModuleSystem implements ModuleSystem
{
   private static final Logger log = Logger.getLogger("org.jboss.modules");

   // This could be expanded to be a list of all packages that should be available to all bundles
   static final Import SYSTEM_MODULE_IMPORT = new BasicImport("system", "system-module", "0.0", "java", false);

   private final Map<ModuleId, Module> moduleMap = new HashMap<ModuleId, Module>();

   private final List<ModuleLoader> moduleLoaders = new LinkedList<ModuleLoader>();

   private final ReentrantReadWriteLock moduleMapLock = new ReentrantReadWriteLock();

   public BasicModuleSystem()
   {
      installSystemModule();
   }

   public Module getModule(final String group, final String name, final String version)
   {
      final BasicModuleId moduleId = new BasicModuleId(group, name, version);
      Module module = getModule(moduleId);
      if(module == null)
         module = loadAndResolve(moduleId, new LinkedList<Module>());
      return module;
   }

   public void addModule(final Module module)
   {
      moduleMapLock.writeLock().lock();
      try
      {
         final ModuleId moduleId = module.getModuleId();
         moduleMap.put(moduleId, module);
      }
      finally
      {
         moduleMapLock.writeLock().unlock();
      }
      resolve(module, new LinkedList<Module>());
   }

   public void addModuleLoader(final ModuleLoader moduleLoader)
   {
      moduleLoaders.add(moduleLoader);
   }

   private Module getModule(ModuleId moduleId)
   {
      moduleMapLock.readLock().lock();
      try
      {
         if(moduleMap.containsKey(moduleId))
         {
            return moduleMap.get(moduleId);
         }
      }
      finally
      {
         moduleMapLock.readLock().unlock();
      }
      return null;
   }

   private Module loadAndResolve(ModuleId moduleId, List<Module> visited)
   {
      Module module = null;
      moduleMapLock.writeLock().lock();
      try
      {
         // Just in case we lost the lock battle.
         if(moduleMap.containsKey(moduleId))
            return moduleMap.get(moduleId);
         for(ModuleLoader moduleLoader : moduleLoaders)
         {
            module = moduleLoader.loadModule(moduleId);
            if(module != null) break;
         }
         if(module != null)
         {
            moduleMap.put(moduleId, module);
            visited.add(module);
         }
      }
      finally
      {
         moduleMapLock.writeLock().unlock();
      }
      if(module != null)
         resolve(module, visited);
      return module;
   }

   private void resolve(final Module module, List<Module> visited)
   {
      log.debugf("Attempting to resolve module [%s]", module);
      if(!module.isResolved())
         visited.add(module);
      for(Import declaredImport : module.getImports())
      {
         final String packageName = declaredImport.getPackageName();

         ModuleId moduleKey = new BasicModuleId(declaredImport.getModuleGroup(), declaredImport.getModuleName(), declaredImport.getModuleVersion());
         Module importedModule = moduleMap.get(moduleKey);
         if(importedModule == null)
            importedModule = loadAndResolve(moduleKey, visited);

         if(importedModule == null && !declaredImport.isOptional())
            throw new ResolutionException("Failed to resolve import [" + declaredImport + "] for module [" + module + "]");
         if(importedModule != null)
         {
            // If we are just importing an module and it can be found we resolve the import
            if(packageName == null)
               ((BasicImport) declaredImport).setResolved(true);
            final List<Export> moduleExports = importedModule.getExports();
            for(Export export : moduleExports)
            {
               if(packageName == null || export.satisfies(packageName))
               {
                  module.getWires().add(new BasicWire(importedModule, module, export));
                  // If we just satisfied a package specific import, then we resolve
                  if(packageName != null)
                     ((BasicImport) declaredImport).setResolved(true);
               }
            }
         }
      }
      if(!module.isResolved())
         throw new ResolutionException("Failed to resolve module [" + module + "]");
   }

   private void installSystemModule()
   {
      final ModuleId systemModuleId = new BasicModuleId(SYSTEM_MODULE_IMPORT.getModuleGroup(),
         SYSTEM_MODULE_IMPORT.getModuleName(), SYSTEM_MODULE_IMPORT.getModuleVersion());

      final Module systemModule = new WrappedClassLoaderModule(systemModuleId, Collections.<Export>singletonList(new BasicExport("java.*")),
         Collections.<Import>emptyList(), BasicModuleSystem.class.getClassLoader());

      addModule(systemModule);
   }
}
