package org.jboss.modules;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Unzip {
    private static final void copyInputStream(InputStream in, OutputStream out)
            throws IOException
    {
        byte[] buffer = new byte[1024];
        int len;

        try {
            while((len = in.read(buffer)) >= 0)
                out.write(buffer, 0, len);
        } finally {
            in.close();
            out.close();
        }

    }

    public static final void execute(File src, File destdir) throws IOException {
        ZipFile zip = new ZipFile(src);

        try {
            Enumeration entries = zip.entries();

            while(entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)entries.nextElement();

                File fp = new File(destdir, entry.getName());
                if(entry.isDirectory() && !fp.exists()) fp.mkdirs();
                else {
                    File parent = fp.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    copyInputStream(zip.getInputStream(entry),
                            new BufferedOutputStream(new FileOutputStream(fp)));
                }
            }
        } finally {
            zip.close();
        }
    }
}
