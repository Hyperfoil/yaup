package perf.yaup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

public class YaupIT {


    public record Output(String out,String err,int exitCode){}

    public static Output run(String...args) throws IOException, InterruptedException{
        String jarPath = System.getProperty("uberJarPath","");
        if(jarPath.isEmpty()){
            return new Output("","",666);
        }
        ProcessBuilder builder = new ProcessBuilder();
        List<String> argList = new ArrayList<>();
        argList.addAll(Arrays.asList("java","-jar",jarPath));
        argList.addAll(Arrays.asList(args));
        
        builder.command(argList);
        Process process = builder.start();
         final InputStream inputStream = process.getInputStream();
         //final OutputStream outputStream = process.getOutputStream();
         final InputStream errorStream = process.getErrorStream();
         int result = process.waitFor();
         String line = null;
         BufferedReader reader = null;
         reader = new BufferedReader(new InputStreamReader(errorStream));
         StringBuilder errBuilder = new StringBuilder();
         while ((line = reader.readLine()) != null) {
            if(errBuilder.length()>0){
                errBuilder.append(System.lineSeparator());
            }
            errBuilder.append(line);
         }
         reader = new BufferedReader(new InputStreamReader(inputStream));
         StringBuilder outBuilder = new StringBuilder();
         while ((line = reader.readLine()) != null) {
            if(outBuilder.length()>0){
                outBuilder.append(System.lineSeparator());
            }
            outBuilder.append(line);
         }
         return new Output(outBuilder.toString(), errBuilder.toString(), result);
    }
    public static String tmpFile(String content) throws IOException{
        File f = File.createTempFile("yaup","tmp");
        f.deleteOnExit();
        Files.write(f.toPath(),content.getBytes());
        return f.getAbsolutePath();
    }

    @Test @Ignore
    public void default_populatePattern_jsEval_addition() throws IOException, InterruptedException{
            Output r = run("${{=1+2}}");
            assertEquals("unexpected error: "+r, "",r.err());
            assertTrue("expected the sum on last line: "+r,r.out().endsWith("3"));
    }

    @Test @Ignore
    public void structure() throws IOException, InterruptedException{
        Output r = run("structure",
            tmpFile("""
                { "foo":"bar"}
            """)
        );
        assertEquals("unexpected exit code: "+r,0,r.exitCode());
        assertEquals("unexpected output: "+r, "{\"foo\": \"string\"}",r.out());

    }


}
