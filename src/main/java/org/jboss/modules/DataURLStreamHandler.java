/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

package org.jboss.modules;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Base64;

/**
 */
final class DataURLStreamHandler extends URLStreamHandler {

    private static final DataURLStreamHandler INSTANCE = new DataURLStreamHandler();

    static DataURLStreamHandler getInstance() {
        return INSTANCE;
    }

    private DataURLStreamHandler() {
    }

    protected URLConnection openConnection(final URL url) throws IOException {
        return new DataURLConnection(url);
    }

    static final class DataURLConnection extends URLConnection {
        private static final int ST_INITIAL = 0;
        private static final int ST_TYPE = 1;
        private static final int ST_SUBTYPE = 2;
        private static final int ST_PARAMETER = 3;
        private static final int ST_PARAMETER_VAL = 4;

        private final byte[] content;
        private final String contentString;
        private final String contentType;

        DataURLConnection(final URL url) throws IOException {
            super(url);

            // We have to use toString() because otherwise URL will eat '?' characters in the content!
            String urlString = url.toString();
            if (! urlString.startsWith("data:")) {
                // should not happen
                throw new ProtocolException("Wrong URL scheme");
            }

            String contentType = "text/plain";
            StringBuilder contentTypeBuilder = null;
            int state = ST_INITIAL;
            final int len = urlString.length();
            int cp;
            int postCt = -1;
            int content = -1;
            int paramStart = -1;
            int paramEnd = -1;
            boolean base64 = false;
            String charsetName = null;
            boolean text = true;
            Charset charset = StandardCharsets.US_ASCII;
            for (int i = 5; i < len; i = urlString.offsetByCodePoints(i, 1)) {
                cp = urlString.codePointAt(i);
                if (state == ST_INITIAL) {
                    if (isCTToken(cp)) {
                        state = ST_TYPE;
                        contentTypeBuilder = new StringBuilder();
                        contentTypeBuilder.appendCodePoint(Character.toLowerCase(cp));
                    } else if (cp == ';') {
                        // default content-type
                        contentTypeBuilder = new StringBuilder();
                        contentTypeBuilder.append("text/plain");
                        postCt = contentTypeBuilder.length();
                        paramStart = urlString.offsetByCodePoints(i, 1);
                        state = ST_PARAMETER;
                    } else if (cp == ',') {
                        content = urlString.offsetByCodePoints(i, 1);
                        // done
                        break;
                    } else {
                        throw invalidChar(i);
                    }
                } else if (state == ST_TYPE) {
                    if (isCTToken(cp)) {
                        contentTypeBuilder.appendCodePoint(Character.toLowerCase(cp));
                    } else if (cp == '/') {
                        state = ST_SUBTYPE;
                        contentTypeBuilder.append('/');
                    } else {
                        throw invalidChar(i);
                    }
                } else if (state == ST_SUBTYPE) {
                    if (isCTToken(cp)) {
                        contentTypeBuilder.appendCodePoint(Character.toLowerCase(cp));
                    } else if (cp == ';') {
                        postCt = contentTypeBuilder.length();
                        paramStart = urlString.offsetByCodePoints(i, 1);
                        state = ST_PARAMETER;
                    } else if (cp == ',') {
                        contentType = contentTypeBuilder.toString();
                        text = contentType.startsWith("text/");
                        content = urlString.offsetByCodePoints(i, 1);
                        // done
                        break;
                    } else {
                        throw invalidChar(i);
                    }
                } else if (state == ST_PARAMETER) {
                    if (isCTToken(cp)) {
                        // OK
                    } else if (cp == ';' || cp == ',') {
                        // no value
                        if (i - paramStart == 6 && urlString.regionMatches(true, paramStart, "base64", 0, 6)) {
                            base64 = true;
                        } else {
                            contentTypeBuilder.append(';').append(urlString.substring(paramStart, i));
                        }
                        if (cp == ',') {
                            text = contentTypeBuilder.lastIndexOf("text/", 5) != -1;
                            if (text && charsetName != null) {
                                contentTypeBuilder.insert(postCt, ";charset=" + charsetName);
                            }
                            contentType = contentTypeBuilder.toString();
                            content = urlString.offsetByCodePoints(i, 1);
                            // done
                            break;
                        }
                        paramStart = urlString.offsetByCodePoints(i, 1);
                        // restart ST_PARAMETER
                    } else if (cp == '=') {
                        paramEnd = i;
                        state = ST_PARAMETER_VAL;
                    } else {
                        throw invalidChar(i);
                    }
                } else if (state == ST_PARAMETER_VAL) {
                    if (isCTToken(cp)) {
                        // OK
                    } else if (cp == ';' || cp == ',') {
                        // there is a value
                        final String value = urlString.substring(paramEnd + 1, i);
                        if (paramEnd - paramStart == 7 && urlString.regionMatches(true, paramStart, "charset", 0, 7)) {
                            try {
                                charset = Charset.forName(value);
                            } catch (UnsupportedCharsetException e) {
                                throw e;
                            } catch (Throwable t) {
                                final UnsupportedCharsetException uce = new UnsupportedCharsetException(value);
                                uce.initCause(t);
                                throw uce;
                            }
                            charsetName = value;
                        } else {
                            contentTypeBuilder.append(urlString.substring(paramStart, i));
                        }
                        if (cp == ',') {
                            text = contentTypeBuilder.lastIndexOf("text/", 5) != -1;
                            if (text && charsetName != null) {
                                contentTypeBuilder.insert(postCt, ";charset=" + charsetName);
                            }
                            contentType = contentTypeBuilder.toString();
                            content = urlString.offsetByCodePoints(i, 1);
                            // done
                            break;
                        }
                        state = ST_PARAMETER;
                    } else {
                        throw invalidChar(i);
                    }
                } else {
                    throw new IllegalStateException();
                }
            }
            if (content == -1) {
                throw new ProtocolException("Missing content");
            }
            // now, get the content
            byte[] bytes;
            String str;
            if (base64) {
                bytes = Base64.getMimeDecoder().decode(urlString.substring(content).replaceAll("\\s+", ""));
                if (text) {
                    str = new String(bytes, charset);
                } else {
                    str = null;
                }
            } else {
                if (text) {
                    str = URLDecoder.decode(urlString.substring(content), charset.name());
                    bytes = str.getBytes(charset);
                } else {
                    // this is a bit hacky...
                    bytes = URLDecoder.decode(urlString.substring(content), StandardCharsets.ISO_8859_1.name()).getBytes(StandardCharsets.ISO_8859_1);
                    str = null;
                }
            }
            this.content = bytes;
            this.contentType = contentType;
            this.contentString = str;
        }

        private static ProtocolException invalidChar(int pos) {
            return new ProtocolException("Invalid character at position " + pos);
        }

        private static boolean isCTToken(int cp) {
            return 0x21 <= cp && cp <= 0x7e &&
                cp != '(' && cp != ')' && cp != '<' && cp != '>' && cp != '@' &&
                cp != ',' && cp != ';' && cp != ':' && cp != '\\' && cp != '"' &&
                cp != '/' && cp != '[' && cp != ']' && cp != '?' && cp != '=';
        }

        public void connect() {
            connected = true;
        }

        public int getContentLength() {
            return content.length;
        }

        public String getContentType() {
            return contentType;
        }

        public Object getContent() {
            return contentString != null ? contentString : content.clone();
        }

        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }
    }
}
