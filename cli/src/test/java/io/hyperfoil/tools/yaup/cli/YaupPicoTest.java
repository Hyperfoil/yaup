package io.hyperfoil.tools.yaup.cli;

import io.hyperfoil.tools.yaup.AsciiArt;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusMainTest
public class YaupPicoTest {

    public String createFiles(Map<String,String> files) throws IOException {
        Path dir = Files.createTempDirectory("yaup");
        dir.toFile().deleteOnExit();

        files.forEach((name,content)->{
            String fileName = name;
            File parentDir = dir.toFile();
            if(name.contains(File.separator)){
                File parent = new File(parentDir,name.substring(0,name.lastIndexOf(File.separator)));
                fileName = name.substring(name.lastIndexOf(File.separator));
                parent.mkdirs();
                parentDir = parent;
            }
            File contentFile = new File(parentDir,fileName);
            try {
                contentFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });

        return null;
    }


    @Test
    public void no_arg_help(QuarkusMainLauncher launcher) {
        CommandLine commandLine = new CommandLine(new YaupPico());
        LaunchResult result = launcher.launch();
        assertNotEquals(0,result.exitCode());
        assertTrue(result.getErrorOutput().contains("Usage:"));
    }

    @Test
    public void no_jsonDiff_help(QuarkusMainLauncher launcher) {
        CommandLine commandLine = new CommandLine(new YaupPico());
        LaunchResult result = launcher.launch("json-diff","-help");
        assertNotEquals(0,result.exitCode());
        assertTrue(result.getErrorOutput().contains("Missing required parameters"));
        assertTrue(result.getErrorOutput().contains("Usage:"));
    }

}
