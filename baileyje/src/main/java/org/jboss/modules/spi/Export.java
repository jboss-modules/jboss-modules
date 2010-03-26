package org.jboss.modules.spi;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public interface Export
{
   String getPackageName();
   boolean satisfies(String packageName);
}
