package org.jboss.modules.metadata;

import org.jboss.modules.spi.Export;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
@XmlType(name="exportType")
public class ExportMetaData
{
   private String packageName;

   public String getPackageName()
   {
      return packageName;
   }

   @XmlAttribute(name="package")
   public void setPackageName(final String packageName)
   {
      this.packageName = packageName;
   }

   @Override
   public String toString()
   {
      return "ExportMetaData{" +
         "packageName='" + packageName + '\'' +
         '}';
   }
}
