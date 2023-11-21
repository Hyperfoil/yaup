package perf.yaup;


import io.hyperfoil.tools.yaup.HashedLists;
import io.hyperfoil.tools.yaup.PopulatePatternException;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.graaljs.JsException;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class StringUtilTest {

   static List<String> evals = Arrays.asList(
           "function milliseconds(v){ return Packages.io.hyperfoil.tools.yaup.StringUtil.parseToMs(v)}",
           "function seconds(v){ return Packages.io.hyperfoil.tools.yaup.StringUtil.parseToMs(v)/1000}",
           "function range(start,stop,step=1){ return Array(Math.ceil(Math.abs(stop - start) / step)).fill(start).map((x, y) => x + Math.ceil(Math.abs(stop - start) / (stop - start)) * y * step);}"
   );


   @Test
   public void jsEval_error_lambda_undefined_variable(){
      Map<Object,Object> globals = new HashMap<Object,Object>();
      String js = "(x)=>{\n  const bar = foo + \n    (foo);\n  return bar;\n}";
      try {
         Object result = StringUtil.jsEval(js, globals);
      }catch(JsException error){
         error.printStackTrace();
         assertTrue(error.getMessage().contains("ReferenceError"));
         assertTrue(error.getMessage().contains("foo"));
         assertEquals(js,error.getJs());
         return;
      }
      fail("expected JsException");
   }
   @Test
   public void jsEval_error_lambda_invalid_array_args(){
      Map<Object,Object> globals = new HashMap<Object,Object>();
      globals.put("min",1);
      globals.put("max",0);
      String js = "(min,max)=>{\nreturn Array.from(Array(max-min).keys())\n}";
      try {
         Object result = StringUtil.jsEval(js, 0,-1);
         System.out.println("result="+result);
      }catch(JsException error){
         error.printStackTrace();
         assertTrue(error.getMessage(),error.getMessage().contains("Invalid array length"));
         assertTrue(error.getMessage(),error.getMessage().contains("RangeError"));
         assertEquals(js,error.getJs());
         return;
      }
      fail("expected JsException");
   }
   @Test
   public void jsEval_error_async_lambda_undefined_variable_no_args(){
      String js = "async (x)=>{return foo;}";Object result = StringUtil.jsEval(js);
      assertTrue("unexpected result type: "+result,result instanceof JsException);
      JsException error = (JsException)result;
      assertTrue(error.getMessage().contains("ReferenceError"));
      assertTrue(error.getMessage().contains("foo"));
      assertEquals(js,error.getJs());
      assertTrue(error.hasErrorLocation());
      assertTrue(error.hasSource());
   }
   @Test
   public void jsEval_error_async_lambda_undefined_variable(){
      Map<Object,Object> args = new HashMap<Object,Object>();
      String js = "async (x)=>{return foo;}";
      Object result = StringUtil.jsEval(js,args);
      assertTrue("unexpected result type: "+result,result instanceof JsException);
      JsException error = (JsException)result;
      assertTrue(error.getMessage().contains("ReferenceError"));
      assertTrue(error.getMessage().contains("foo"));
      assertEquals(js,error.getJs());
      assertTrue(error.hasErrorLocation());
      assertTrue(error.hasSource());
   }

   @Test
   public void jsEval_global_json_member_math(){
      Map<Object,Object> globals = new HashMap<Object,Object>();
      globals.put("FOO",Json.fromString("{\"bar\":2}"));
      globals.put("BAR",2);
      Object result = StringUtil.jsEval("FOO.bar+BAR",globals,Collections.EMPTY_LIST);
      assertEquals("FOO should be found in global context",4L,result);
   }

   @Test
   public void jsEval_global_json_member(){
      Map<Object,Object> globals = new HashMap<Object,Object>();
      globals.put("FOO",Json.fromString("{\"bar\":\"biz\"}"));
      Object result = StringUtil.jsEval("FOO.bar+'buz'",globals,Collections.EMPTY_LIST);
      assertEquals("FOO should be found in global context","bizbuz",result);
   }

   @Test
   public void jsEval_global_string(){
      Map<Object,Object> globals = new HashMap<>();
      globals.put("FOO","one");
      Object result = StringUtil.jsEval("FOO+'two'",globals,Collections.EMPTY_LIST);
      assertEquals("FOO should be found in global context","onetwo",result);
   }
   @Test
   public void jsEval_global_number(){
      Map<Object,Object> globals = new HashMap<>();
      globals.put("FOO",1);
      Object result = StringUtil.jsEval("FOO+2",globals,Collections.EMPTY_LIST);
      assertEquals("FOO should be found in global context",3l,result);
   }

   @Test
   public void jsEval_function_return_null(){
      Object result = StringUtil.jsEval("function(a,b){return null;}","a","b");
      assertNull("result should be null",result);
   }
   @Test
   public void jsEval_function_no_return(){
      Object result = StringUtil.jsEval("function(a,b){}","a","b");
      assertNotNull("no result should not be null",result);
      assertTrue("result should be a string",result instanceof String);
      assertEquals("result should be an empty string","",result);
   }
   @Test
   public void jsEval_return_missing_key(){ //TODO should returning a missing key return null or empty string
      Object result = StringUtil.jsEval("(a)=>a.foo",Json.fromString("{\"notfoo\":\"bar\"}"));
      assertNotNull("no result should not be null",result);
      assertTrue("result should be a string",result instanceof String);
      assertEquals("result should be an empty string","",result);

      //assertNull("missing keys should return null as undefined",result);
   }


   @Test
   public void jsEval_function_return_json(){
      Object result = StringUtil.jsEval("function(a,b){return {a:'a',b:'b'};}","a","b");
      assertNotNull("result should not be null",result);
      assertTrue("result should be Json",result instanceof Json);
      Json json = (Json)result;
      assertTrue("json has a",json.has("a"));
      assertEquals("json a = a","a",json.getString("a"));
      assertTrue("json has b",json.has("b"));
      assertEquals("json b = b","b",json.getString("b"));
   }

   @Test
   public void jsEval_array_spread(){
      Object result = StringUtil.jsEval("(a)=>{return [...a,'three']}",Json.fromString("[\"one\",\"two\"]"));
      assertNotNull("result should not be null",result);
      assertTrue("result should be Json",result instanceof Json);
      Json json = (Json)result;
      assertTrue("json should be an array: "+json,json.isArray());
      assertEquals("json should have 3 entries: "+json,3,json.size());
      assertTrue("json should contain three: "+json,json.values().contains("three"));
   }
   @Test
   public void jsEval_array_spread_empty_array(){
      Object result = StringUtil.jsEval("(a)=>{return [...a,'three']}",Json.fromString("[]"));
      assertNotNull("result should not be null",result);
      assertTrue("result should be Json",result instanceof Json);
      Json json = (Json)result;
      assertTrue("json should be an array: "+json,json.isArray());
      assertEquals("json should have 1 entries: "+json,1,json.size());
      assertTrue("json should contain three: "+json,json.values().contains("three"));
   }
   @Test
   public void jsEval_array_push(){
      Object result = StringUtil.jsEval("function(a){a.push('three'); return a;}",Json.fromString("[\"one\",\"two\"]"));
      assertNotNull("result should not be null",result);
      assertTrue("result should be Json",result instanceof Json);
      Json json = (Json)result;
      assertTrue("json should be an array: "+json,json.isArray());
      assertEquals("json should have 3 entries: "+json,3,json.size());
      assertTrue("json should contain three: "+json,json.values().contains("three"));
   }
   @Test
   public void jsEval_array_push_lambda(){
      Json input = Json.fromString("[\"one\",\"two\"]");
      Object result = StringUtil.jsEval("(a)=>{a.push('three'); return a;}",input);
      assertNotNull("result should not be null",result);
      assertTrue("result should be Json",result instanceof Json);
      Json json = (Json)result;
      assertTrue("json should be an array: "+json,json.isArray());
      assertEquals("json should have 3 entries: "+json,3,json.size());
      assertTrue("json should contain three: "+json,json.values().contains("three"));
      assertTrue("input should contain three: "+input,input.values().contains("three"));
   }
   @Test
   public void jsEval_array_concat(){
      Object result = StringUtil.jsEval("(a)=>{return a.concat(['a','b']);}",Json.fromString("[\"one\",\"two\"]"));
      assertNotNull("result should not be null",result);
      assertTrue("result should be Json",result instanceof Json);
      Json json = (Json)result;
      assertTrue("json should be an array: "+json,json.isArray());
      assertEquals("json should have 4 entries: "+json,4,json.size());
      assertTrue("json should contain 'a': "+json,json.values().contains("a"));
   }
   @Test
   public void jsEval_array_map(){
      Object result = StringUtil.jsEval("(a)=>{return a.map(v=>`${v}_value`);}",Json.fromString("[\"one\",\"two\"]"));
      assertNotNull("result should not be null",result);
      assertTrue("result should be Json",result instanceof Json);
      Json json = (Json)result;
      assertTrue("json should be an array: "+json,json.isArray());
      assertEquals("json should have 2 entries: "+json,2,json.size());
      assertTrue("json should contain 'a': "+json,json.values().contains("one_value"));
   }

   @Test
   public void jsEval_array_filter(){
      Object result = StringUtil.jsEval("(a)=>{return a.filter(v=>v<3);}",Json.fromString("[1,2,3,4]"));
      assertNotNull("result should not be null",result);
      assertTrue("result should be Json",result instanceof Json);
      Json json = (Json)result;
      assertTrue("json should be an array: "+json,json.isArray());
      assertEquals("json should have 2 entries: "+json,2,json.size());
      assertTrue("json should contain 2: "+json,json.values().contains(2l));//graaljs uses long not ints
   }

   @Test
   public void jsEval_array_length(){
      Object result = StringUtil.jsEval("(a)=>{return a.length;}",Json.fromString("[1,2,3,4]"));
      assertNotNull("result should not be null",result);
      assertTrue("result snould be a number",result instanceof Number);
      Integer value = ((Number)result).intValue();
      assertEquals("length should be 4",4,value.intValue());
   }

   @Test
   public void jsEval_async_await_fetch(){
      Object result = StringUtil.jsEval("async (a,b)=>{ let rtrn = false; rtrn = await fetch('https://www.redhat.com'); return rtrn;}","","");
      assertFalse("async should not return until after fetch",result instanceof Boolean);
      assertTrue("fetch should return json",result instanceof Json);
      Json json = (Json)result;
      assertTrue("json.status should exist",json.has("status"));
   }

   @Test
   public void isDuration(){
      assertTrue("seconds",StringUtil.isDuration("1s"));
      assertTrue("two no space",StringUtil.isDuration("1m1s"));
      assertTrue("two with space",StringUtil.isDuration("1m 1s"));
      assertFalse("empty string",StringUtil.isDuration(""));
      assertFalse("just a number",StringUtil.isDuration("1"));
      assertFalse("just a suffix",StringUtil.isDuration("s"));
   }
   @Test
   public void parseToMs(){
      assertEquals("seconds",1000,StringUtil.parseToMs("1s"),0.0001);
      assertEquals("after decimal",0.1,StringUtil.parseToMs(".1ms"),0.0001);
      assertEquals("before and after decimal",1.1,StringUtil.parseToMs("1.1ms"),0.0001);
      assertEquals("before and after decimal",1.123456,StringUtil.parseToMs("1.123456ms"),0.0001);
      assertEquals("microsecond",0.001,StringUtil.parseToMs("1"+StringUtil.MICROSECONDS),0.0001);
   }

   @Test
   public void jsEval_asynch_new_fetch(){
      Object result = StringUtil.jsEval("async (a,b)=>await fetch2('https://www.redhat.com',{})",
              Arrays.asList("fetch2 = async (url,options)=>new Promise(new (Java.type('io.hyperfoil.tools.yaup.json.graaljs.JsFetch'))(url,options));"),"","");

      assertTrue("fetch should return json",result instanceof Json);
      Json json = (Json)result;
      assertTrue("json.status should exist",json.has("status"));
   }

   @Test
   public void isJsFnLike(){
      assertTrue(StringUtil.isJsFnLike("(a,b,c)=>a"));
      assertTrue(StringUtil.isJsFnLike("(a,b,c)=>{ return a }"));
      assertTrue(StringUtil.isJsFnLike("()=>'a'"));
      assertTrue(StringUtil.isJsFnLike("()=>{return 'a'} "));
      assertTrue(StringUtil.isJsFnLike("({a,b},c)=>'a'"));
      assertTrue(StringUtil.isJsFnLike("(all,json,state)=>{\n" +
              "  (!('lts_payload' in all)){\n" +
              "    all.lts_payload=[]\n" +
              "  }\n" +
              "  all.lts_payload.push(json)\n" +
              "}\n"));
   }
   @Test
   public void jsEval_async_await_fetch_invalid_host(){
      Object result = StringUtil.jsEval("async (a,b)=>{ let rtrn = false; rtrn = await fetch('https://fail.fail.www.redhat.com').then((a)=>{return 'resolve'},(b)=>{return 'reject'}); console.log('rtrn',rtrn); return rtrn;}","","");
      assertTrue("fetch should return json: "+result,result instanceof String);
      assertEquals("fetch should use reject handler","reject",(String)result);
   }
   @Test
   public void jsEval_async_await_fetch_insecure(){
      Object result = StringUtil.jsEval("async (a,b)=>{ let rtrn = false; rtrn = await fetch('https://www.redhat.com',\n"+
              "      { \n" +
              "        tls : 'ignore', \n" +
              "        method: 'HEAD', \n" +
              "        redirect: 'ignore', \n" +
              "        headers: {\n" +
              "          'Authorization' : 'Basic '+btoa(\"foo:bar\"),\n" +
              "          'Content-Type' : 'application/json'\n" +
              "        }\n" +
              "      }\n" +
              "); return rtrn;}","","");
      assertFalse("async should not return until after fetch "+(result!=null?result.getClass():"null")+" "+result,result instanceof Boolean);
      assertTrue("fetch should return json "+(result!=null?result.getClass():"null")+" "+result,result instanceof Json);
      Json json = (Json)result;
      assertTrue("json.status should exist",json.has("status"));
   }

   @Test
   public void jsEval_lambda_invalidJs(){
      try {
         Object result = StringUtil.jsEval("(a,b)=>{return [b;}", "a", "b");
         fail("invalid js should throw an exception");
      }catch(JsException e){
         //expected
      }
   }

   @Test
   public void jsEval_function(){
      Map<String,String> map = new HashMap<>();
      map.put("foo","FOO");
      Object result = StringUtil.jsEval("function(a,b){return b;}","a","b");
      assertEquals("expect function to evaluate with input","b",result);
   }

   @Test
   public void jsEval_math(){
      Object result = StringUtil.jsEval("2+2");
      assertNotNull("result should not be null default",result);
      assertTrue("result should be a Long "+result,result instanceof Long);
      assertEquals("result should be 4",new Long(4),result);
   }
   @Test
   public void jsEval_math_lambda(){
      Object result = StringUtil.jsEval("(a,b)=>a+b",1,2);
      assertNotNull("result should not be null default",result);
      assertTrue("result should be a Long "+result,result instanceof Long);
      assertEquals("result should be 4",new Long(3),result);
   }

   @Test
   public void jsEval_json_dot_notation(){
      Object result = StringUtil.jsEval("(a)=>a.foo",Json.fromString("{\"foo\":\"bar\"}"));
      assertNotNull("result should not be null default",result);
      assertTrue("result should be a String "+result,result instanceof String);
      assertEquals("result should be from json","bar",result);
   }
   @Test
   public void jsEval_json_nested_dot_notation(){
      Object result = StringUtil.jsEval("(a)=>a.foo.bar",Json.fromString("{\"foo\": {\"bar\":\"biz\" }}"));
      assertNotNull("result should not be null default",result);
      assertTrue("result should be a String "+result,result instanceof String);
      assertEquals("result should be from json","biz",result);
   }
   @Test
   public void jsEval_object_keys(){
      Object result = StringUtil.jsEval("(a)=>Object.keys(a)",Json.fromString("{\"foo\":\"FOO\",\"bar\":\"BAR\"}"));
      assertNotNull("result should not be null default",result);
      assertTrue("result should be a Json"+result.getClass(),result instanceof Json);
      Json json = (Json)result;
      assertTrue("json should be an array "+json,json.isArray());
      assertEquals("Json should have 2 entries: "+json,2,json.size());
   }
   @Test
   public void jsEval_object_values(){
      Object result = StringUtil.jsEval("(a)=>Object.values(a)",Json.fromString("{\"foo\":\"FOO\",\"bar\":\"BAR\"}"));
      assertNotNull("result should not be null default",result);
      assertTrue("result should be a Json"+result.getClass(),result instanceof Json);
      Json json = (Json)result;
      assertTrue("json should be an array "+json,json.isArray());
      assertEquals("Json should have 2 entries: "+json,2,json.size());
   }
   @Test
   public void jsEval_object_entries(){
      Object result = StringUtil.jsEval("(a)=>Object.entries(a)",Json.fromString("{\"foo\":\"FOO\",\"bar\":\"BAR\"}"));
      assertNotNull("result should not be null default",result);
      assertTrue("result should be a Json"+result.getClass(),result instanceof Json);
      Json json = (Json)result;
      assertTrue("json should be an array "+json,json.isArray());
      assertEquals("Json should have 2 entries: "+json,2,json.size());
      assertTrue("json[0] should be json: "+json.get(0),json.get(0) instanceof Json);
      assertTrue("json[0] should be an array: "+json.get(0),json.getJson(0).isArray());
   }

   @Test
   public void jsEval_object_assign_entry(){
      Json input = new Json(false);
      Object result = StringUtil.jsEval("(a)=>{if(!('foo' in a)){a.foo=[]}}",input);
      assertTrue("json should have foo key:"+input,input.has("foo"));
   }

   @Test
   public void countOccurrances_jsonpaths() {
      HashedLists grouped = StringUtil.groupCommonPrefixes(Arrays.asList(
         "$.faban.run.SPECjEnterprise.\"fa:runConfig\".\"fa:runControl\".\"fa:rampUp\".\"text()\"",
         "$.faban.run.SPECjEnterprise.\"fa:runConfig\".\"fa:scale\".\"text()\"",
         "$.faban.xml.benchResults.benchSummary.runId.\"text()\""
      ), (a, b) -> {
         String prefix = a.substring(0, StringUtil.commonPrefixLength(a, b));
         if (prefix.length() == 0) {
            return 0;
         }
         if (prefix.contains(".")) {
            prefix = prefix.substring(0, prefix.lastIndexOf("."));
         }
         return prefix.length();
      });
      assertEquals("expect 2 groups", 2, grouped.size());
      assertTrue("expect runConfig group", grouped.containsKey("$.faban.run.SPECjEnterprise.\"fa:runConfig\""));
      assertTrue("expect faban group", grouped.containsKey("$.faban"));
      assertEquals("expect 2 entries for $.faban.run.SPECjEnterprise.\"fa:runConfig\"", 2, grouped.get("$.faban.run.SPECjEnterprise.\"fa:runConfig\"").size());
   }

   @Test
   public void countOccurrances_nonOverlapping() {
      assertEquals("don't let pattern overlap", 2, StringUtil.countOccurances("{{{{", "{{"));
   }

   @Test
   public void groupCommonPrefixes_no_omatches() {
      HashedLists grouped = StringUtil.groupCommonPrefixes(Arrays.asList(
         "one",
         "two",
         "apple"
      ));
      assertEquals("separate group for each input", 3, grouped.size());
   }

   @Test
   public void groupCommonPrefixes_all_matches() {
      HashedLists grouped = StringUtil.groupCommonPrefixes(Arrays.asList(
         "one",
         "onetwo",
         "oneapple"
      ));
      assertEquals("all in one group", 1, grouped.size());
      assertTrue("group name is one", grouped.keys().contains("one"));
   }

   @Test
   public void groupCommonPrefixes_longest_match() {
      HashedLists grouped = StringUtil.groupCommonPrefixes(Arrays.asList(
         "one",
         "onetwo",
         "onetwothree"
      ));
      assertEquals("two groups", 2, grouped.size());
      assertTrue("groups are [one, onetwo]", grouped.keys().containsAll(Arrays.asList("one", "onetwo")));
      assertEquals("group onetwo should have 2 entries", 2, grouped.get("onetwo").size());
   }

   @Test
   public void getPatternNames_nested(){
      Map<Object, Object> map = new HashMap<>();
      try {
         List<String> names = StringUtil.getPatternNames("${{foo.${{bar}}.biz}}",map);
         assertEquals("expect 2 entries: "+names,2,names.size());
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void getPatternNames_no_names(){
      Map<Object, Object> map = new HashMap<>();
      try {
         List<String> names = StringUtil.getPatternNames("no names",map);
         assertEquals("names should be empty: "+names.toString(),0,names.size());
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void getPatternNames_single_pattern(){
      Map<Object, Object> map = new HashMap<>();
      try {
         List<String> names = StringUtil.getPatternNames("${{FOO}}",map);
         assertEquals("names should have 1 entry: "+names.toString(),1,names.size());
         assertTrue("names should contain expected entries: "+names.toString(),names.containsAll(Arrays.asList("FOO")));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void getPatternNames_pattern_in_default(){
      Map<Object, Object> map = new HashMap<>();
      try {
         List<String> names = StringUtil.getPatternNames("${{FOO:${{BAR}}}}",map);
         assertEquals("names should have 2 entries: "+names.toString(),2,names.size());
         assertTrue("names should contain expected entries: "+names.toString(),names.containsAll(Arrays.asList("FOO","BAR")));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void getPatternNames_pattern_in_javascript(){
      Map<Object, Object> map = new HashMap<>();
      try {
         List<String> names = StringUtil.getPatternNames("${{='${{FOO}}'.split('${{BAR}}').join(' ')}}",map);
         assertEquals("names should have 2 entries: "+names.toString(),2,names.size());
         assertTrue("names should contain expected entries: "+names.toString(),names.containsAll(Arrays.asList("FOO","BAR")));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void getPatternNames_array_expansion(){
      Map<Object, Object> map = new HashMap<>();
      try {
         List<String> names = StringUtil.getPatternNames("${{ [...${{RUN.FOO}},{'test':'worked'}] }}",map,
                 (v)->v,
                 StringUtil.PATTERN_PREFIX,
                 "_",
                 StringUtil.PATTERN_SUFFIX,
                 StringUtil.PATTERN_JAVASCRIPT_PREFIX);
         assertEquals("names should have 1 entry: "+names,1,names.size());
         assertEquals("names[0] should be RUN.FOO: "+names,"RUN.FOO",names.get(0));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void getPatternNames_multiple_patterns(){
      Map<Object, Object> map = new HashMap<>();
      try {
         List<String> names = StringUtil.getPatternNames("${{FOO}}${{BAR}}",map);
         assertEquals("names should have 2 entries: "+names.toString(),2,names.size());
         assertTrue("names should contain expected entries: "+names.toString(),names.containsAll(Arrays.asList("FOO","BAR")));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void getPatternNames_pattern_in_map(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","${{BAR}}");
      try {
         List<String> names = StringUtil.getPatternNames("${{FOO}}",map);
         assertEquals("names should have 2 entries: "+names.toString(),2,names.size());
         assertTrue("names should contain expected entries: "+names.toString(),names.containsAll(Arrays.asList("FOO","BAR")));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void getPatternNames_pattern_with_dots(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","${{BAR}}");
      try {
         List<String> names = StringUtil.getPatternNames("${{foo.bar.biz}}",map);
         assertEquals("names should have 1 entry: "+names.toString(),1,names.size());
         assertTrue("names should contain expected entries: "+names.toString(),names.containsAll(Arrays.asList("foo.bar.biz")));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test @Ignore /*Still working on how this should work for json in map*/
   public void populatePattern_jsonpath_search(){
      try{
         Json json = Json.fromJs("[{key:'a',value:'ant'},{key:'b-b',value:'bat'},{key:'c',value:'cat'}]");
         Map<Object, Object> map = new HashMap<>();
         map.put("data",json);
         String response = StringUtil.populatePattern("${{data[?(@.key==\"c\")]}}",map);
         assertEquals("response should be value of b","bat",response);
      }catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_missing_nested_pattern_uses_default(){
      try {
         Map<Object, Object> map = new HashMap<>();
         String response = StringUtil.populatePattern("${{${{afk}}:default}}",map);
         assertEquals( "default", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_javascript_expand_object() {
      try {
         Map<Object, Object> map = new HashMap<>();
         String response = StringUtil.populatePattern("$<<={...{\"foo\":\"bar\"}}>>", map, "$<<", "_", ">>", "=");
         assertEquals("return javascript ", "{\"foo\":\"bar\"}", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_cat_jq_issue() {
      Map<Object, Object> map = new HashMap<>();
      map.put("file", "foo");
      try {
         String response = StringUtil.populatePattern("cat ${{file}}.user | jq '.storage.files[ .storage.files | length] |= . + {\"filesystem\":\"root\",\"path\":\"/etc/resolv.conf\",\"user\":{\"name\":\"root\"},\"contents\":{\"source\":\"data:text/plain;charset=utf-8;base64,'$(echo -e \"# Generated by ME\\nsearch rdu2.scalelab.redhat.com scalelab\\nnameserver 172.16.41.125\" | base64 --wrap=0)'\",\"verification\":{}},\"mode\":420}' > ${{file}}.resolv", map);
         assertFalse("should replace all ${{file}}", response.contains("${{file}}"));
         assertEquals("failed to replace file", "cat foo.user | jq '.storage.files[ .storage.files | length] |= . + {\"filesystem\":\"root\",\"path\":\"/etc/resolv.conf\",\"user\":{\"name\":\"root\"},\"contents\":{\"source\":\"data:text/plain;charset=utf-8;base64,'$(echo -e \"# Generated by ME\\nsearch rdu2.scalelab.redhat.com scalelab\\nnameserver 172.16.41.125\" | base64 --wrap=0)'\",\"verification\":{}},\"mode\":420}' > foo.resolv", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_issue() {
      Map<Object, Object> map = new HashMap<>();

      map.put("APP.name", "getting-started");
      map.put("VARIANT.name", "jvm");
      map.put("CPU.cores", 1);
      map.put("getting-started.jvm.1.pid", "1234");
      try {
         String response = StringUtil.populatePattern("${{${{APP.name}}.${{VARIANT.name}}.${{CPU.cores}}.pid}}", map);
         assertEquals("expect response from map", "1234", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test
   public void populatePattern_sed_example() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FABAN_BENCHMARK", "foo");
      try {
         String response = StringUtil.populatePattern(
            "sed -i 's/\\(\\s*\\)archiveName=.*/\\1archiveName=\"${{FABAN_BENCHMARK:$baseName}}.$extension\"/g' ./modules/specjdriverharness/specjdriverharness.gradle",
            map,
            StringUtil.PATTERN_PREFIX,
            StringUtil.PATTERN_DEFAULT_SEPARATOR,
            StringUtil.PATTERN_SUFFIX,
            StringUtil.PATTERN_JAVASCRIPT_PREFIX
         );
         assertEquals("pattern should replace when in quotes that are outside of ${{..}}}", "sed -i 's/\\(\\s*\\)archiveName=.*/\\1archiveName=\"foo.$extension\"/g' ./modules/specjdriverharness/specjdriverharness.gradle", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_javascript_array_spread_with_default(){
      Map<Object, Object> map = new HashMap<>();
      map.put("hostname","foo.bar.biz.buz");
      try {
         String response = StringUtil.populatePattern("${{=[ \"${{hostname}}\" , ...${{RUN.FOO:[]}} ]}}",map,StringUtil.PATTERN_PREFIX, StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX, StringUtil.PATTERN_JAVASCRIPT_PREFIX);
      } catch (PopulatePatternException pe){
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_array_spread_in_reference(){
      Map<Object, Object> map = new HashMap<>();
      map.put("hostname","foo.bar.biz.buz");
      map.put("value","${{=[ \"${{hostname}}\" , ...${{RUN.FOO:[]}} ]}}");
      try {
         String response = StringUtil.populatePattern("${{value:${{hostname}}}}",map,StringUtil.PATTERN_PREFIX, StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX, StringUtil.PATTERN_JAVASCRIPT_PREFIX);
      } catch (PopulatePatternException pe){
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_javascript_array_spread_append_object() {
      Map<Object, Object> map = new HashMap();
      map.put("FOO", new Json(true));

      try {
         String response = StringUtil.populatePattern("${{[...${{FOO}},{'test':'worked'}] }}", map, StringUtil.PATTERN_PREFIX, "::", StringUtil.PATTERN_SUFFIX, StringUtil.PATTERN_JAVASCRIPT_PREFIX);
         assertEquals("test should be added to an javascript array", "[{\"test\":\"worked\"}]", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }
   @Test
   public void populatePattern_javascript_array_spread_empty_array() {
      Map<Object, Object> map = new HashMap();
      map.put("alpha", Json.fromString("[]"));
      map.put("charlie", "\"cat\"");

      try {
         String response = StringUtil.populatePattern("${{ [...${{alpha}}, ${{charlie}} ] }}", map);
         assertEquals("expect arrays to be combined", "[\"cat\"]", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test
   public void populatePattern_javascript_array_spread_dot_in_key() {
      Map<Object, Object> map = new HashMap();
      map.put("alpha.foo.bar", Arrays.asList("\"ant\"", "\"apple\""));
      map.put("charlie", "\"cat\"");
      map.put("key", "foo");

      try {
         String response = StringUtil.populatePattern("${{=[...${{alpha.${{key}}.bar}},${{charlie}}]}}", map);

         assertEquals("expect arrays to be combined", "[\"ant\",\"apple\",\"cat\"]", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   /**
    * Tests that default separator is not detected when it appears as part of the boolean operator
    */
   @Test
   public void populatePattern_javascript_boolean_expression_no_default(){
      Map<Object, Object> map = new HashMap();
      map.put("missing",false);
      map.put("bar","dos");
      try {
         String response = StringUtil.populatePattern("${{=missing ? \"found\" : \"missing\"}}", map);
         assertEquals("missing",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_boolean_expression_default(){
      Map<Object, Object> map = new HashMap();
      map.put("missing",false);
      map.put("bar","dos");
      try {
         String response = StringUtil.populatePattern("${{=missing ? \"found\" : \"missing\" : \"bad\"}}", map);
         assertEquals("missing",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_nested_boolean_expression_no_separator(){
      Map<Object, Object> map = new HashMap();
      map.put("test",true);
      map.put("bar","dos");
      try {
         String response = StringUtil.populatePattern("${{=test ? bar ? \"bar\" : \"no-bar\" : \"missing\"}}", map);
         assertEquals("bar",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_quoted_question_not_boolean_expression(){
      Map<Object, Object> map = new HashMap();
      map.put("test",true);
      map.put("bar","dos");
      try {
         String response = StringUtil.populatePattern("${{=\"?${{afk}}:${{bar}}\":defaultValue}}", map);
         assertEquals("defaultValue",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_single_quote_question(){
      Map<Object, Object> map = new HashMap();
      map.put("test",true);
      map.put("bar","dos");
      try {
         String response = StringUtil.populatePattern("${{=\'?${{afk}}${{bar:}}\':defaultValue}}", map);
         assertEquals("defaultValue",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_string_template(){
      Map<Object, Object> map = new HashMap();
      map.put("test",true);
      map.put("bar","dos");
      try {
         String response = StringUtil.populatePattern("${{=`?${afk}${bar}`:defaultValue}}", map);
         assertEquals("defaultValue",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_quoted_separator_no_default(){
      Map<Object, Object> map = new HashMap();
      map.put("test",true);
      map.put("bar","dos");
      try {
         String response = StringUtil.populatePattern("${{=\"${{test}}:${{bar}}\".toLowerCase()}}", map);
         assertEquals("true:dos",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_array_spread() {
      Map<Object, Object> map = new HashMap();
      map.put("alpha", Arrays.asList("\"ant\"", "\"apple\""));
      map.put("bravo", Arrays.asList("\"bear\"", "\"bull\""));
      map.put("charlie", "\"cat\"");

      try {
         String response = StringUtil.populatePattern("${{ [${{charlie}}, ...${{alpha}}, ...${{bravo}} ] }}", map);
         assertEquals("expect arrays to be combined", "[\"cat\",\"ant\",\"apple\",\"bear\",\"bull\"]", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }
   @Test
   public void populatePattern_javascript_array_spread_quote_in_value() {
      Map<Object, Object> map = new HashMap();
      map.put("alpha", Arrays.asList("\"ant\"", "\"apple\""));
      map.put("bravo", Arrays.asList("\"bear\"", "\"bull\""));
      map.put("charlie", "\"cat\"");

      try {
         String response = StringUtil.populatePattern("${{ [${{charlie}}, ...${{alpha}}, ...${{bravo}} ] }}", map);
         assertEquals("expect arrays to be combined", "[\"cat\",\"ant\",\"apple\",\"bear\",\"bull\"]", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   /**
    * having { ..${{..}} } inside a ${{}} causes the outer pattern to use the extra } from the inner replacement and close the pattern too soon
    * options:
    * change the prefix / suffix to not be two of the same character and hope we find a unique character combiniation {[]}
    * use last instance of suffix?
    * how do we identify the split between name and default value?
    * count prefixes?
    * we have prefix index and index of next prefix, suffix, separator
    * if prefix > -1 we inc count and look for a separate after dropping count number of suffix?
    */
   @Test
   public void populatePattern_prefix_suffix() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO", "_");
      map.put("{_}", "FOUND");
      try {
         String response = StringUtil.populatePattern("$<<{$<<FOO>>}>>", map, "$<<", ":", ">>", "=");
         assertEquals("FOUND", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test
   public void populatePattern_multiple_separators(){
      //TODO decide if first or last separator should be used, currently use last
      Map<Object, Object> map = new HashMap<>();
      try{
         String response = StringUtil.populatePattern("${{FOO:bar:biz}}",map);
         assertEquals("response should be from last separator","biz",response);
      }catch (PopulatePatternException pe){
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatPattern_javascript_padStart(){
      Map<Object, Object> map = new HashMap<>();
      Json array = Json.fromString("[\"one\",\"two\"]");
      map.put("index",5);
      try {
         String response = StringUtil.populatePattern("${{=(${{index}}).toString().padStart(3,\"0\")}}",map);
         assertEquals("005",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());

      }
   }

   @Test
   public void populatePattern_javascript_missing(){
      Map<Object, Object> map = new HashMap<>();
      Json array = Json.fromString("[\"one\",\"two\"]");
      map.put("ARRAY",array);
      //map.put("ADD","vert.x-eventloop-thread-1,5,main");
      try {
         String response = StringUtil.populatePattern("${{= [...${{ARRAY}}, ${{ADD}} ] }}",map);
      } catch (PopulatePatternException pe) {
         return;
      }
      fail("expected an exception from missing value");
   }

   @Test
   public void populatePattern_javascript_incorrect_filter(){
      Map<Object, Object> map = new HashMap<>();
      Json servers = Json.fromString("[ {\"name\":\"server-01\", \"idrac\":\"server01-drac.example.com\" } ]");
      map.put("servers",servers);
      map.put("missingTarget", "server01");

      try {
         String response = StringUtil.populatePattern("${{= ${{servers}}.filter(server => server.name === '${{missingTarget}}' )[0].idrac }}",map);
      } catch (PopulatePatternException pe) {
         assertTrue(pe.isJsFailure());
         return;
      }
      fail("expected an exception from missing value");
   }



   @Test(timeout = 10_000)
   public void populatePattern_looped_references(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","${{BAR}}");
      map.put("BAR","${{BIZ}}");
      map.put("BIZ","${{BUZ}}");
      map.put("BUZ","${{FOO}}");

      try{
         String response = StringUtil.populatePattern("${{FOO}}",map);
      }catch (PopulatePatternException pe){
         return;
      }
      fail("expected an exception not infinite loop");
   }

   @Test
   public void populatePattern_replace_regex(){
      Map<Object, Object> map = new HashMap<>();
      try {
         String response = StringUtil.populatePattern(
            StringUtil.PATTERN_PREFIX + StringUtil.PATTERN_JAVASCRIPT_PREFIX + " \"00:11:22:33:44:0b\".replace(/:/g,'-') " + StringUtil.PATTERN_SUFFIX,
            map,
            StringUtil.PATTERN_PREFIX,
            "_",
            StringUtil.PATTERN_SUFFIX,
            StringUtil.PATTERN_JAVASCRIPT_PREFIX
            );
      }catch (PopulatePatternException pe){
         fail(pe.getMessage());
      }


   }

   @Test
   public void populatePattern_javascript_toLowerCase(){
      Map<Object, Object> map = new HashMap<>();
      map.put("vm","VaLuE");
      try {
         String response = StringUtil.populatePattern("lowercase=\"${{=\"${{vm}}\".toLowerCase()}}\"",map);
         assertEquals("lowercase=\"value\"",response);
      }catch (PopulatePatternException pe){
         fail("did not expect a pattern exception\n"+pe.getMessage());
      }
   }

   @Test
   public void populatePattern_quotes_in_default(){
      Map<Object, Object> map = new HashMap<>();
      try{
         String response = StringUtil.populatePattern("${{missing:'not_found'}}",map);
         assertEquals("'not_found'",response);
      }catch (PopulatePatternException pe){
         fail("did not expect a pattern exception\n"+pe.getMessage());
      }
   }

   @Test
   public void populatePattern_add_to_list(){
      Map<Object, Object> map = new HashMap<>();
      map.put("vm",new Json(true));
      try{
         String response = StringUtil.populatePattern("${{= [...${{vm:[]}},\"added\"]}}",map);
      }catch (PopulatePatternException pe){
         fail("did not expect a pattern exception\n"+pe.getMessage());
      }

   }

   @Test(timeout = 10_000)
   public void populatePatttern_self_reference(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","${{FOO}}");
      try{
         String response = StringUtil.populatePattern("${{FOO}}",map);
      }catch (PopulatePatternException pe){
         return;
      }
      fail("expect an exception not infinite loop");
   }

   @Test(timeout = 10_000)
   public void populatePatttern_self_reference_padded_javascript(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","${{= [...${{FOO}},new String(${{BAR}}) ] }}");
      try{
         String response = StringUtil.populatePattern("${{= ${{FOO}}.join(' ')}}",map);
      }catch (PopulatePatternException pe){
         return;
      }
      fail("expect an exception not infinite loop");
   }

   @Test
   public void populatePattern_same_pattern_twice(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","BAR");

      try{
         String response = StringUtil.populatePattern("${{FOO}}${{FOO}}",map);
         assertEquals("expect pattern to replace twice","BARBAR",response);
      }catch (PopulatePatternException pe){
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_javascript_regex(){
      try {
         String response = StringUtil.populatePattern("${{='<foo>'.match(/<(.*?)>/)[1]}}", null);
         assertEquals("capture tag","foo",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_object_spread() {
      Map<Object, Object> map = new HashMap<>();
      Json foo = Json.fromString("{\"foo1\":\"one\",\"foo2\":{\"buz\":\"foo\"}}");
      Json bar = Json.fromString("{\"bar1\":\"one\",\"bar2\":{\"buz\":\"bee\"}}");

      map.put("FOO", foo);
      map.put("BAR", bar);

      try {
         String response = StringUtil.populatePattern("$<<(()=>({...$<<FOO>>,bar:$<<BAR>>}))()>>", map, "$<<", "_", ">>", "=");
         assertEquals("function should evaluate and spread json object", "{\"foo1\":\"one\",\"foo2\":{\"buz\":\"foo\"},\"bar\":{\"bar1\":\"one\",\"bar2\":{\"buz\":\"bee\"}}}", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test
   public void populatePattern_javascript_range(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","1");
      try {
         String response = StringUtil.populatePattern("${{=range(1,10)}}", map, evals,StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
         assertTrue("expect javascript: "+response,Json.isJsonLike(response));
         Json json = Json.fromString(response);
         assertNotNull(json);
         assertTrue("json should be an array",json.isArray());
         assertEquals("json should have 8 entries",9,json.size());
         assertEquals("json[0] should be 1",1,json.getLong(0));
         assertEquals("json[8] should be 9",9,json.getLong(8));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_range_negative_stop(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","1");
      try {
         String response = StringUtil.populatePattern("${{=range(0,-10)}}", map, evals,StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
         assertTrue("expect javascript: "+response,Json.isJsonLike(response));
         Json json = Json.fromString(response);
         assertNotNull(json);
         assertTrue("json should be an array",json.isArray());
         assertEquals("json size: "+json,10,json.size());
         assertEquals("json[0]",0,json.getLong(0));
         assertEquals("json[9]",-9,json.getLong(9));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_range_implicit_negative_step(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","1");
      try {
         String response = StringUtil.populatePattern("${{=range(20,10)}}", map, evals,StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
         assertTrue("expect javascript: "+response,Json.isJsonLike(response));
         Json json = Json.fromString(response);
         assertNotNull(json);
         assertTrue("json should be an array:"+json,json.isArray());
         assertEquals("json should have 10 entries:"+json,10,json.size());
         assertEquals("json[0]",20,json.getLong(0));
         assertEquals("json[9]",11,json.getLong(9));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_range_step(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","1");
      try {
         String response = StringUtil.populatePattern("${{=range(20,10,2)}}", map, evals,StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
         assertTrue("expect javascript: "+response,Json.isJsonLike(response));
         Json json = Json.fromString(response);
         assertNotNull(json);
         assertTrue("json should be an array: "+json,json.isArray());
         assertEquals("json should have 5 entries: "+json,5,json.size());
         assertEquals("json[0]: "+json,20,json.getLong(0));
         assertEquals("json[4]: "+json,12,json.getLong(4));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_range_incorrect_step(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","1");
      boolean caught = false;
      try {
         String response = StringUtil.populatePattern("${{=range(20,10,-2)}}", map, evals,StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
      } catch (PopulatePatternException pe) {
         caught = true;
      }
      assertTrue("Expect a PopulatePatternException when step is the wrong direction for range",caught);
   }

   @Test
   public void populatePattern_javascript_milliseconds_concat(){
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO","1");
      try {
         String response = StringUtil.populatePattern("${{=milliseconds(${{FOO}}+'m'):100}}", map, evals,StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
         assertEquals("expect 1m in ms","60000",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_javascript_milliseconds() {
      try {
         String response = StringUtil.populatePattern("${{=milliseconds('1m')}}", null, evals,StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
         assertEquals("expect 1m in ms","60000",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_javascript_seconds() {
      try {
         String response = StringUtil.populatePattern("${{=seconds('1m')}}", null, evals,StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
         assertEquals("expect 1m in seconds","60",response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_constant_in_evals(){
      Map<Object,Object> state = new HashMap<>();
      String response = null;
      try {
         response = StringUtil.populatePattern("${{=foo}}",state,Arrays.asList("const foo = 'bar'\nfunction doit(){return 'biz'}"),StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
      } catch (PopulatePatternException e) {
         fail(e.getMessage());
      }
      assertEquals("response should be set from javascript constant","bar",response);
   }
   @Test
   public void populatePattern_constant_function_in_evals(){
      Map<Object,Object> state = new HashMap<>();
      String response = null;
      try {
         response = StringUtil.populatePattern("${{=doit()}}",state,Arrays.asList("const foo = 'bar'\nfunction doit(){return 'biz'}"),StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
      } catch (PopulatePatternException e) {
         fail(e.getMessage());
      }
      assertEquals("response should be set from javascript constant","biz",response);
   }

   @Test
   public void populatePattern_javascript_seconds_concat(){
      Map<Object,Object> state = new HashMap<>();
      state.put("FOO", "10");
      state.put("BAR", "'1m'");

      String response = null;
      try {
         response = StringUtil.populatePattern("${{= 2*(seconds(${{BAR}})+${{FOO}}) :-1}}", state, evals,StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
         assertEquals("expected value with seconds()", "140", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test
   public void populatePattern_javascript_lambda_string_literal() {
      Map<Object, Object> map = new HashMap();
      Json list = new Json(true);
      list.add("one");
      list.add("two");
      list.add("three");
      map.put("list", list);
      try {
         String response = StringUtil.populatePattern("${{ ${{list}}.reduce((x,v)=>`${x}\n  ${v}:80`,'').trim()}}", map);
         assertEquals("one:80\n  two:80\n  three:80", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test
   public void populatePattern_missing_not_replace() {
      Map<Object, Object> map = new HashMap<>();
      try {
         String response = StringUtil.populatePattern("BAR=${{FOO}}", map);
         assertEquals("expect to not replace the pattern", "BAR=${{FOO}}", response);
         fail("did not trow expected PopulatePatternException");
      } catch (PopulatePatternException pe) {
         assertTrue("passed as expected", true);
      }

   }

   @Test
   public void populatePattern_missing_replace() {
      Map<Object, Object> map = new HashMap<>();
      try {
         String response = StringUtil.populatePattern("BAR=${{FOO:}}", map);

         assertEquals("expect to not replace the pattern", "BAR=", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test @Ignore
   public void populatePattern_resolve_name_from_map(){
      Map<Object, Object> map = new HashMap<>();
      map.put("NAME",2);
      try {
         String response = StringUtil.populatePattern("${{ 2*NAME :-1}}", map);
         assertEquals("expected default value when missing value", "4", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_arithmetic_missing_value() {
      Map<Object, Object> map = new HashMap<>();
      try {
         String response = StringUtil.populatePattern("${{ 2*MISSING :-1}}", map);
         assertEquals("expected default value when missing value", "-1", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test
   public void populatePattern_two_values() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO", "foo");
      map.put("BAR", "bar");
      try {
         String response = StringUtil.populatePattern("${{FOO}}${{BAR}}", map);
         assertEquals("foobar", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }
   @Test
   public void populatePattern_padding() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO", "foo");
      try {
         String response = StringUtil.populatePattern("${{  FOO  }}", map);
         assertEquals("foo", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_default_is_pattern() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO", "foo");
      map.put("BAR", "bar");
      try {
         String response = StringUtil.populatePattern("${{BIZ:${{FOO}}}}${{BAR}}", map);
         assertEquals("foobar", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test
   public void populatePattern_string_encode_json() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO", Json.fromString("{\"foo1\":\"one\",\"foo2\":{\"buz\":\"foo\"}}"));
      map.put("BAR", Json.fromString("{\"bar1\":\"one\",\"bar2\":{\"buz\":\"bee\"}}"));
      try {
         String response = StringUtil.populatePattern("${{FOO}}", map);
         assertEquals("response should be json encoded", map.get("FOO"), Json.fromString(response));
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test
   public void populatePattern_populate_before_exception(){
      Map<Object, Object> map = new HashMap<>();
      try {
         String response= StringUtil.populatePattern("${{missing}}_${{hasDefault:}}", map);
         fail("populatePattern should throw a PopulatePatternException");
      }catch (PopulatePatternException pe){
         String result = pe.getResult();
         assertNotNull("result should not be null",result);
         assertFalse("result should not contain hasDefault: "+result,result.contains("hasDefault"));
      }
   }

   @Test
   public void populatePattern_name_and_default_patterns() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO", "${{BAR}}");
      map.put("BAR", "bar");
      try {
         String response = StringUtil.populatePattern("${{FOO:${{BIZ:biz}}}}${{BIZ:${{BAR}}}}", map);
         assertEquals("barbar", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test
   public void populatePattern_use_default() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO", "foo");
      map.put("BAR", "bar");
      try {
         String response = StringUtil.populatePattern("${{FOO}}.${{BAR}}.${{BIZ:biz}}", map);
         assertEquals("foo.bar.biz", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }

   }

   @Test
   public void populatePattern_use_js_on_value() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO", "foo");
      try {
         String response = StringUtil.populatePattern("${{\"${{FOO}}\".toUpperCase()}}", map);
         assertEquals("FOO", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_default_over_empty_value() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO", "");
      try {
         String response = StringUtil.populatePattern("${{FOO:foo}}", map);
         assertEquals("foo", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_empty_default_over_empty_value() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO", "");
      try {
         String response = StringUtil.populatePattern("${{FOO:}}", map);
         assertEquals("", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void populatePattern_empty_value_no_default() {
      Map<Object, Object> map = new HashMap<>();
      map.put("FOO", "");
      try {
         String response = StringUtil.populatePattern("${{FOO}}", map);
         assertEquals("", response);
      } catch (PopulatePatternException pe) {
         fail(pe.getMessage());
      }
   }

   @Test
   public void quoteReplaceWithEscaped() {
      String quoted = StringUtil.quote("foo\"bar");

      assertTrue("starts with quote", quoted.startsWith("\""));
      assertTrue("ends with quote", quoted.endsWith("\""));

      String sripped = quoted.substring(1, quoted.length() - 1);

      assertFalse(sripped.matches("\"(?<!\\\\\")"));
   }

   @Test
   public void findNotQuotedSimple() {

      assertEquals("foo\" bar\"", StringUtil.findNotQuoted("foo\" bar\" ", " "));
   }

   @Test
   public void findNotQuotedNotFund() {
      assertEquals("12345", StringUtil.findNotQuoted("12345", " "));
   }

   @Test
   public void findNotQuotedFirstChar() {
      assertEquals("", StringUtil.findNotQuoted("12345", " 1"));
   }

   @Test
   public void escapeBash(){
      assertEquals("escape \\u001b","\\\u001b",StringUtil.escapeBash("\u001b"));
      assertEquals("don't double escape \\\\u001b","\\\u001b",StringUtil.escapeBash("\\\u001b"));
      assertEquals("escape newline","\\n",StringUtil.escapeBash("\n"));
      assertEquals("don't double escape \\n","\\n",StringUtil.escapeBash("\\n"));
      assertEquals("escape character return","\\r",StringUtil.escapeBash("\r"));
      assertEquals("escape character return","\\t",StringUtil.escapeBash("\t"));
   }

   @Test
   public void escapeRegex() {

      assertEquals(". literals", "foo\\.bar\\.biz\\.buz", StringUtil.escapeRegex("foo.bar.biz.buz"));
   }
}
