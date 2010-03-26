package org.jboss.modules.basic.system;

import org.jboss.logging.Logger;
import org.jboss.modules.basic.BasicExport;
import org.jboss.modules.basic.BasicImport;
import org.jboss.modules.basic.BasicModule;
import org.jboss.modules.cl.ClassLoaderResourceLoader;
import org.jboss.modules.spi.ModuleId;
import org.jboss.modules.spi.ResourceLoader;
import org.jboss.modules.metadata.ExportMetaData;
import org.jboss.modules.metadata.ImportMetaData;
import org.jboss.modules.metadata.ModuleMetaData;
import org.jboss.modules.spi.Export;
import org.jboss.modules.spi.Import;
import org.jboss.modules.spi.Module;
import org.jboss.modules.spi.ModuleLoader;
import org.jboss.xb.binding.Unmarshaller;
import org.jboss.xb.binding.UnmarshallerFactory;
import org.jboss.xb.binding.sunday.unmarshalling.DefaultSchemaResolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class RepositoryModuleLoader implements ModuleLoader
{
   private static final Logger log = Logger.getLogger("org.jboss.modules");

   private final Unmarshaller unmarshaller = UnmarshallerFactory.newInstance().newUnmarshaller();

   private final File repositoryBase;

   public RepositoryModuleLoader(final File repositoryBase)
   {
      this.repositoryBase = repositoryBase;
   }

   public Module loadModule(final ModuleId moduleId)
   {
      final String moduleBase = getModuleFileBase(moduleId);
      final File moduleMetaDataFile = new File(repositoryBase, moduleBase + ".xml");
      final File moduleFile = new File(repositoryBase, moduleBase + ".jar");

      BasicModule module = null;
      if(moduleFile.exists())
      {
         try
         {
            final List<Import> imports = new LinkedList<Import>();
            final List<Export> exports = new LinkedList<Export>();

            // Always prime with the system module
            imports.add(BasicModuleSystem.SYSTEM_MODULE_IMPORT);

            if(moduleMetaDataFile.exists())
            {
               log.debugf("Parsing module metadata file [%s] for module []", moduleMetaDataFile, moduleId);
               ModuleMetaData moduleMetaData = parseFile(moduleMetaDataFile);

               if(moduleMetaData.getImports() != null)
                  imports.addAll(getImports(moduleMetaData.getImports()));
               if(moduleMetaData.getExports() != null)
                  exports.addAll(getExports(moduleMetaData.getExports()));
            }

            final URLClassLoader classLoader = new URLClassLoader(new URL[]{moduleFile.toURI().toURL()});
            final ResourceLoader loader = new ClassLoaderResourceLoader(classLoader);
            module = new BasicModule(moduleId, exports, imports, Collections.singletonList(loader));

            log.debugf("Loaded Module [%s]", module);
         }
         catch(Exception e)
         {
            throw new RuntimeException("Failed to load module [" + moduleId + "]", e);
         }
      }
      return module;
   }

   private List<Export> getExports(List<ExportMetaData> declaredExports)
   {
      final List<Export> exports = new LinkedList<Export>();
      for(ExportMetaData declaredExport : declaredExports)
      {
         exports.add(new BasicExport(declaredExport.getPackageName()));
      }
      return exports;
   }

   private List<Import> getImports(List<ImportMetaData> declaredImports)
   {
      final List<Import> imports = new LinkedList<Import>();
      for(ImportMetaData declaredImport : declaredImports)
      {
         imports.add(new BasicImport(declaredImport.getModuleGroup(), declaredImport.getModuleName(), declaredImport.getModuleVersion(), declaredImport.getPackageName(), declaredImport.isOptional()));
      }
      return imports;
   }

   private ModuleMetaData parseFile(final File file) throws Exception
   {
      unmarshaller.setSchemaValidation(false);
      unmarshaller.setValidation(false);
      ModuleMetaData moduleMetaData;
      Reader reader = new BufferedReader(new FileReader(file));
      try
      {
         DefaultSchemaResolver resolver = new DefaultSchemaResolver();
         resolver.addClassBinding("urn:jboss:module:1.0", ModuleMetaData.class);

         moduleMetaData = (ModuleMetaData) unmarshaller.unmarshal(reader, resolver);
      }
      finally
      {
         reader.close();
      }
      return moduleMetaData;
   }

   private String getModuleFileBase(final ModuleId moduleId)
   {
      return moduleId.getGroup() + "-" + moduleId.getName() + "-" + moduleId.getVersion();
   }
}
