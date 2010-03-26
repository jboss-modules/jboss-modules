package org.jboss.modules.metadata;

import org.jboss.xb.annotations.JBossXmlSchema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
@JBossXmlSchema(namespace = "urn:jboss:module:1.0", elementFormDefault = XmlNsForm.QUALIFIED)
@XmlRootElement(name="jboss-module")
@XmlType(name="moduleType", propOrder={"exports", "imports"})
public class ModuleMetaData
{
   private List<ImportMetaData> imports;
   private List<ExportMetaData> exports;

   public List<ImportMetaData> getImports()
   {
      return imports;
   }

   @XmlElement(name="import", type = ImportMetaData.class)
   public void setImports(List<ImportMetaData> imports)
   {
      this.imports = imports;
   }


   public List<ExportMetaData> getExports()
   {
      return exports;
   }

   @XmlElement(name="export", type = ExportMetaData.class)
   public void setExports(final List<ExportMetaData> exports)
   {
      this.exports = exports;
   }
}
