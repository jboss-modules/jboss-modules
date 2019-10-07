/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module org.jboss.modules {
  requires java.desktop;
  requires java.instrument;
  requires java.logging;
  requires java.management;
  requires java.prefs;
  exports org.jboss.modules;
  exports org.jboss.modules.filter;
  exports org.jboss.modules.log;
  exports org.jboss.modules.management;
  exports org.jboss.modules.maven;
  exports org.jboss.modules.ref;
  exports org.jboss.modules.security;
  exports org.jboss.modules.xml;
  uses java.util.logging.LogManager;
  uses java.util.prefs.PreferencesFactory;
  uses javax.management.MBeanServerBuilder;
  uses java.lang.SecurityManager;
  uses java.security.Provider;
  uses org.jboss.modules.PreMain;
  uses System.LoggerFinder;
}
