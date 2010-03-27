package org.jboss.modules.basic;

import org.jboss.modules.spi.Export;
import org.jboss.modules.spi.Import;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class BasicExport implements Export
{
   private final String packageName;

   public BasicExport(final String packageName)
   {
      this.packageName = packageName;
   }

   public String getPackageName()
   {
      return packageName;
   }

   public boolean satisfies(final String importedPackageName)
   {
      if(packageName.endsWith(".*"))
         return importedPackageName.startsWith(packageName.substring(0, packageName.length() - 3));
      else
         return packageName.equals(importedPackageName);
   }

   @Override
   public String toString()
   {
      return String.format("BasicExport{packageName='%s'}",packageName);
   }
}
