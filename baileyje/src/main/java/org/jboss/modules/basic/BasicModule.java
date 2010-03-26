package org.jboss.modules.basic;

import org.jboss.modules.cl.ClassLoaderUtil;
import org.jboss.modules.cl.ModuleClassLoader;
import org.jboss.modules.spi.ModuleId;
import org.jboss.modules.spi.ResourceLoader;
import org.jboss.modules.spi.ClassSpec;
import org.jboss.modules.spi.Export;
import org.jboss.modules.spi.Import;
import org.jboss.modules.spi.Module;
import org.jboss.modules.spi.ResourceSpec;
import org.jboss.modules.spi.Wire;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class BasicModule implements Module
{
   private final ModuleId moduleId;
   private final List<Import> imports;
   private final List<Export> exports;
   private final List<Wire> wires = new LinkedList<Wire>();
   private final ModuleClassLoader moduleClassLoader;

   public BasicModule(final ModuleId moduleId, final List<Export> exports, final List<Import> imports, final List<ResourceLoader> resourceLoaders)
   {
      this.moduleId = moduleId;
      this.exports = exports;
      this.imports = imports;
      this.moduleClassLoader = new ModuleClassLoader(this, resourceLoaders);
   }

   public ModuleId getModuleId()
   {
      return moduleId;
   }

   public List<Export> getExports()
   {
      return exports;
   }

   public List<Import> getImports()
   {
      return imports;
   }

   public List<Wire> getWires()
   {
      return wires;
   }

   public ClassSpec<?> loadClass(final String name)
   {
      String packageName = ClassLoaderUtil.getPackageName(name);
      if(!exported(packageName))
         return null;
      return moduleClassLoader.loadClassSpec(name);
   }

   public ResourceSpec getResource(final String name)
   {
      String packageName = ClassLoaderUtil.getResourcePackageName(name);
      if(!exported(packageName))
         return null;
      return moduleClassLoader.findResourceSpec(name);
   }

   public List<ResourceSpec> getResources(final String name)
   {
      String packageName = ClassLoaderUtil.getResourcePackageName(name);
      if(!exported(packageName))
         return null;
      return moduleClassLoader.findResourceSpecs(name);
   }

   public boolean isResolved()
   {
      for(Import anImport : imports)
      {
         if(!anImport.isResolved())
            return false;
      }
      return true;
   }

   private boolean exported(String packageName)
   {
      return true;
//      for(Export export : exports)
//      {
//         if(export.getPackageName().equals(packageName))
//            return true;
//      }
//      return false;
   }

   @Override
   public String toString()
   {
      return "BasicModule{" +
         "id='" + getModuleId() + '\'' +
         ", exports=" + getExports() +
         ", imports=" + getImports() +
         ", resolved=" + isResolved() +
         ", wires=" + wires +
         '}';
   }
}
