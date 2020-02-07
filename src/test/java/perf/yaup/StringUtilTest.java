package perf.yaup;

import io.hyperfoil.tools.yaup.HashedList;
import io.hyperfoil.tools.yaup.HashedLists;
import io.hyperfoil.tools.yaup.PopulatePatternException;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class StringUtilTest {





   @Test
   public void countOccurrances_jsonpaths(){
      HashedLists grouped = StringUtil.groupCommonPrefixes(Arrays.asList(
         "$.faban.run.SPECjEnterprise.\"fa:runConfig\".\"fa:runControl\".\"fa:rampUp\".\"text()\"",
         "$.faban.run.SPECjEnterprise.\"fa:runConfig\".\"fa:scale\".\"text()\"",
         "$.faban.xml.benchResults.benchSummary.runId.\"text()\""
      ),(a,b)->{
         String prefix = a.substring(0,StringUtil.commonPrefixLength(a,b));
         if(prefix.length()==0){
            return 0;
         }
         if(prefix.contains(".")) {
            prefix = prefix.substring(0, prefix.lastIndexOf("."));
         }
         System.out.println("prefixing\na="+a+"\nb="+b+"\nprefix="+prefix);
         return prefix.length();
      });
      grouped.forEach((key,list)->{
         System.out.println(key+" : "+list);
      });
   }


    @Test
    public void countOccurrances_nonOverlapping(){

        assertEquals("don't let pattern overlap",2,StringUtil.countOccurances("{{{{","{{"));
    }

    @Test
    public void groupCommonPrefixes_no_omatches(){
       HashedLists grouped = StringUtil.groupCommonPrefixes(Arrays.asList(
          "one",
          "two",
          "apple"
       ));
       grouped.forEach((key,list)->{
          System.out.println(key+" : "+list);
       });
       assertEquals("separate group for each input",3,grouped.size());
    }
   @Test
   public void groupCommonPrefixes_all_matches(){
      HashedLists grouped = StringUtil.groupCommonPrefixes(Arrays.asList(
         "one",
         "onetwo",
         "oneapple"
      ));
      grouped.forEach((key,list)->{
         System.out.println(key+" : "+list);
      });
      grouped.values().forEach(v->{

      });
      assertEquals("all in one group",1,grouped.size());
      assertTrue("group name is one",grouped.keys().contains("one"));
   }
   @Test
   public void groupCommonPrefixes_longest_match(){
      HashedLists grouped = StringUtil.groupCommonPrefixes(Arrays.asList(
         "one",
         "onetwo",
         "onetwothree"
      ));
      grouped.forEach((key,list)->{
         System.out.println(key+" : "+list);
      });
      assertEquals("two groups",2,grouped.size());
      assertTrue("groups are [one, onetwo]",grouped.keys().containsAll(Arrays.asList("one","onetwo")));
      assertEquals("group onetwo should have 2 entries",2,grouped.get("onetwo").size());
   }

   @Test
   public void populatePattern_issue(){
      Map<Object,Object> map = new HashMap<>();

      map.put("APP.name","getting-started");
      map.put("VARIANT.name","jvm");
      map.put("CPU.cores",1);
      map.put("getting-started.jvm.1.pid","1234");
      try {
          String response = StringUtil.populatePattern("${{${{APP.name}}.${{VARIANT.name}}.${{CPU.cores}}.pid}}",map);
          assertEquals("expect response from map","1234",response);
      } catch (PopulatePatternException pe){
          fail();
      }

   }

   @Test
   public void populatePattern_sed_example(){
      Map<Object,Object> map = new HashMap<>();
      map.put("FABAN_BENCHMARK","foo");
      try{
          String response = StringUtil.populatePattern(
             "sed -i 's/\\(\\s*\\)archiveName=.*/\\1archiveName=\"${{FABAN_BENCHMARK:$baseName}}.$extension\"/g' ./modules/specjdriverharness/specjdriverharness.gradle",
             map,
             false,
             StringUtil.PATTERN_PREFIX,
             StringUtil.PATTERN_DEFAULT_SEPARATOR,
             StringUtil.PATTERN_SUFFIX
          );

          System.out.println(response);
          assertEquals("pattern should replace when in quotes that are outside of ${{..}}}","sed -i 's/\\(\\s*\\)archiveName=.*/\\1archiveName=\"foo.$extension\"/g' ./modules/specjdriverharness/specjdriverharness.gradle",response);
        } catch (PopulatePatternException pe){
            fail();
        }
   }

   @Test
   public void populatePattern_javascript_array_spread_append_object(){
      Map<Object,Object> map = new HashMap();
      map.put("FOO",new Json(true));

      try{
          String response = StringUtil.populatePattern("${{ [...${{FOO}},{'test':'worked'}] }}",map,false,StringUtil.PATTERN_PREFIX,"::",StringUtil.PATTERN_SUFFIX);
          assertEquals("test should be added to an javascript array","[{\"test\":\"worked\"}]",response);
      } catch (PopulatePatternException pe){
          fail();
      }

   }

    @Test
    public void populatePattern_javascript_array_spread(){
       Map<Object,Object> map = new HashMap();
       map.put("alpha", Arrays.asList("\"ant\"","\"apple\""));
       map.put("bravo",Arrays.asList("\"bear\"","\"bull\""));
       map.put("charlie","\"cat\"");

       try {
           String response = StringUtil.populatePattern("${{ [${{charlie}}, ...${{alpha}}, ...${{bravo}} ] }}", map, false);
           assertEquals("expect arrays to be combined", "[\"cat\",\"ant\",\"apple\",\"bear\",\"bull\"]", response);
        } catch (PopulatePatternException pe){
            fail();
        }

}

   /**
    * having { ..${{..}} } inside a ${{}} causes the outer pattern to use the extra } from the inner replacement and close the pattern too soon
    * options:
    *   change the prefix / suffix to not be two of the same character and hope we find a unique character combiniation {[]}
    *   use last instance of suffix?
    *     how do we identify the split between name and default value?
    *       count prefixes?
    *       we have prefix index and index of next prefix, suffix, separator
    *         if prefix > -1 we inc count and look for a separate after dropping count number of suffix?
    */
   @Test
    public void populatePattern_prefix_suffix(){
       Map<Object,Object> map = new HashMap<>();
       map.put("FOO","_");
       map.put("{_}","FOUND");
       try{
           String response  = StringUtil.populatePattern("$<<{$<<FOO>>}>>",map,false,"$<<",":",">>");
           assertEquals("FOUND",response);
       } catch (PopulatePatternException pe){
           fail();
       }

   }
    @Test
    public void populatePattern_javascript_object_spread(){
       Map<Object,Object> map = new HashMap<>();
       Json foo = Json.fromString("{\"foo1\":\"one\",\"foo2\":{\"buz\":\"foo\"}}");
       Json bar = Json.fromString("{\"bar1\":\"one\",\"bar2\":{\"buz\":\"bee\"}}");

       map.put("FOO",foo);
       map.put("BAR",bar);

       try{
           String response  = StringUtil.populatePattern("$<<{...$<<FOO>>}>>",map,false,"$<<",":",">>");
           System.out.println(response);
       } catch (PopulatePatternException pe){
           fail();
       }

    }

    @Test
    public void populatePattern_javascript_lambda_string_literal(){
       Map<Object,Object> map = new HashMap();
       Json list = new Json(true);
       list.add("one");
       list.add("two");
       list.add("three");
       map.put("list",list);
       try{
           String response = StringUtil.populatePattern("${{ ${{list}}.reduce((x,v)=>`${x}\n  ${v}:80`,'').trim()}}",map,false);
           assertEquals("one:80\n  two:80\n  three:80",response);
       } catch (PopulatePatternException pe){
           fail();
       }

    }

    @Test
    public void populatePattern_missing_not_replace(){
       Map<Object,Object> map = new HashMap<>();
       try{
           String response = StringUtil.populatePattern("BAR=${{FOO}}",map,false);

           assertEquals("expect to not replace the pattern","BAR=${{FOO}}",response);
       } catch (PopulatePatternException pe){
           fail();
       }

    }
   @Test
   public void populatePattern_missing_replace(){
      Map<Object,Object> map = new HashMap<>();
      try{
         String response = StringUtil.populatePattern("BAR=${{FOO:}}",map,true);

         assertEquals("expect to not replace the pattern","BAR=",response);
      } catch (PopulatePatternException pe){
         fail(pe.getMessage());
      }

   }

    @Test
    public void populatePattern_arithmetic_missing_value(){
       Map<Object,Object> map = new HashMap<>();
       try{
           String response = StringUtil.populatePattern("${{ 2*MISSING :-1}}",map);
           assertEquals("expected default value when missing value","-1",response);
       } catch (PopulatePatternException pe){
           fail();
       }

    }

    @Test
    public void populatePattern_two_values(){
        Map<Object,Object> map = new HashMap<>();
        map.put("FOO","foo");
        map.put("BAR","bar");
        try{
            String response  = StringUtil.populatePattern("${{FOO}}${{BAR}}",map);
            assertEquals("foobar",response);
        } catch (PopulatePatternException pe){
            fail();
        }

    }
    @Test
    public void populatePattern_default_is_pattern(){
        Map<Object,Object> map = new HashMap<>();
        map.put("FOO","foo");
        map.put("BAR","bar");
        try{
            String response  = StringUtil.populatePattern("${{BIZ:${{FOO}}}}${{BAR}}",map);
            assertEquals("foobar",response);
        } catch (PopulatePatternException pe){
            fail();
        }

    }
    @Test
    public void populatePattern_string_encode_json(){
       Map<Object,Object> map = new HashMap<>();
       map.put("FOO",Json.fromString("{\"foo1\":\"one\",\"foo2\":{\"buz\":\"foo\"}}"));
       map.put("BAR",Json.fromString("{\"bar1\":\"one\",\"bar2\":{\"buz\":\"bee\"}}"));
       try{
           String response  = StringUtil.populatePattern("${{FOO}}",map);

           assertEquals("response should be json encoded",map.get("FOO"),Json.fromString(response));
       } catch (PopulatePatternException pe){
           fail();
       }

    }


    @Test
    public void populatePattern_name_and_default_patterns(){
        Map<Object,Object> map = new HashMap<>();
        map.put("FOO","${{BAR}}");
        map.put("BAR","bar");
        try{
            String response  = StringUtil.populatePattern("${{FOO:${{BIZ:biz}}}}${{BIZ:${{BAR}}}}",map);
            assertEquals("barbar",response);
        } catch (PopulatePatternException pe){
            fail();
        }

    }
    @Test
    public void populatePattern_use_default(){
        Map<Object,Object> map = new HashMap<>();
        map.put("FOO","foo");
        map.put("BAR","bar");
        try{
            String response  = StringUtil.populatePattern("${{FOO}}.${{BAR}}.${{BIZ:biz}}",map);
            assertEquals("foo.bar.biz",response);
        } catch (PopulatePatternException pe){
            fail();
        }

    }
   @Test
   public void populatePattern_use_js_on_value(){
      Map<Object,Object> map = new HashMap<>();
      map.put("FOO","foo");
      try {
          String response = StringUtil.populatePattern("${{\"${{FOO}}\".toUpperCase()}}", map);
          assertEquals("FOO", response);
      } catch (PopulatePatternException pe){
          fail();
      }
   }

    @Test
    public void populatePattern_default_over_empty_value(){
        Map<Object,Object> map = new HashMap<>();
        map.put("FOO","");
        try{
            String response = StringUtil.populatePattern("${{FOO:foo}}",map);
            assertEquals("foo",response);
        } catch (PopulatePatternException pe){
            fail();
        }


    }

    @Test
    public void quoteReplaceWithEscaped(){
        String quoted = StringUtil.quote("foo\"bar");

        assertTrue("starts with quote",quoted.startsWith("\""));
        assertTrue("ends with quote",quoted.endsWith("\""));

        String sripped = quoted.substring(1,quoted.length()-1);

        assertFalse(sripped.matches("\"(?<!\\\\\")"));
    }

    @Test
    public void findNotQuotedSimple(){

        assertEquals("foo\" bar\"",StringUtil.findNotQuoted("foo\" bar\" "," "));
    }
    @Test
    public void findNotQuotedNotFund(){
        assertEquals("12345",StringUtil.findNotQuoted("12345"," "));
    }
    @Test
    public void findNotQuotedFirstChar(){
        assertEquals("",StringUtil.findNotQuoted("12345"," 1"));
    }

    @Test
    public void escapeRegex(){

        assertEquals(". literals","foo\\.bar\\.biz\\.buz", StringUtil.escapeRegex("foo.bar.biz.buz"));
    }
}
