package perf.yaup.file;

import io.hyperfoil.tools.yaup.file.FileUtility;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FileUtilityTest {

   private ArchiveOutputStream getArchiveOutputStream(String suffix, OutputStream stream) throws IOException {
      ArchiveOutputStream rtrn = null;
      suffix = suffix.toString();
      if(suffix.equals(".tar.gz") || suffix.equals(".tgz")){
         rtrn = new TarArchiveOutputStream(new GzipCompressorOutputStream(stream));
      }else if (suffix.equals(".tar")){
         rtrn = new TarArchiveOutputStream(stream);
      }else if (suffix.equals(".zip")){
         rtrn = new ZipArchiveOutputStream(stream);
      }else if (suffix.equals(".jar")){
         rtrn = new JarArchiveOutputStream(stream);
      }
      return rtrn;
   }
   private String makeArchive(String suffix,String...entries){
      try {
         File f = Files.createTempDirectory("yaup").toFile();
         f.deleteOnExit();
         return makeArchive(f, suffix, entries);
      }catch(IOException e){
         e.printStackTrace();
      }
      return null;
   }
   private String makeArchive(File parent,String suffix,String ...entries) {
      Map<String,String> map = new HashMap<>();
      for(String entry : entries){
         map.put(entry,entry);
      }
      return makeArchive(parent,suffix,map);
   }
   private String makeArchive(File parent, String suffix, Map<String,String> entries){
      File f = new File(parent,""+System.currentTimeMillis()+suffix);
      f.deleteOnExit();
      Set<String> createdPaths = new HashSet<>();
      List<String> paths = entries.keySet().stream().sorted().collect(Collectors.toList());
      String rtrn = null;
      try {

         //f.deleteOnExit();
         FileOutputStream fileOutputStream = new FileOutputStream(f);
         BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
         ArchiveOutputStream archiveOutputStream = getArchiveOutputStream(suffix,bufferedOutputStream);

         for(String path : paths){
            File tmp = File.createTempFile("yaup-archive","");
            FileUtility.writeObjectFile(tmp,entries.get(path));
            tmp.deleteOnExit();
            ArchiveEntry entry = archiveOutputStream.createArchiveEntry(tmp,path);
            archiveOutputStream.putArchiveEntry(entry);
            if(!path.endsWith("/")) {
               try (InputStream i = Files.newInputStream(tmp.toPath())) {
                  IOUtils.copy(i, archiveOutputStream);
               }
            }
            archiveOutputStream.closeArchiveEntry();
         }
         archiveOutputStream.flush();
         archiveOutputStream.finish();
         archiveOutputStream.close();
         bufferedOutputStream.flush();
         bufferedOutputStream.close();
         fileOutputStream.flush();
         fileOutputStream.close();

         rtrn = f.getPath();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      return rtrn;
   }

   Path testZipPath = Paths.get(
      FileUtilityTest.class.getProtectionDomain().getCodeSource().getLocation().getPath()
   ).resolve(
      Paths.get("test.zip")
   );

   @Test
   public void getFile_in_archive(){
      File f = FileUtility.getFile(testZipPath.toString()+"#foo.txt",true);
      assertTrue("file should exist",f.exists());
      assertTrue("file name should start with foo.txt: "+f.getName(),f.getName().startsWith("foo.txt"));
   }

   @Test
   public void getArchiveEntries_zip(){
      String path = makeArchive(".zip","bar/foo.txt","bar/biz/bar.txt","biz.txt");
      File f = new File(path);
      List<String> entries = FileUtility.getArchiveEntries(path);
      assertEquals("should find 3 entries: "+entries,3,entries.size());
   }

   @Test
   public void getFiles_recurse_in_zip(){
      try {
         Path parent = Files.createTempDirectory("yaup");
         String pathZip = makeArchive(parent.toFile(),".zip","bar/foo.txt","bar/biz/bar.txt","biz.txt");
         List<String> entries = FileUtility.getFiles(parent.toString(),"",true,true);
         assertEquals("incorrect number of entries",4,entries.size());
         assertTrue("entries should start with zip path: "+entries,entries.contains(pathZip+FileUtility.ARCHIVE_KEY+"bar/foo.txt"));
         assertTrue("entries should start with zip path: "+entries,entries.contains(pathZip+FileUtility.ARCHIVE_KEY+"bar/biz/bar.txt"));
         assertTrue("entries should start with zip path: "+entries,entries.contains(pathZip+FileUtility.ARCHIVE_KEY+"biz.txt"));
      } catch (IOException e) {
         fail(e.getMessage());
      }
   }

   @Test
   public void getFiles_in_one_archive(){
      String pathZip = makeArchive(".zip","bar/foo.txt","bar/biz/bar.txt","biz.txt");
      List<String> files = FileUtility.getFiles(pathZip,"",true,true);
      assertEquals("incorrect number of files "+files,3,files.size());
   }
}
