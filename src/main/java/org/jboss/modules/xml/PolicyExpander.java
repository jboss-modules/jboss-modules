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

package org.jboss.modules.xml;


import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
/**
 * Expand EL Expression in Permission grants
 * Meets requirements set out here: https://docs.oracle.com/javase/8/docs/technotes/guides/security/PolicyFiles.html#PropertyExp
 *
 * @author <a href="mailto:jshepherd@redhat.com">Jason Shepherd</a>
 */
final class PolicyExpander {

    private static final int INITIAL = 0;
    private static final int GOT_DOLLAR = 1;
    private static final int GOT_OPEN_BRACE = 2;
    private static final int CAPTURE_EXPRESSION = 3;
    private static final int GOT_FILE_SEPERATOR = 4;
    public static final String ENV_START = "env.";

    private PolicyExpander() {
        // forbidden instantiation
    }

    static String expand(final String input) {
        StringBuilder valueToReturn = new StringBuilder();
        int state = INITIAL;
        int propStart = -1;
        for (int i = 0; i < input.length(); i = input.offsetByCodePoints(i, 1)) {
            final int ch = input.codePointAt(i);
            switch(state){
                case INITIAL: {
                    switch(ch){
                        case '$': {
                            state = GOT_DOLLAR;
                            continue;
                        }
                        default:{
                            valueToReturn.appendCodePoint(ch);
                            continue;
                        }
                    }
                }
                case GOT_DOLLAR:{
                    switch(ch){
                        case '{': {
                            state = GOT_OPEN_BRACE;
                            continue;
                        }
                        default:{
                            valueToReturn.append('$').appendCodePoint(ch);
                            state = INITIAL;
                            continue;
                        }
                    }
                }
                case GOT_OPEN_BRACE:{
                    switch(ch){
                        case '}': {
                            expandValue(input, valueToReturn, propStart, i);
                            state = INITIAL;
                            continue;
                        }
                        case '/': {
                            state = GOT_FILE_SEPERATOR;
                            continue;
                        }
                        default:{
                            propStart = i;
                            state = CAPTURE_EXPRESSION;
                            continue;
                        }
                    }
                }
                case CAPTURE_EXPRESSION:{
                    switch(ch) {
                        case '}': {
                            expandValue(input, valueToReturn, propStart, i);
                            state = INITIAL;
                            continue;
                        }
                        default: {
                            //part of an expression, skip
                            continue;
                        }
                    }
                }
                case GOT_FILE_SEPERATOR:{
                    switch(ch){
                        case '}': {
                            valueToReturn.append(File.separator);
                            state = INITIAL;
                            continue;
                        }
                        default:{
                            propStart = i - 1; //include skipped '/'
                            state = CAPTURE_EXPRESSION;
                            continue;
                        }
                    }
                }
            }
        }
        String returnValue = valueToReturn.toString();
        if(returnValue.isEmpty())
            return null;
        return returnValue;
    }

    private static void expandValue(String input, StringBuilder valueToReturn, int valueStart, int offset){
        final String value = input.substring(valueStart, offset);
        if(value.startsWith(ENV_START)){
            // 'env.' prefix is optional - and supported due to backward compatibility
            String var = getEnvironmentVariable(value.substring(ENV_START.length()));
            if(var != null)
                valueToReturn.append(var);
            return;
        }
        // System properties first
        String prop = getSystemProperty(value);
        if(prop != null) {
            valueToReturn.append(prop);
            return;
        }
        // Environment variables second (some users may prefer to not specify 'env.' prefix)
        prop = getEnvironmentVariable(value);
        if(prop != null) {
            valueToReturn.append(prop);
        }
    }

    private static String getEnvironmentVariable(String key) {
        if(key == null || key.isEmpty())
            return null;
        return AccessController.doPrivileged(new PrivilegedAction<String>(){
            @Override
            public String run() {
                return System.getenv(key);
            }
        });
    }

    private static String getSystemProperty(String key){
        if(key == null || key.isEmpty())
            return null;
        return AccessController.doPrivileged(new PrivilegedAction<String>(){
            @Override
            public String run() {
                return System.getProperty(key);
            }
        });
    }
}
