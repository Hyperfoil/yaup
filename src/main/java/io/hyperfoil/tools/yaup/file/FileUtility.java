package io.hyperfoil.tools.yaup.file;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import io.hyperfoil.tools.yaup.HashedLists;
import io.hyperfoil.tools.yaup.Sets;

import java.io.*;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by wreicher
 * <p>
 * What I want
 * Search:
 * //foo.txt
 * ./foo/bar//biz.txt
 * myEar.ear#/myWar.war#myJar.jar#entry.xml&gt;xpath
 * myEar.ear#/myWar.war#myJar.jar#entry.json&gt;jsonPath
 * myArchive.jar#//findMeAnywhereIncludingNestedArchives.txt
 * <p>
 * thoughts on how to do it:
 * need some way to treat a File and an ArchiveInputStream the same way (wrapper around F.getFiles() and AIS.getNextEntry()
 * AIS flattens all paths
 * /foo//bar needs to be applied as a single match rule or we getAllEntires into a list and pass to /foo, then //bar
 * using the list works well with getFiles() but is inefficient use of memory
 * split search pattern like XmlPath?
 */
public class FileUtility {

   private class Child {
      private File file;
      private ArchiveEntry archiveEntry;
   }

   public enum Format {bz2, gz, tar, zip, Z, tarZ, rar, sevenZ, xz}

   ;

   private static final HashedLists<Format, int[]> magic_numbers = new HashedLists<>();

   static {//https://en.wikipedia.org/wiki/List_of_file_signatures
      magic_numbers.put(Format.gz, new int[]{0x1F, 0x8B});
      magic_numbers.put(Format.tar, new int[]{0x75, 0x73, 0x74, 0x61, 0x72});
      magic_numbers.putAll(Format.zip, Arrays.asList(
         new int[]{0x50, 0x4B, 0x03, 0x04},
         new int[]{0x50, 0x4B, 0x05, 0x06},
         new int[]{0x50, 0x4B, 0x07, 0x08}
      ));
      magic_numbers.putAll(Format.Z, Arrays.asList(
         new int[]{0x1F, 0x9D},
         new int[]{0x1F, 0xA0}
      ));
      magic_numbers.put(Format.tarZ, new int[]{0x1F, 0xA0});
      magic_numbers.put(Format.bz2, new int[]{0x42, 0x5A, 0x68});
      magic_numbers.putAll(Format.rar, Arrays.asList(
         new int[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00},
         new int[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00}
      ));
      magic_numbers.put(Format.sevenZ, new int[]{0x37, 0x7A, 0xBC, 0xAF, 0x72, 0x1C});
      magic_numbers.put(Format.xz, new int[]{0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00, 0x00});

   }

   public static final String ARCHIVE_KEY = "#";
   public static final String SEARCH_KEY = ">";
   public static final String REMOTE_KEY = ":";

   public static int OPERATION_LENGTH = 2;
   public static final String ADD_OPERATION = "++";
   public static final String DELETE_OPERATION = "--";
   public static final String SET_OPERATION = "==";

   public static final Set<String> OPERATIONS = Collections.unmodifiableSet(Sets.of(ADD_OPERATION, DELETE_OPERATION, SET_OPERATION));

   /**
    * Get a <code>List</code> of all the files in baseDir that contain nameSubstring
    *
    * @param baseDir       the full path to the directory to start the search
    * @param nameSubstring the substring to find in the file name
    * @param recursive     - search subdirectories if <code>true</code>
    * @return an <code>Immutable</code> list of the files which match the search
    */
   public static List<String> getFiles(String baseDir, String nameSubstring,
                                       boolean recursive) {
      return search(baseDir, nameSubstring, recursive, true, false, false);
   }

   /**
    * Get a <code>List</code> of all the directories in baseDir that contain nameSubstring
    *
    * @param baseDir       the full path to the directory to start the search
    * @param nameSubstring the substring to find in the file name
    * @param recursive     - search subdirectories if <code>true</code>
    * @return an <code>Immutable</code> list of the files which match the search
    */
   public static List<String> getDirectories(String baseDir, String nameSubstring,
                                             boolean recursive) {
      return search(baseDir, nameSubstring, recursive, false, true, false);
   }

   /**
    * @param fileName      file name to search for within an archive
    * @return <code>true</code> if fileName refers to an entry within an archive file and is not an existing file
    */
   public static boolean isArchiveEntryPath(String fileName) {
      if (fileName == null || !fileName.contains(ARCHIVE_KEY)) {
         return false;
      }
      File parentFile = new File(fileName.substring(0, fileName.indexOf(ARCHIVE_KEY)));
      File tmpFile = new File(fileName);
      return (!tmpFile.exists() && parentFile.exists());
   }

   private static InputStream wrapStream(InputStream stream, String path) throws IOException {
      InputStream rtrn = stream;
      if (path.endsWith(".tar.gz") || path.endsWith(".tgz")) {
         rtrn = new TarArchiveInputStream(new GzipCompressorInputStream(stream));
      } else if (path.endsWith((".gz"))) {
         rtrn = new GzipCompressorInputStream(stream);
      } else if (path.endsWith(".tar")) {
         rtrn = new TarArchiveInputStream(stream);
      } else if (path.endsWith(".zip")) {
         rtrn = new ZipArchiveInputStream(stream);
      } else if (path.endsWith(".Z")) {
         rtrn = new ZCompressorInputStream(stream);
      } else if (path.endsWith(".tar.bz2") || path.endsWith("tbz2")) {
         rtrn = new TarArchiveInputStream(new BZip2CompressorInputStream(stream));
      } else if (path.endsWith(".bz2")) {
         rtrn = new BZip2CompressorInputStream(stream);
      } else if (path.endsWith(".jar") || path.endsWith(".war") || path.endsWith(".ear")) {
         rtrn = new JarArchiveInputStream(stream);
      }
      return rtrn;
   }



   /**
    * Get the size of the input target. Either the total file size of a folder or the inflated size of a compressed entry
    *
    * @param fullPath the path for the file with an optional archive entry subPaths for archive files (e.g. jars)
    * @return an InputStream if fullPath exists or null
    */
   public static long getInputSize(String fullPath) {
      String archivePath = fullPath;
      String entryPath = "";
      //TODO get an input stream for remote file ?
      if (isArchiveEntryPath(fullPath)) {
         archivePath = getArchiveFilePath(fullPath);
         entryPath = getArchiveEntrySubPath(fullPath);
      }
      File archiveFile = new File(archivePath);
      if (!archiveFile.exists()) {
         return 0;
      }

      if(archiveFile.isDirectory()){
         Path folder = archiveFile.toPath();
         try {
            long size = Files.walk(folder)
               .filter(p -> p.toFile().isFile())
               .mapToLong(p -> p.toFile().length())
               .sum();
            return size;
         } catch (IOException e) {
            e.printStackTrace();
         }

      }

      try {
         InputStream rtrn = new FileInputStream(archiveFile);
         if (archivePath.endsWith(".tar.gz") || archivePath.endsWith(".tgz")) {
            rtrn = new TarArchiveInputStream(new GzipCompressorInputStream(rtrn));
         } else if (archivePath.endsWith((".gz"))) {
            rtrn = new GzipCompressorInputStream(rtrn);
         } else if (archivePath.endsWith(".tar")) {
            rtrn = new TarArchiveInputStream(rtrn);
         } else if (archivePath.endsWith(".zip")) {
            rtrn = new ZipArchiveInputStream(rtrn);
         } else if (archivePath.endsWith(".Z")) {
            rtrn = new ZCompressorInputStream(rtrn);
         } else if (archivePath.endsWith(".tar.bz2") || archivePath.endsWith("tbz2")) {
            rtrn = new TarArchiveInputStream(new BZip2CompressorInputStream(rtrn));
         } else if (archivePath.endsWith(".bz2")) {
            rtrn = new BZip2CompressorInputStream(rtrn);
         } else if (archivePath.endsWith(".jar") || archivePath.endsWith(".ear") || archivePath.endsWith(".war")) {
            rtrn = new JarArchiveInputStream(rtrn);
         } else { //just a file
            return archiveFile.length();
         }
         if (!entryPath.isEmpty()) {
            if (rtrn instanceof ArchiveInputStream) {
               ArchiveInputStream ais = (ArchiveInputStream) rtrn;
               ArchiveEntry ae = null;
               InputStream entryStream = null;
               while ((ae = ais.getNextEntry()) != null) {

                  String aeName = ae.getName();
                  if (aeName.startsWith("./")) {
                     aeName = aeName.substring(2);
                  } else if (aeName.startsWith("/")) {
                     aeName = aeName.substring(1);
                  }
                  if (entryPath.startsWith("./")) {
                     entryPath = entryPath.substring(2);
                  } else if (entryPath.startsWith("/")) {
                     entryPath = entryPath.substring(1);
                  }

                  if (aeName.equals(entryPath)) {
                     ae.getSize();
                     entryStream = (rtrn);
                     break;
                  }
               }
               if (entryStream == null) {
                  throw new RuntimeException("Could not find " + entryPath + " in " + archivePath + " on local file system");
               } else {
                  rtrn = entryStream;
               }
            } else {
               throw new RuntimeException("Could not find " + entryPath + " in " + archivePath + " because it is not an archive collection");
            }
         }else{ // not looking inside the arcive
            return archiveFile.length();

         }
         if (rtrn != null) {
            return rtrn.available();
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return 0;
   }

   public static List<String> lines(String fullPath) {
      String content = readFile(fullPath);
      return new ArrayList<>(Arrays.asList(content.split("\r?\n")));
   }

   public static Stream<String> stream(String path) {
      return new BufferedReader(new InputStreamReader(FileUtility.getInputStream(path))).lines();
   }

   public static String readHead(String fullPath, int lines) {
      StringBuilder sb = new StringBuilder();
      InputStream stream = getInputStream(fullPath);
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

      try {
         for (int i = 0; i < lines; i++) {
            if (i > 0) {
               sb.append(System.lineSeparator());
            }
            sb.append(reader.readLine());
         }
      } catch (IOException e) {
         //TODO handle IOE
      }
      return sb.toString();
   }

   public static String readFile(String fullPath) {
      InputStream stream = getInputStream(fullPath);
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      int nRead;
      byte[] data = new byte[16384];
      try {
         while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
         }
         buffer.flush();
      } catch (IOException e) {
         //TODO handle IOE
      }

      return new String(buffer.toByteArray());

   }

   private static InputStream getInputStream(ArchiveInputStream inputStream, String currentPath) throws IOException {
      String entryPath = currentPath;
      String remainder = "";
      if (currentPath.contains(ARCHIVE_KEY)) {
         entryPath = getArchiveFilePath(currentPath);
         remainder = getArchiveEntrySubPath(currentPath);
      }

      if (entryPath.startsWith("./")) {
         entryPath = entryPath.substring(2);
      }
      if (entryPath.startsWith("/")) {
         entryPath = entryPath.substring(1);
      }

      ArchiveEntry entry;
      InputStream rtrn = null;
      while ((entry = inputStream.getNextEntry()) != null) {
         String name = entry.getName();
         if (name.startsWith("./")) {
            name = name.substring(2);
         }
         if (name.startsWith("/")) {
            name = name.substring(1);
         }
         if (name.equals(entryPath)) {
            rtrn = wrapStream(inputStream, entryPath);
            if (!remainder.isEmpty() && isArchive(entryPath)) {
               if (rtrn instanceof ArchiveInputStream) {
                  rtrn = getInputStream((ArchiveInputStream) rtrn, remainder);
               }
            }
            break;
         }
      }
      return rtrn;
   }

   public static File getFile(String fullPath,boolean delete){
      String archivePath = fullPath;
      String entryPath = "";
      if(isArchiveEntryPath(fullPath)){
         archivePath = getArchiveFilePath(fullPath);
         entryPath = getArchiveEntrySubPath(fullPath);
      }
      if(isArchive(archivePath)){
         try {
            FileSystem fs = FileSystems.newFileSystem(Paths.get(archivePath),FileUtility.class.getClassLoader());
            Path entry = fs.getPath(entryPath);
            String prefix = entryPath;
            if(prefix.contains("/")){
               prefix = prefix.substring(prefix.lastIndexOf("/"));
            }
            Path tmp = Files.createTempFile(prefix+"_",".yaup");
            Files.copy(entry, tmp, StandardCopyOption.REPLACE_EXISTING);
            File rtrn = tmp.toFile();
            if(delete){
               rtrn.deleteOnExit();
            }
            return rtrn;
         } catch (IOException e) {
            e.printStackTrace();
            return null;
         }

      }else{
         return new File(fullPath);
      }


   }
   public static InputStream getInputStream(String fullPath) {
      InputStream rtrn = null;
      String archivePath = fullPath;
      String entryPath = "";
      //TODO get an input stream for remote file ?
      if (isArchiveEntryPath(fullPath)) {
         archivePath = getArchiveFilePath(fullPath);
         entryPath = getArchiveEntrySubPath(fullPath);
      }
      //for each of the possible fileSuffix types
      try {
         if (!(new File(archivePath)).exists()) {
            rtrn = null;
            throw new RuntimeException("Cannot find " + archivePath + " on local file system");
         }
         rtrn = new BufferedInputStream(new FileInputStream(archivePath));
         rtrn = wrapStream(rtrn, archivePath);
         if (!entryPath.isEmpty() && rtrn instanceof ArchiveInputStream) {
            rtrn = getInputStream((ArchiveInputStream) rtrn, entryPath);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      return rtrn;
   }

   /**
    * Get an Unmodifiable list of the entries in the archive
    *
    * @param archivePath full path to archive to retrieve entries from
    * @return a List containing all the named entires in the archive or an empty list
    */
   public static List<String> getArchiveEntries(String archivePath) {
      return getArchiveEntries(archivePath, false);
   }

   public static List<String> getArchiveEntries(String archivePath, boolean deep) {
      List<String> rtrn = new LinkedList<String>();
      try (InputStream is = getInputStream(archivePath)) {
         if (is != null && is instanceof ArchiveInputStream) {
            try {
               addEntries((ArchiveInputStream) is, "", rtrn, deep);
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return Collections.unmodifiableList(rtrn);
   }

   private static void addEntries(ArchiveInputStream inputStream, String prefix, List<String> rtrn, boolean recursive) throws IOException {
      if (inputStream != null) {
         ArchiveEntry entry = null;
         while ((entry = inputStream.getNextEntry()) != null) {
            String name = entry.getName();
            rtrn.add(prefix + name);
            if (recursive && isArchive(name)) {
               InputStream wrappedStream = wrapStream(inputStream, name);
               if (wrappedStream instanceof ArchiveInputStream) {
                  addEntries((ArchiveInputStream) wrappedStream, prefix + name + ARCHIVE_KEY, rtrn, recursive);
               }
            }
         }
      }
   }

   /**
    * return the archive entry subPath portion or an empty <code>String</code>
    *
    * @param archiveEntryPath    fully-qualified filename of archive
    * @return the archive entry subPath or an empty String
    */
   public static String getArchiveEntrySubPath(String archiveEntryPath) {
      if (archiveEntryPath == null || archiveEntryPath.isEmpty()) {
         return "";
      }
      if (archiveEntryPath.contains(ARCHIVE_KEY)) {
         return archiveEntryPath.substring(archiveEntryPath
            .indexOf(ARCHIVE_KEY) + ARCHIVE_KEY.length());
      }

      return "";
   }

   /**
    * return the path to the archive file, removing any archive entry subPath
    *
    * @param archiveEntryPath      fully-qualified filename of archive
    * @return the archive's filePath or an empty String
    */
   public static String getArchiveFilePath(String archiveEntryPath) {
      if (archiveEntryPath == null || archiveEntryPath.isEmpty()) {
         return "";
      }
      if (archiveEntryPath.contains(ARCHIVE_KEY)) {
         return archiveEntryPath.substring(0,
            archiveEntryPath.indexOf(ARCHIVE_KEY));
      }

      return "";
   }

   public static String getDirectory(String fileName) {
      if (isArchiveEntryPath(fileName)) {
         fileName = fileName.substring(0, fileName.indexOf(ARCHIVE_KEY));
      }
      if (fileName.contains("\\")) {
         fileName = fileName.replaceAll("\\\\", "/");
      }
      if (fileName.endsWith("/")) {
         return fileName;
      } else {
         return fileName.substring(0, fileName.lastIndexOf("/") + 1);
      }
   }

   public static String getParentDirectory(String fileName) {
      if (fileName == null) {
         return "";
      }
      if (fileName.contains("\\")) {
         fileName = fileName.replaceAll("\\\\", "/");
      }
      if (fileName.endsWith("/")) {
         fileName = fileName.substring(0, fileName.length() - 1);
      }

      return fileName.substring(0, fileName.lastIndexOf("/") + 1);
   }

   private static final List<String> search(String baseDir, String nameSubstring, boolean recursive, boolean wantFiles, boolean depthFirst, boolean inArchive) {
      List<String> rtrn = new ArrayList<String>();
      List<String> toParse = new ArrayList<String>();
      toParse.add(baseDir);

      while (!toParse.isEmpty()) {
         String next = toParse.remove(0);
         File f = new File(next);
         if (f.exists() && f.isDirectory()) {
            File subs[] = f.listFiles();
            if (subs != null) {
               for (File sub : subs) {
                  if (recursive && sub.isDirectory()) {
                     if (depthFirst) {
                        toParse.add(0, sub.getAbsolutePath());
                     } else {
                        toParse.add(sub.getAbsolutePath());
                     }
                  }
                  // probably don't need both boolean comparisons but I'm
                  // curious if isFile!=isDirectory is a contract
                  if (sub.isFile() == wantFiles && sub.isDirectory() != wantFiles) {
                     // if there is name filtering
                     if (nameSubstring != null && !nameSubstring.isEmpty()) {
                        if (sub.getName().contains(nameSubstring) /*&& !isArchive(sub)*/) {
                           rtrn.add(sub.getAbsolutePath());
                        }
                     } else {
                        rtrn.add(sub.getAbsolutePath());
                     }
                  }
                  if (inArchive && sub.isFile() && isArchive(sub)) {

                     List<String> entries = getArchiveEntries(sub.getPath());
                     for (String entry : entries) {
                        if (entry.contains(nameSubstring)) {
                           rtrn.add(entry);
                        }
                     }
                  }
               }
            }
         } else if (f.exists()) {
            if (f.isFile() == wantFiles && f.isDirectory() != wantFiles) {
               // if there is name filtering
               if (nameSubstring != null && !nameSubstring.isEmpty()) {
                  if (f.getName().contains(nameSubstring) /*&& !isArchive(sub)*/) {
                     rtrn.add(f.getAbsolutePath());
                  }
               }
            }
            if (inArchive && f.isFile() && isArchive(f)) {

               List<String> entries = getArchiveEntries(f.getPath());
               for (String entry : entries) {
                  if (entry.contains(nameSubstring)) {
                     rtrn.add(entry);
                  }
               }
            }

         }
      }
      return rtrn;
   }

   public static JSONObject readJsonObjectFile(String fileName) {
      JSONObject rtrn = new JSONObject();
      if (Files.exists(Paths.get(fileName))) {
         try {
            rtrn = new JSONObject(new String(Files.readAllBytes(Paths.get(fileName))));
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      return rtrn;
   }

   public static JSONArray readJsonArrayFile(String fileName) {
      JSONArray rtrn = new JSONArray();
      if (Files.exists(Paths.get(fileName))) {
         try {
            rtrn = new JSONArray(new String(Files.readAllBytes(Paths.get(fileName))));
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      return rtrn;
   }

   public static Object readObjectFile(String fileName) {
      return readObjectFile(new File(fileName));
   }

   public static Object readObjectFile(File file) {
      ObjectInputStream ois = null;
      Object rtrn = null;
      try {
         ois = new ObjectInputStream(new FileInputStream(file));
         rtrn = ois.readObject();
         ois.close();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } catch (ClassNotFoundException e) {
         e.printStackTrace();
      } finally {
         if (ois != null) {
            try {
               ois.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
      return rtrn;
   }

   public static int lineCount(String substring, String fileName) {
      int index = 0 - substring.length();
      int count = 0;
      String line = null;
      try {
         BufferedReader reader = new BufferedReader(new FileReader(fileName));
         while ((line = reader.readLine()) != null) {
            if (line.indexOf(substring) > -1) {
               count++;
            }
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return count;
   }

   public static void writeObjectFile(String fileName, Object object) {
      writeObjectFile(new File(fileName), object);
   }

   public static void writeObjectFile(File file, Object object) {
      ObjectOutputStream oos = null;
      try {
         oos = new ObjectOutputStream(new FileOutputStream(file));
         oos.writeObject(object);
         oos.flush();
         oos.close();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         if (oos != null) {
            try {
               oos.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
   }

   public static boolean isArchive(String filePath) {
      return isArchive(new File(filePath));
   }

   public static boolean isArchive(File file) {
      String n = file.getName();
      if (n.endsWith(".zip")
         || n.endsWith(".tar")
         || n.endsWith("tar.gz")
         || n.endsWith(".tgz")
         || n.endsWith(".Z")
         || n.endsWith(".jar")
         || n.endsWith(".ear")
         || n.endsWith(".war")
         || n.endsWith(".bzip2")) {
         return true;
      }
      return false;
   }


   public static boolean isZip(InputStream stream) {
      boolean rtrn = false;

      if (stream.markSupported()) {
         stream.mark(10);
      }
      try {
         for (int i = 0; i < 10; i++) {
            int val = stream.read();
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      if (stream.markSupported()) {
         try {
            stream.reset();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }


      return rtrn;
   }
}
