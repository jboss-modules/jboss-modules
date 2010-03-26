package org.jboss.modules.basic;

import org.jboss.modules.spi.ModuleId;

public class BasicModuleId implements ModuleId
{
   private final String group;
   private final String name;
   private final String version;

   public BasicModuleId(final String group, final String name, final String version)
   {
      this.group = group;
      this.name = name;
      this.version = version;
   }

   public String getGroup()
   {
      return group;
   }

   public String getName()
   {
      return name;
   }

   public String getVersion()
   {
      return version;
   }

   @Override
   public boolean equals(final Object o)
   {
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;

      final BasicModuleId moduleId = (BasicModuleId) o;

      if(group != null ? !group.equals(moduleId.group) : moduleId.group != null) return false;
      if(name != null ? !name.equals(moduleId.name) : moduleId.name != null) return false;
      if(version != null ? !version.equals(moduleId.version) : moduleId.version != null) return false;

      return true;
   }

   @Override
   public int hashCode()
   {
      int result = group != null ? group.hashCode() : 0;
      result = 31 * result + (name != null ? name.hashCode() : 0);
      result = 31 * result + (version != null ? version.hashCode() : 0);
      return result;
   }

   @Override
   public String toString()
   {
      return "BasicModuleId{" + "group='" + group + '\'' + ", name='" + name + '\'' + ", version='" + version + '\'' + '}';
   }
}