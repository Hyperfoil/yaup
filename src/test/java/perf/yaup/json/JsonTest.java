package perf.yaup.json;

import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.vertx.core.json.JsonObject;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class JsonTest {

    @Test
    public void isJsonSearchPath(){
        assertTrue("search should be jsonpath",
                Json.isJsonSearchPath("$.foo[?(bar)]"));
        assertTrue("path tree search should match",
                Json.isJsonSearchPath("$.foo..bar"));

        assertFalse("array spread is not the same as json path tree search",
                Json.isJsonSearchPath("${{=[ \"${{hostname}}\" , ...${{RUN.FOO:[]}} ]}}")
        );
    }

    @Test
    public void fromYaml_emptyKey(){
        Json json = Json.fromYaml("foo:\n  bar:");
        assertTrue(json.has("foo"));
        Object foo = json.get("foo");
        assertTrue("foo should be json",foo instanceof Json);
        Json fooJson = (Json)foo;
        assertTrue("foo.bar should exist",fooJson.has("bar"));
        assertNull("foo.bar should be null",fooJson.get("bar"));

    }
    @Test
    public void has_array_string_index(){
        Json json = new Json();
        json.add("foo");
        assertTrue("json should have ['0']",json.has("0"));
        assertTrue("json should have [0]",json.has(0));
    }

    @Test
    public void has_map_integer_key(){
        Json json = Json.fromString("{\"10\":\"found\"}");
        assertTrue("json should have ['10'] "+json,json.has("10"));
        assertTrue("json should have [10] "+json,json.has(10));
    }
    @Test
    public void has_map_integer_key_from_set(){
        Json json = new Json();
        json.set(10,"found");
        assertTrue("json should have ['10'] "+json,json.has("10"));
        assertTrue("json should have [10] "+json,json.has(10));
    }

    @Test
    public void toChain_doubleQuote_key(){
        List<String> chain = Json.toChain("a.\"1.2.3\".b");
        assertEquals("expect 3 entries: "+chain,3,chain.size());
        assertEquals("incorrect chain[0]","a",chain.get(0));
        assertEquals("incorrect chain[1]","1.2.3",chain.get(1));
        assertEquals("incorrect chain[2]","b",chain.get(2));
    }
    @Test
    public void toChain_singleQuote_key(){
        List<String> chain = Json.toChain("a.'1.2.3'.b");
        assertEquals("expect 3 entries: "+chain,3,chain.size());
        assertEquals("incorrect chain[0]","a",chain.get(0));
        assertEquals("incorrect chain[1]","1.2.3",chain.get(1));
        assertEquals("incorrect chain[2]","b",chain.get(2));
    }
    @Test
    public void bracket_key(){
        List<String> chain = Json.toChain("a[1.2.3].b");
        assertEquals("incorrect chain[0]","a",chain.get(0));
        assertEquals("incorrect chain[1]","1.2.3",chain.get(1));
        assertEquals("incorrect chain[2]","b",chain.get(2));
    }

    @Test
    public void toChain_no_dots(){
        List<String> chain = Json.toChain("a");
        assertEquals("expect 1 entry: "+chain,1,chain.size());
        assertEquals("incorrect chain[0]","a",chain.get(0));
    }
    @Test
    public void toChain_separate_dots(){
        List<String> chain = Json.toChain("a.b.c");
        assertEquals("expect 3 entries: "+chain,3,chain.size());
        assertEquals("incorrect chain[0]","a",chain.get(0));
        assertEquals("incorrect chain[1]","b",chain.get(1));
        assertEquals("incorrect chain[2]","c",chain.get(2));
    }
    @Test
    public void toChain_skip_slash_dot(){
        List<String> chain = Json.toChain("a\\.b.c");
        assertEquals("expect 2 entries: "+chain,2,chain.size());
        assertEquals("incorrect chain[0]","a\\.b",chain.get(0));
        assertEquals("incorrect chain[1]","c",chain.get(1));
    }

    @Test
    public void toChain_array_reference(){
        List<String> chain = Json.toChain("v[0]");
        assertEquals("expect 2 entries: "+chain,2,chain.size());
        assertEquals("incorrect chain[0]","v",chain.get(0));
        assertEquals("incorrect chain[1]","0",chain.get(1));
    }

    @Test
    public void fromString_empty_object(){
        Json json = Json.fromString("{}");
        assertNotNull(json);
        assertFalse("json should not be an array",json.isArray());

        json = Json.fromString("{\"foo\":{}}");
        assertNotNull(json);
        assertTrue("json.foo should exist and be a json",json.has("foo") && json.get("foo") instanceof Json);
        Json foo = json.getJson("foo");
        assertFalse("json.foo should not be an array",foo.isArray());
    }

    @Test
    public void find_exp_InvalidPathException(){
        Json target = Json.fromJs("{one:[{name:'foo',value:'bar'}]}");
        Object found = Json.find(target,"$. (2*FOO)+'m' ");
        assertNull("should not throw an exception or find the path",found);
    }

    @Test
    public void find_single_value_search_expression(){
        Json target = Json.fromJs("{one:[{name:'foo',value:'bar'}]}");
        Object found = Json.find(target,"$.one[?(@.name=='foo')].value");
        assertTrue("found should be a string but was: "+found,found instanceof String);
        assertEquals("should find bar","bar",found);
    }
    @Test
    public void find_single_value_search_deepScan(){
        Json target = Json.fromJs("{one:[{name:'foo',value:'bar'}]}");
        Object found = Json.find(target,"$.one..value");
        assertTrue("found should be a string but was: "+found,found instanceof String);
        assertEquals("should find bar","bar",found);
    }

    @Test
    public void find_key_chain(){
        Json target = Json.fromJs("{FOO:{biz:{buz:'one'}}}");
        Object found = Json.find(target,"$.FOO.biz.buz");
        assertNotNull(found);
    }

    //the Array.prototype.push method is not threadsafe in graaljs (because javascript objects are not sharable)
    @Test @Ignore
    public void array_add_concurrency(){
        int limit = 10;
        int concurrency = 1;
        CountDownLatch start = new CountDownLatch(concurrency);
        CountDownLatch stop = new CountDownLatch(concurrency);


        Json json = new Json(true);
        Runnable r = ()->{
            start.countDown();
            try {
                start.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for(int i=0;i<limit; i++){
                StringUtil.jsEval("(v,i)=>{v.push(i)}", Arrays.asList("Array.prototype.push=(v,a,b,c)=>{for (var key in this) {\n" +
                        "    print(key);\n" +
                        "}console.log(v,this);}"),json,i);
                //json.add(i);
            }
            stop.countDown();
        };
        for(int i=0; i < concurrency; i++){
            Thread t = new Thread(r);
            t.start();
        }
        try {
            stop.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(json.size());
        assertEquals("wrong number of entries",limit*concurrency,json.size());
    }
    @Test
    public void chainSet_array_reference(){
        Json root = Json.fromString("{\"v\":[{},{}]}");
        Json.chainSet(root,"v[0].uno","one");

        Json first = root.getJson("v");
        assertNotNull("v should exist",first);
        first = first.getJson(0);
        assertNotNull("v[0] should exist",first);
        assertTrue("v[0].uno should exist: "+first.toString(),first.has("uno"));

    }

    @Test
    public void chainSet_existing(){
        Json root = new Json();
        root.set("first","uno");
        Json.chainSet(root,"first","dos");
    }

    @Test
    public void chainSet_path_with_number_array_value(){
        Json root = new Json();
        Json.chainSet(root,"config-quickstart.JVM.10.foo",new Json(true));

        Json target = null;
        target = root.getJson("config-quickstart");
        assertNotNull(target);
        target = target.getJson("JVM");
        assertNotNull(target);
        target = target.getJson("10");
        assertNotNull(target);
        target = target.getJson("foo");
        assertNotNull(target);
        assertTrue("foo should be an array "+target,target.isArray());
    }

    @Test
    public void chainSet_path_with_number(){
        Json root = new Json();
        Json.chainSet(root,"first.second.10.bar",10);
        Json target = null;
        target = root.getJson("first");
        assertNotNull(target);
        target = target.getJson("second");

        assertNotNull(target);
        Json second = target;
        assertFalse("first.second should not be an array",target.isArray());
        assertEquals("first.second should have 1 entry: "+target,1,target.size());
        assertTrue("first.second should have '10' as a key",target.has("10"));
        target = second.getJson("10");
        assertNotNull("first.second.`10` should exist",target);
        assertTrue("first.second should have 10 as a key",second.has(10));
        target = second.getJson(10);
        assertNotNull("first.second.10 should exist",target);
    }
    @Test
    public void chainSet_new_array(){
        Json root = new Json();
        Json.chainSet(root,"first.second.0.bar",10);
        Json target = null;
        target = root.getJson("first");
        assertNotNull(target);
        target = target.getJson("second");
        assertNotNull(target);
        assertTrue("first.second should be an array",target.isArray());
        assertTrue("first.second should have '0' as a key",target.has("0"));
        assertTrue("first.second should have 0 as a key",target.has(0));
    }

    @Test
    public void chainSet_existingPath(){
        Json root = new Json();
        root.set("first",new Json());
        Json second = new Json();
        root.getJson("first").set("second",second);

        Json.chainSet(root,"first.second.third",10);

        assertTrue("second should have 1 entry",second.size()==1);
        assertTrue("second[third] should exist",second.has("third"));
        assertEquals("second[third] should be 10",10, second.get("third"));

        Json.chainSet(root,"first.second.fourth",20);
        assertTrue("second[fourth] should exist\n"+second.toString(2),second.has("fourth"));
        assertEquals("second[fourth] should be 20",20, second.get("fourth"));
    }

    @Test
    public void new_jsonobject(){
        Json event = Json.fromString("{'foo':'bar'}");
        JsonObject converted = new JsonObject(event.toString());
        assertNotNull(converted);
    }

    @Test
    public void string_escape_tab(){
        Json event = new Json();
        event.add("foo","bar\tbiz");
        try {
            JsonObject test = new JsonObject(event.toString());
        }catch(Exception e){
            fail(e.getMessage());
        }
    }
    @Test
    public void string_escape_bash(){

    }
    @Test
    public void fromJs_oc_array(){
        String input="" +
           "[\n" +
           "  {\n" +
           "    \"isNull\": null,\n" +
           "    \"isUndefined\": undefined,\n" +
           "  }\n" +
           "]";
        try {
            Json json = Json.fromJs(input);
            assertFalse("json should not be null",json == null);
            assertTrue("json should be an array",json.isArray());
            assertEquals("json should have 1 entry",1,json.size());
            assertTrue("json[0] should be json",json.get(0) instanceof Json);
        }catch(Exception e){
            fail(e.getMessage());
        }
    }

    @Test @Ignore //TODO Graaljs does not yet support on demand bindings like Nashorn
    public void fromJs_array_new_bindings(){
        Json json = Json.fromJs("[one,2,three]");
        assertTrue("json is an array",json.isArray());
        assertEquals("json.size ==3",3,json.size());
    }

    @Test @Ignore //TODO Graaljs does not yet support on demand bindings like Nashorn
    public void fromJs_map_new_bindings(){
        Json json = Json.fromJs("{key:value}");
        assertTrue("json is a map",!json.isArray());
        assertEquals("json.size ==1",1,json.size());
        assertEquals("json[key]=value","value",json.get("key"));
    }
    @Test
    public void fromJs_array_of_objects(){
        Json json = Json.fromJs("[{value:'one'},{value:'two'},{value:'three'}]");
        assertTrue("json !=null",json != null);
        assertTrue("json is an array\n"+json.toString(2),json.isArray());
        assertEquals("json.size() == 3\n"+json.toString(2),3,json.size());
        assertTrue("json[0] should be json",json.get(0) instanceof Json);
    }

    @Test
    public void toString_quotesInValue(){
        Json json = new Json();
        json.set("key","\"quoted\" value");
        try {
            new JSONObject(json.toString());
        }catch(Exception e){
            Assert.fail("failed to parse with JSONObject");
        }
    }

    @Test
    public void toString_escapedKeys(){
        Json json = new Json();
        json.set(1,1);
        json.set(2,2);
        try {
            assertEquals("{\"1\":1,\"2\":2}", json.toString());
        }catch(Exception e){
            Assert.fail("failed to parse with JSONObject");
        }
    }
    @Test
    public void equals(){
        Json a = new Json();
        a.add("key","foo");
        a.add("value","bar");
        a.add("child",new Json());
        a.getJson("child").add(new Json());
        a.getJson("child").add(new Json());

        a.getJson("child").getJson(0).add(new Json());
        a.getJson("child").getJson(0).getJson(0).add("key","a");
        a.getJson("child").getJson(0).getJson(0).add("value","Alpha");
        a.getJson("child").getJson(0).add(new Json());
        a.getJson("child").getJson(0).getJson(1).add("key","b");
        a.getJson("child").getJson(0).getJson(1).add("value","Bravo");
        a.getJson("child").getJson(1).add(new Json());
        a.getJson("child").getJson(1).getJson(0).add("key","y");
        a.getJson("child").getJson(1).getJson(0).add("value","Yankee");
        a.getJson("child").getJson(1).add(new Json());
        a.getJson("child").getJson(1).getJson(1).add("key","Z");
        a.getJson("child").getJson(1).getJson(1).add("value","Zulu");


        Json b = new Json();
        b.add("child",new Json());
        b.getJson("child").add(new Json());
        b.getJson("child").add(new Json());
        b.add("value","bar");
        b.add("key","foo");

        b.getJson("child").getJson(0).add(new Json());
        b.getJson("child").getJson(0).getJson(0).add("value","Alpha");
        b.getJson("child").getJson(0).getJson(0).add("key","a");
        b.getJson("child").getJson(0).add(new Json());
        b.getJson("child").getJson(0).getJson(1).add("value","Bravo");
        b.getJson("child").getJson(0).getJson(1).add("key","b");


        b.getJson("child").getJson(1).add(new Json());
        b.getJson("child").getJson(1).getJson(0).add("value","Yankee");
        b.getJson("child").getJson(1).getJson(0).add("key","y");

        b.getJson("child").getJson(1).add(new Json());
        b.getJson("child").getJson(1).getJson(1).add("value","Zulu");
        b.getJson("child").getJson(1).getJson(1).add("key","Z");


        assertTrue("a json should be equal to itself",a.equals(a));
        assertTrue("a should equal b",a.equals(b));
        assertTrue("b should equal a",b.equals(a));

    }

    @Test
    public void fromString(){
        Json expected = Json.fromString("[{\"comment\":\"comment1\"},{\"comment\":\"comment2\"},{\"key\":\"0Level1\"},{\"value\":\"hasValue\",\"key\":\"0Level1\",\"child\":[[{\"value\":\"normal\",\"key\":\"1\"},{\"value\":\"quoted[{]}\\\\\\\"Value\",\"key\":\"1\",\"child\":[[{\"value\":\"one\",\"key\":\"1.1\"},{\"value\":\"Alpha\",\"key\":\"1.1.a\"},{\"value\":\"Bravo\",\"key\":\"1.1.b\"}],[{\"value\":\"two\",\"key\":\"1.2\"},{\"value\":\"Yankee\",\"key\":\"1.2.y\"}],[{\"value\":\"Zulu\",\"key\":\"1.2.Z\"}]]}]]},{\"comment\":\"inlineComment\",\"key\":\"0Level2\",\"child\":[[{\"key\":\"1\",\"child\":[[{\"key\":\"first\"},{\"key\":\"second\"},{\"key\":\"quoted\\\\\\\" :,[{]\"},{\"key\":\"other\"},[[{\"key\":\"subOne\"},{\"key\":\"subTwo\"}]],[[{\"value\":\"subValue\",\"key\":\"subKey\"}]],{\"key\":\"zed\"}]]},{\"value\":\"bar\",\"key\":\"2\"}]]}]\n");
        assertNotNull(expected);
        assertTrue("should be an array",expected.isArray());
        assertTrue("should have entires",expected.size()>0);
    }

    @Test @Ignore
    public void indent(){
        Json json = new Json();
        json.set("a",new Json());
        json.getJson("a").set("aa",new Json());
        json.getJson("a").getJson("aa").set("value","foo");
        json.set("b",new Json());
        json.getJson("b").add("foo");
        json.getJson("b").add("bar");


    }
}
