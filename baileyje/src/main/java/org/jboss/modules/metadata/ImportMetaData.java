package org.jboss.modules.metadata;

import org.jboss.modules.spi.Import;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
@XmlType(name = "importType")
public class ImportMetaData
{
   private String moduleGroup;
   private String moduleName;
   private String moduleVersion;
   private String packageName;
   private boolean optional;

   @XmlAttribute(name = "group")
   public String getModuleGroup()
   {
      return moduleGroup;
   }

   public void setModuleGroup(final String moduleGroup)
   {
      this.moduleGroup = moduleGroup;
   }

   @XmlAttribute(name = "module")
   public String getModuleName()
   {
      return moduleName;
   }

   public void setModuleName(final String moduleName)
   {
      this.moduleName = moduleName;
   }

   @XmlAttribute(name = "package", required = false)
   public String getPackageName()
   {
      return packageName;
   }

   public void setPackageName(final String packageName)
   {
      this.packageName = packageName;
   }

   @XmlAttribute(name = "version")
   public String getModuleVersion()
   {
      return moduleVersion;
   }

   public void setModuleVersion(final String version)
   {
      this.moduleVersion = version;
   }

   @XmlAttribute(required = false)
   public boolean isOptional()
   {
      return optional;
   }

   public void setOptional(final boolean optional)
   {
      this.optional = optional;
   }

   @Override
   public String toString()
   {
      return "ImportMetaData{" +
         ", moduleGroup='" + moduleGroup + '\'' +
         ", moduleName='" + moduleName + '\'' +
         ", moduleVersion='" + moduleVersion + '\'' +
         ", packageName='" + packageName + '\'' +
         ", optional='" + optional + '\'' +
         '}';
   }
}
