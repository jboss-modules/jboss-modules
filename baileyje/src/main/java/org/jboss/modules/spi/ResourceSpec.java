package org.jboss.modules.spi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public interface ResourceSpec
{
   URL getUrl();
   InputStream openStream() throws IOException;
}
