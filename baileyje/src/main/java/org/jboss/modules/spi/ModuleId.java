package org.jboss.modules.spi;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public interface ModuleId
{
   String getGroup();
   String getName();
   String getVersion();
}
