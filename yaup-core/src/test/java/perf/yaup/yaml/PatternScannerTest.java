package perf.yaup.yaml;

import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.yaml.DeferableConstruct;
import io.hyperfoil.tools.yaup.yaml.OverloadConstructor;
import io.hyperfoil.tools.yaup.yaml.PatternScanner;
import io.hyperfoil.tools.yaup.yaml.PatternTemplate;
import org.junit.Ignore;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
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
import org.yaml.snakeyaml.scanner.Scanner;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PatternScannerTest {


    public Object read(String input,Class clazz){
        OverloadConstructor constructor = new  OverloadConstructor();
        Resolver resolver = new Resolver();
        resolver.addImplicitResolver(new Tag("pattern"), Pattern.compile("^\\$\\{\\{.*?}}"),"$");
        constructor.addConstruct(new Tag("pattern"), new PatternTemplate.Construct());
        LoaderOptions loaderOptions = new LoaderOptions();
        StreamReader sreader = new StreamReader(input);
        Scanner scanner = new PatternScanner(sreader,loaderOptions);
        Parser parser = new ParserImpl(scanner);
        Composer composer = new Composer(parser, resolver,loaderOptions);
        constructor.setComposer(composer);
        Object obj = constructor.getSingleData(clazz);
        return obj;
    }

    @Test
    public void pattern_in_mapping(){
        Object obj = read("foo: ${{ref}}",Object.class);
        assertTrue(obj instanceof Map);
        Map map = (Map)obj;
        assertTrue("map should contain foo "+map.keySet(),map.containsKey("foo"));
        Object foo = map.get("foo");
        assertTrue(foo instanceof PatternTemplate);
        PatternTemplate patternTemplate = (PatternTemplate)foo;
        assertEquals("${{ref}}",patternTemplate.getTemplate());
    }


    @Test
    public void pattern_in_flow_mapping(){
        Object obj = read("{foo: ${{=${{ref:[]}}.map((k)=>k.boo():def}} }",Object.class);
        assertTrue(obj instanceof Map);
        Map map = (Map)obj;
        assertTrue("map should contain foo "+map.keySet(),map.containsKey("foo"));
        Object foo = map.get("foo");
        assertTrue("foo not expected class type:"+foo+" "+foo.getClass(),foo instanceof PatternTemplate);
        PatternTemplate patternTemplate = (PatternTemplate)foo;
        assertEquals("${{=${{ref:[]}}.map((k)=>k.boo():def}}",patternTemplate.getTemplate());
    }

    @Test
    public void two_patterns_in_flow_sequence(){
        Object obj = read("[${{foo}},${{bar}}]",Object.class);
        assertTrue(obj instanceof List);
        List list = (List)obj;
        assertEquals("expect 2 entries:"+list,2,list.size());
        Object first = list.get(0);
        assertTrue("first entry should be a PatternTemplate: "+first,first instanceof PatternTemplate);
        PatternTemplate pt = (PatternTemplate) first;
        assertEquals("${{foo}}",pt.getTemplate());
        Object second = list.get(1);
        assertTrue("second entry should be a PatternTemplate: "+second,second instanceof PatternTemplate);
        pt = (PatternTemplate) second;
        assertEquals("${{bar}}",pt.getTemplate());

    }

}


