package perf.yaup.json;

import io.hyperfoil.tools.yaup.json.Json;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonTest {


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

    @Test
    public void chainSet_existing(){
        Json root = new Json();
        root.set("first","uno");
        Json.chainSet(root,"first","dos");
    }

    @Test
    public void chainSet_existingPath(){
        Json root = new Json();
        root.set("first",new Json());
        Json second = new Json();
        root.getJson("first").set("second",second);

        Json.chainSet(root,"first.second.third",10);

        assertTrue("second shoudl have 1 entry",second.size()==1);
        assertTrue("second[third] should exist",second.has("third"));
        assertEquals("second[third] should be 10",10, second.get("third"));

        Json.chainSet(root,"first.second.fourth",20);
        assertTrue("second[fourth] should exist\n"+second.toString(2),second.has("fourth"));
        assertEquals("second[fourth] should be 20",20, second.get("fourth"));


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
