package org.jboss.modules.spi;

import org.jboss.modules.spi.ResourceSpec;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public interface ResourceLoader
{
   ResourceSpec findResource(Module module, String path);
}
