/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

/**
 * The logging interface for JBoss Modules.  Logging is abstracted in order to support logging services being provided
 * by a module.  To change the logger in use, use the {@link org.jboss.modules.Module#setModuleLogger(ModuleLogger)} method.
 * See {@link ModuleLogger} for the logging contract.
 */
package org.jboss.modules.log;