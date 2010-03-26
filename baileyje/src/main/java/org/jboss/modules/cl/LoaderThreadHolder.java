package org.jboss.modules.cl;

import org.jboss.logging.Logger;

import java.util.ArrayDeque;
import java.util.Queue;

class LoaderThreadHolder
{
   private static final Logger log = Logger.getLogger("org.jboss.modules");

   static final Thread LOADER_THREAD;
   static final Queue<ModuleClassLoader.LoadRequest> REQUEST_QUEUE;

   static
   {
      REQUEST_QUEUE = new ArrayDeque<ModuleClassLoader.LoadRequest>();
      LOADER_THREAD = new ModuleClassLoader.LoaderThread();
      log.debugf("Startng loader thread");
      LOADER_THREAD.start();
   }

}


