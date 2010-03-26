package org.jboss.modules.spi;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public interface Wire
{
   Module getExportingModule();
   Module getImportingModule();
   boolean supportsPackage(String packageName);
}
