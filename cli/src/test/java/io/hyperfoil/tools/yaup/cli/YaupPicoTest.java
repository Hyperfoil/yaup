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
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusMainTest
public class YaupPicoTest {

    @Test
    public void no_arg_help(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch();

        assertNotEquals(0,result.exitCode());
        assertTrue(result.getErrorOutput().contains("Usage:"));
    }

    @Test
    public void jsonDiff_help(QuarkusMainLauncher launcher) {
        CommandLine commandLine = new CommandLine(new YaupPico());
        LaunchResult result = launcher.launch("json-diff","-help");
        System.out.println(result.getOutput());
        System.out.println(result.getErrorOutput());
        assertNotEquals(0,result.exitCode());
        System.out.println("out\n"+result.getOutput());
        System.out.println("err\n"+result.getErrorOutput());
        assertTrue(result.getErrorOutput().contains("Missing required parameters"),result.getErrorOutput()+result.getOutput());
        assertTrue(result.getErrorOutput().contains("Usage:"));
    }

    /**
     * Ensuring xml operations work
     * @param launcher
     * @throws IOException
     */
    @Test
    public void xml_get(QuarkusMainLauncher launcher) throws IOException {
        Path filePath = Files.writeString(Files.createTempFile("yaup",".xml").toAbsolutePath(),
                """
                <foo>
                    <bar><biz>one</biz><buz>two</buz></bar>
                    <bar><biz>uno</biz><buz>dos</buz></bar>
                </foo>
                """
        );
        filePath.toFile().deleteOnExit();
        LaunchResult result = launcher.launch("xml","/foo/bar[0]/buz/text()",filePath.toString());
        assertEquals(0,result.exitCode(),"execution should return 0");
        assertEquals("two",result.getOutput().trim(),"unexpected output:\n"+result.getOutput());
        assertTrue(result.getErrorOutput().isEmpty(),"unexpected errors:\n"+result.getErrorOutput());
    }
}
