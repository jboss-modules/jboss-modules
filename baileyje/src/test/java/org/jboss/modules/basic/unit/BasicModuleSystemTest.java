package org.jboss.modules.basic.unit;

import org.jboss.modules.metadata.ImportMetaData;
import org.jboss.modules.spi.ClassSpec;
import org.jboss.modules.spi.Module;
import org.jboss.modules.basic.system.BasicModuleSystem;
import org.jboss.modules.basic.system.RepositoryModuleLoader;
import org.jboss.modules.basic.system.ResolutionException;
import org.jboss.modules.spi.ResourceSpec;
import org.junit.Test;

import java.io.File;
import java.net.URL;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class BasicModuleSystemTest
{
   @Test
   public void testCircularModule() throws Exception
   {
      URL repoUrl = BasicModuleSystemTest.class.getResource("/repository");
      File repoFile = new File(repoUrl.toURI());


      BasicModuleSystem moduleSystem = new BasicModuleSystem();
      moduleSystem.addModuleLoader(new RepositoryModuleLoader(repoFile));

      Module module = moduleSystem.getModule("circular", "a-module", "1.0");
      assert module != null : "Module should not be null";

      module.loadClass("org.jboss.test.other.OtherClass");
   }

   @Test
   public void testMissingModule() throws Exception
   {
      URL repoUrl = BasicModuleSystemTest.class.getResource("/repository");
      File repoFile = new File(repoUrl.toURI());


      BasicModuleSystem moduleSystem = new BasicModuleSystem();
      moduleSystem.addModuleLoader(new RepositoryModuleLoader(repoFile));

      try
      {
         moduleSystem.getModule("missing-dep", "module", "1.0");
         assert true : "Should have thrown a resolution exception";
      }
      catch(ResolutionException expected)
      {
      }
   }

   @Test
   public void testBasicModuleClassLoading() throws Exception
   {
      URL repoUrl = BasicModuleSystemTest.class.getResource("/repository");
      File repoFile = new File(repoUrl.toURI());


      BasicModuleSystem moduleSystem = new BasicModuleSystem();
      moduleSystem.addModuleLoader(new RepositoryModuleLoader(repoFile));

      ImportMetaData importMetaData = new ImportMetaData();
      importMetaData.setModuleName("test-module");
      importMetaData.setModuleVersion("1.0");

      Module module = moduleSystem.getModule("test", "module", "1.0");

      ClassSpec<?> classSpec = module.loadClass("org.jboss.test.SomeClass");

      classSpec.getType().newInstance();

      System.out.println(classSpec.getModule());

      ResourceSpec resource = module.getResource("config/marker");
      System.out.println(resource);

      System.out.println(module.getResources("config/marker"));


      importMetaData = new ImportMetaData();
      importMetaData.setModuleName("other-module");
      importMetaData.setModuleVersion("1.0");
      importMetaData.setPackageName("org.jboss.test.other");


      module = moduleSystem.getModule("other", "module", "1.0");


      classSpec = module.loadClass("org.jboss.test.other.TestClass");
      System.out.println(classSpec.getType().getClassLoader());

      resource = module.getResource("config/marker");
      System.out.println(resource);

      classSpec = module.loadClass("org.jboss.test.SomeClass");
      assert classSpec == null : "Should have thrown an exception";
   }
}
