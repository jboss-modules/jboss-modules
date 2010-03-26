package org.jboss.modules.spi;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public interface ClassSpec<T>
{
   String getName();
   Module getModule();
   Class<T> getType();
}
