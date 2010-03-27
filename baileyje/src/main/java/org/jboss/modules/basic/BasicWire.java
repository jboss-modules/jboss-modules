package org.jboss.modules.basic;

import org.jboss.modules.spi.Export;
import org.jboss.modules.spi.Module;
import org.jboss.modules.spi.Wire;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class BasicWire implements Wire
{
   private final Module exportingModule;
   private final Module importingModule;
   private final Export export;

   public BasicWire(final Module exportingModule, final Module importingModule, final Export export)
   {
      this.exportingModule = exportingModule;
      this.importingModule = importingModule;
      this.export = export;
   }

   public Module getExportingModule()
   {
      return exportingModule;
   }

   public Module getImportingModule()
   {
      return importingModule;
   }

   public boolean supportsPackage(final String packageName)
   {
      return export.satisfies(packageName);
   }

   @Override
   public String toString()
   {
      return String.format("BasicWire{exportingModule='%s', importingModule='%s', package='%s'}",
         exportingModule, importingModule, export.getPackageName());
   }
}
