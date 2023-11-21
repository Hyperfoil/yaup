package perf.yaup.yaml;

import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.yaml.OverloadConstructor;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.scanner.Scanner;
import org.yaml.snakeyaml.scanner.ScannerImpl;
import org.yaml.snakeyaml.tokens.FlowMappingEndToken;
import org.yaml.snakeyaml.tokens.FlowMappingStartToken;
import org.yaml.snakeyaml.tokens.ScalarToken;
import org.yaml.snakeyaml.tokens.Token;

import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class OverloadConstructorTest {

    static Yaml yaml = null;

    @BeforeClass
    public static void setupYaml(){
        OverloadConstructor constructor = new  OverloadConstructor();
        yaml = new Yaml(constructor);
    }

    @Test
    public void json_value_object(){
        OverloadConstructor constructor = new  OverloadConstructor();
        Node n = yaml.compose(new StringReader("key: {}"));
        Json json = OverloadConstructor.json(n);
        assertTrue("json[key] should exist",json.has("key"));
        Object key = json.get("key");
        assertTrue("key should be json",key instanceof Json);
        Json asJson = (Json)key;
        assertFalse("key should be an object, not array",asJson.isArray());

    }
    @Test
    public void json_string_value_object(){
        OverloadConstructor constructor = new  OverloadConstructor();
        Node n = yaml.compose(new StringReader("key: \"0.20\""));
        Json json = OverloadConstructor.json(n);
        assertTrue("json[key] should exist",json.has("key"));
        Object value = json.get("key");
        assertFalse("json[key] should be a number: "+value.getClass(),value instanceof Number);
    }
    @Test
    public void json_number_value_object(){
        OverloadConstructor constructor = new  OverloadConstructor();
        Node n = yaml.compose(new StringReader("key: 0.20"));
        Json json = OverloadConstructor.json(n);
        assertTrue("json[key] should exist",json.has("key"));
        Object value = json.get("key");
        assertTrue("json[key] should be a number: "+value.getClass(),value instanceof Number);
    }
    @Test
    public void json_multiline(){
        OverloadConstructor constructor = new  OverloadConstructor();
        Node n = yaml.compose(new StringReader("key: >\n foo\n bar"));
        Json json = OverloadConstructor.json(n);
        assertTrue("json[key] should exist",json.has("key"));
        Object value = json.get("key");
        assertTrue("json[key] should be a string"+value.getClass(),value instanceof String);
        assertEquals("json[key] value check","foo bar",value.toString());
    }
    @Test
    public void json_multiline_spaced(){
        OverloadConstructor constructor = new  OverloadConstructor();
        Node n = yaml.compose(new StringReader("key: >\n foo\n\n bar"));
        Json json = OverloadConstructor.json(n);
        assertTrue("json[key] should exist",json.has("key"));
        Object value = json.get("key");
        assertTrue("json[key] should be a string"+value.getClass(),value instanceof String);
        assertEquals("json[key] value check","foo\nbar",value.toString());
    }

}
