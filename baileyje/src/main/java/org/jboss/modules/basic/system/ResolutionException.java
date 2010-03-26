package org.jboss.modules.basic.system;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class ResolutionException extends RuntimeException
{
   public ResolutionException(final String message, final Throwable cause)
   {
      super(message, cause);
   }

   public ResolutionException(final String message)
   {
      super(message);
   }
}
