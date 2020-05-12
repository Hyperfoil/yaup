package perf.yaup.file;

import io.hyperfoil.tools.yaup.file.FileUtility;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class FileUtilityTest {

   Path testTarPath = Paths.get(
      FileUtilityTest.class.getProtectionDomain().getCodeSource().getLocation().getPath()
   ).resolve(
      Paths.get("test.tar.gz")
   );

   Path testZipPath = Paths.get(
      FileUtilityTest.class.getProtectionDomain().getCodeSource().getLocation().getPath()
   ).resolve(
      Paths.get("test.zip")
   );

   @Test
   public void getFile(){
      File f = FileUtility.getFile(testZipPath.toString()+"#foo.txt",true);
      assertTrue("file should exist",f.exists());
      assertTrue("file name should start with foo.txt: "+f.getName(),f.getName().startsWith("foo.txt"));
   }
}
