package org.jboss.modules;

import junit.framework.Assert;
import org.jboss.modules.util.Util;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MavenResourceTest
{
   protected static final ModuleIdentifier MODULE_ID = ModuleIdentifier.fromString("test.maven");

   @Rule
   public TemporaryFolder tmpdir = new TemporaryFolder();

   private ModuleLoader moduleLoader;

   @Before
   public void setupRepo() throws Exception {
      final File repoRoot = Util.getResourceFile(getClass(), "test/repo");;
      moduleLoader = new LocalModuleLoader(new File[] {repoRoot});
   }

   @Test
   public void testIt() throws Exception {
      System.setProperty("local.maven.repo.path", tmpdir.newFolder("repository").getAbsolutePath());
      System.setProperty("remote.maven.repo", "http://repository.jboss.org/nexus/content/groups/public/");
      try
      {
         Module module = moduleLoader.loadModule(MODULE_ID);
         URL url = module.getResource("org/jboss/resteasy/plugins/providers/jackson/ResteasyJacksonProvider.class");
         System.out.println(url);
         Assert.assertNotNull(url);
      }
      finally
      {
         System.clearProperty("local.repository.path");
         System.clearProperty("remote.repository");
      }


   }
}
