package org.jboss.modules;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.BitSet;

//Copied from sun.net.www.ParseUtil, and sun.security.util.PropertyExpander

public class PropertyExpander {
    static BitSet encodedInPath;

    static {
        encodedInPath = new BitSet(256);

        // Set the bits corresponding to characters that are encoded in the
        // path component of a URI.

        // These characters are reserved in the path segment as described in
        // RFC2396 section 3.3.
        encodedInPath.set('=');
        encodedInPath.set(';');
        encodedInPath.set('?');
        encodedInPath.set('/');

        // These characters are defined as excluded in RFC2396 section 2.4.3
        // and must be escaped if they occur in the data part of a URI.
        encodedInPath.set('#');
        encodedInPath.set(' ');
        encodedInPath.set('<');
        encodedInPath.set('>');
        encodedInPath.set('%');
        encodedInPath.set('"');
        encodedInPath.set('{');
        encodedInPath.set('}');
        encodedInPath.set('|');
        encodedInPath.set('\\');
        encodedInPath.set('^');
        encodedInPath.set('[');
        encodedInPath.set(']');
        encodedInPath.set('`');

        // US ASCII control characters 00-1F and 7F.
        for (int i = 0; i < 32; i++)
            encodedInPath.set(i);
        encodedInPath.set(127);
    }

    public static class ExpandException extends GeneralSecurityException {
        public ExpandException(String msg) {
            super(msg);
        }
    }

    public static String expand(String value) throws ExpandException{
        return expand(value, false);
    }

    public static String expand(String value, boolean encodeURL) throws ExpandException {
        if (value == null)
            return null;

        int p = value.indexOf("${", 0);

        // no special characters
        if (p == -1)
            return value;

        StringBuffer sb = new StringBuffer(value.length());
        int max = value.length();
        int i = 0; // index of last character we copied

        scanner: while (p < max) {
            if (p > i) {
                // copy in anything before the special stuff
                sb.append(value.substring(i, p));
                i = p;
            }
            int pe = p + 2;

            // do not expand ${{ ... }}
            if (pe < max && value.charAt(pe) == '{') {
                pe = value.indexOf("}}", pe);
                if (pe == -1 || pe + 2 == max) {
                    // append remaining chars
                    sb.append(value.substring(p));
                    break scanner;
                } else {
                    // append as normal text
                    pe++;
                    sb.append(value.substring(p, pe + 1));
                }
            } else {
                while ((pe < max) && (value.charAt(pe) != '}')) {
                    pe++;
                }
                if (pe == max) {
                    // no matching '}' found, just add in as normal text
                    sb.append(value.substring(p, pe));
                    break scanner;
                }
                String prop = value.substring(p + 2, pe);
                if (prop.equals("/")) {
                    sb.append(java.io.File.separatorChar);
                } else {
                    String val = System.getProperty(prop);
                    if (val != null) {
                        if (encodeURL) {
                            // encode 'val' unless it's an absolute URI
                            // at the beginning of the string buffer
                            try {
                                if (sb.length() > 0 || !(new URI(val)).isAbsolute()) {
                                    val = encodePath(val);
                                }
                            } catch (URISyntaxException use) {
                                val = encodePath(val);
                            }
                        }
                        sb.append(val);
                    } else {
                        throw new ExpandException("unable to expand property " + prop);
                    }
                }
            }
            i = pe + 1;
            p = value.indexOf("${", i);
            if (p == -1) {
                // no more to expand. copy in any extra
                if (i < max) {
                    sb.append(value.substring(i, max));
                }
                // break out of loop
                break scanner;
            }
        }
        return sb.toString();
    }

    public static String encodePath(String path) {
        return encodePath(path, true);
    }

    /*
     * flag indicates whether path uses platform dependent File.separatorChar or
     * not. True indicates path uses platform dependent File.separatorChar.
     */
    public static String encodePath(String path, boolean flag) {
        char[] retCC = new char[path.length() * 2 + 16];
        int retLen = 0;
        char[] pathCC = path.toCharArray();

        int n = path.length();
        for (int i = 0; i < n; i++) {
            char c = pathCC[i];
            if ((!flag && c == '/') || (flag && c == File.separatorChar))
                retCC[retLen++] = '/';
            else {
                if (c <= 0x007F) {
                    if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
                        retCC[retLen++] = c;
                    } else if (encodedInPath.get(c))
                        retLen = escape(retCC, c, retLen);
                    else
                        retCC[retLen++] = c;
                } else if (c > 0x07FF) {
                    retLen = escape(retCC, (char) (0xE0 | ((c >> 12) & 0x0F)), retLen);
                    retLen = escape(retCC, (char) (0x80 | ((c >> 6) & 0x3F)), retLen);
                    retLen = escape(retCC, (char) (0x80 | ((c >> 0) & 0x3F)), retLen);
                } else {
                    retLen = escape(retCC, (char) (0xC0 | ((c >> 6) & 0x1F)), retLen);
                    retLen = escape(retCC, (char) (0x80 | ((c >> 0) & 0x3F)), retLen);
                }
            }
            // worst case scenario for character [0x7ff-] every single
            // character will be encoded into 9 characters.
            if (retLen + 9 > retCC.length) {
                int newLen = retCC.length * 2 + 16;
                if (newLen < 0) {
                    newLen = Integer.MAX_VALUE;
                }
                char[] buf = new char[newLen];
                System.arraycopy(retCC, 0, buf, 0, retLen);
                retCC = buf;
            }
        }
        return new String(retCC, 0, retLen);
    }

    private static int escape(char[] cc, char c, int index) {
        cc[index++] = '%';
        cc[index++] = Character.forDigit((c >> 4) & 0xF, 16);
        cc[index++] = Character.forDigit(c & 0xF, 16);
        return index;
    }
}