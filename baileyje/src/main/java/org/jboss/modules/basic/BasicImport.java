package org.jboss.modules.basic;

import org.jboss.modules.spi.Import;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class BasicImport implements Import
{
   private final String groupGroup;
   private final String moduleName;
   private final String moduleVersion;
   private final String packageName;
   private final boolean optional;
   private boolean resolved;

   public BasicImport(final String groupGroup, final String moduleName, final String moduleVersion, final String packageName, final boolean optional)
   {
      this.groupGroup = groupGroup;
      this.moduleName = moduleName;
      this.moduleVersion = moduleVersion;
      this.packageName = packageName;
      this.optional = optional;
   }

   public String getModuleGroup()
   {
      return groupGroup;
   }

   public String getModuleName()
   {
      return moduleName;
   }

   public String getModuleVersion()
   {
      return moduleVersion;
   }

   public String getPackageName()
   {
      return packageName;
   }

   public boolean isResolved()
   {
      return resolved;
   }

   public void setResolved(final boolean resolved)
   {
      this.resolved = resolved;
   }

   public boolean isOptional()
   {
      return optional;
   }

   @Override
   public String toString()
   {
      return "BasicImport{" +
         "moduleGroup='" + groupGroup + '\'' +
         ", moduleName='" + moduleName + '\'' +
         ", moduleVersion='" + moduleVersion + '\'' +
         ", packageName='" + packageName + '\'' +
         ", optional=" + optional +
         ", resolved=" + resolved +
         '}';
   }
}
