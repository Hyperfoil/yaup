package perf.yaup.yaml;

import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.yaml.OverloadConstructor;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class OverloadConstructorTest {

    static Yaml yaml = null;

    @BeforeClass
    public static void setupYaml(){
        OverloadConstructor constructor = new  OverloadConstructor();
        yaml = new Yaml(constructor);
    }



    @Test @Ignore
    public void pattern_in_mapping(){
        OverloadConstructor constructor = new  OverloadConstructor();
        Resolver resolver = new Resolver(){

            @Override
            public Tag resolve(NodeId kind, String value, boolean implicit) {
                Tag resolved = super.resolve(kind,value,implicit);
                return resolved;
            }
        };
        resolver.addImplicitResolver(new Tag("pattern"),Pattern.compile("\u200B\u200C.*?\u200C\u200B"),"\u200B");
        resolver.addImplicitResolver(new Tag("pattern"),Pattern.compile("$\\{\\{.*?}}"),"$");
        constructor.addConstruct(new Tag("pattern"), new Construct() {
            @Override
            public Object construct(Node node) {
                if(node instanceof ScalarNode){
                    String value= ((ScalarNode)node).getValue();//it doesn't strip the surrounding values, need to re-convert them
                    return new StringUtil.PatternRef(value,"${{","}}");
                }
                return 42;
            }
            @Override
            public void construct2ndStep(Node node, Object o) {

            }
        });
        StreamReader sreader = new StreamReader("foo: ${{ref}}");
        Parser parser = new ParserImpl(sreader);
        Composer composer = new Composer(parser, resolver);

        //Node n = yaml.compose(new StringReader("key: { foo: ${{ref}} }"));
        //Object obj = yaml.load("{ foo: ${{ref}} }");
        constructor.setComposer(composer);
        Object obj = constructor.getSingleData(Object.class);
        assertTrue(obj instanceof Map);
        Map map = (Map)obj;
        assertTrue("map should contain foo "+map.keySet(),map.containsKey("foo"));
        Object foo = map.get("foo");
        //foo is a String, but we want it to auto-box to 42
    }
    @Test @Ignore
    public void pattern_in_flow_mapping(){
        OverloadConstructor constructor = new  OverloadConstructor();
        Resolver resolver = new Resolver(){

            @Override
            public Tag resolve(NodeId kind, String value, boolean implicit) {
                Tag resolved = super.resolve(kind,value,implicit);
                return resolved;
            }
        };
        resolver.addImplicitResolver(new Tag("pattern"), Pattern.compile("\\$"),"$",Integer.MAX_VALUE);


        yaml = new Yaml(constructor,new Representer(), new DumperOptions(),resolver);

        StreamReader sreader = new StreamReader("{ foo: ${{ref}} }");
        Parser parser = new ParserImpl(sreader);
        Composer composer = new Composer(parser, resolver);

        //Node n = yaml.compose(new StringReader("key: { foo: ${{ref}} }"));
        //Object obj = yaml.load("{ foo: ${{ref}} }");
        constructor.setComposer(composer);
        Object obj = constructor.getSingleData(Object.class);

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
