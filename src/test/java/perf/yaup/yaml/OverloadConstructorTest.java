package perf.yaup.yaml;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.yaml.OverloadConstructor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.StringReader;

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
