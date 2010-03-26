package org.jboss.modules.spi;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public interface Import
{
   String getModuleGroup();
   String getModuleName();
   String getModuleVersion();
   String getPackageName();
   boolean isResolved();
   boolean isOptional();
}
