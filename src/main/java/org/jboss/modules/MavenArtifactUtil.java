package org.jboss.modules;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Helper class to resolve a maven artifact
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MavenArtifactUtil
{
   public static String[] getLocalRepositoryPaths()
   {
      String localRepositoryPath = System.getProperty("local.repository.path");
      if (localRepositoryPath == null)
      {
         File m2 = new File(System.getProperty("user.home"), ".m2");
         File repository = new File(m2, "repository");
         String path = repository.getAbsolutePath();
         String[] rtn = {path};
         return rtn;
      }
      else
      {
         return localRepositoryPath.split(File.pathSeparator);
      }
   }

   /**
    * Tries to find a maven jar artifact from the system property "local.repository.path"
    * This property is a list of platform separated directory names.  If not specified, then it looks in
    * ${user.home}/.m2/repository by default.
    *
    * If it can't find it in local paths, then will try to download from a remote repository from the system property
    * "remote.repository".  There is no default remote repository.  It will download both the pom and jar and put
    * it into the first directory listed in "local.repository.path" (or the default dir).  This directory will be
    * created if it doesn't exist.
    *
    * Finally, if you do not want a message to console, then set the system property "maven.download.message"
    * to "false"
    *
    * @param groupId
    * @param artifactId
    * @param version
    * @return absolute path to artifact, null if none exists
    * @throws Exception
    */
   public static File resolveJarArtifact(String groupId, String artifactId, String version) throws IOException {
      String artifactRelativePath = relativeArtifactPath(groupId, artifactId, version);
      String[] localRepositoryPaths = getLocalRepositoryPaths();
      for (String localRepository : localRepositoryPaths)
      {
         File fp = new File(localRepository, artifactRelativePath + ".jar");
         if (fp.exists()) return fp;
      }

      String remoteRepository = System.getProperty("remote.repository");
      if (remoteRepository == null) return null;
      if (!remoteRepository.endsWith("/")) remoteRepository += "/";

      File jarFile = new File(localRepositoryPaths[0], artifactRelativePath + ".jar");
      File pomFile = new File(localRepositoryPaths[0], artifactRelativePath + ".pom");
      String remotePomPath = remoteRepository + relativeArtifactHttpPath(groupId, artifactId, version) + ".pom";
      String remoteJarPath = remoteRepository + relativeArtifactHttpPath(groupId, artifactId, version) + ".jar";
      downloadFile(groupId + ":" + artifactId + ":" + version + ":pom", remotePomPath, pomFile);
      downloadFile(groupId + ":" + artifactId + ":" + version + ":jar", remoteJarPath, jarFile);
      return jarFile;
   }

   public static String relativeArtifactPath(String groupId, String artifactId, String version)
   {
      return relativeArtifactPath(File.separatorChar, groupId, artifactId, version);
   }

   public static String relativeArtifactHttpPath(String groupId, String artifactId, String version)
   {
      return relativeArtifactPath('/', groupId, artifactId, version);
   }


   private static String relativeArtifactPath(char separator, String groupId, String artifactId, String version)
   {
      StringBuilder builder = new StringBuilder(groupId.replace('.',separator));
      builder.append(separator)
              .append(artifactId)
              .append(separator)
              .append(version)
              .append(separator)
              .append(artifactId).append('-').append(version);
      return builder.toString();
   }

   public static void downloadFile(String artifact, String src, File dest)
           throws IOException
   {
      final int CHUNK_SIZE = 8192;
      final byte[] dataChunk = new byte[CHUNK_SIZE];

      final URL url = new URL(src);
      final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      boolean message = Boolean.parseBoolean(System.getProperty("maven.download.message", "true"));

      InputStream bis = connection.getInputStream();
      try
      {
         dest.getParentFile().mkdirs();
         FileOutputStream fos = new FileOutputStream(dest);
         BufferedOutputStream bos = new BufferedOutputStream(fos);
         try
         {
            if (message) System.out.print("Downloading " + artifact + "\r");
            int dataread = 0;
            long bytesRead = 0;
            boolean kilobytes = false;
            while (dataread >= 0)
            {
               dataread = bis.read(dataChunk,0,CHUNK_SIZE);
               if ( dataread > 0 )
               {
                  bos.write(dataChunk,0,dataread);

                  if (kilobytes)
                  {
                     dataread = dataread / 1024;
                     bytesRead += dataread;
                  }
                  else
                  {
                     bytesRead += dataread;
                     if (bytesRead > 1024)
                     {
                        bytesRead = bytesRead / 1024;
                        kilobytes = true;
                     }
                  }
                  String read = bytesRead + (kilobytes ? "k" : "b");
                  if (message) System.out.print("Downloading " + artifact + " [" + read + "]\r");
               }
            }
            if (message) System.out.println("Downloading " + artifact + " done.");
         }
         finally
         {
            bos.close();
         }
      }
      finally
      {
         bis.close();
      }
   }



}
