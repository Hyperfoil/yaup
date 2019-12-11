package perf.yaup;

import io.hyperfoil.tools.yaup.HashedList;
import io.hyperfoil.tools.yaup.HashedLists;
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
    public void populatePattern_javascript_array_spread(){
       Map<Object,Object> map = new HashMap();
       map.put("alpha", Arrays.asList("\"ant\"","\"apple\""));
       map.put("bravo",Arrays.asList("\"bear\"","\"bull\""));
       map.put("charlie","\"cat\"");

       String response = StringUtil.populatePattern("${{ [${{charlie}}, ...${{alpha}}, ...${{bravo}} ] }}",map,false);
       assertEquals("expect arrays to be combined","[\"cat\",\"ant\",\"apple\",\"bear\",\"bull\"]",response);
    }

    @Test
    public void populatePattern_javascript_lambda_string_literal(){
       Map<Object,Object> map = new HashMap();
       Json list = new Json(true);
       list.add("one");
       list.add("two");
       list.add("three");
       map.put("list",list);
       String response = StringUtil.populatePattern("${{ ${{list}}.reduce((x,v)=>`${x}\n  ${v}:80`,'').trim()}}",map,false);
       assertEquals("one:80\n  two:80\n  three:80",response);
    }

    @Test
    public void populatePattern_missing_not_replace(){
       Map<Object,Object> map = new HashMap<>();
       String response = StringUtil.populatePattern("${{FOO}}",map,false);

       assertEquals("expect to not replace the pattern","${{FOO}}",response);
    }

    @Test
    public void populatePattern_arithmetic_missing_value(){
       Map<Object,Object> map = new HashMap<>();
       String response = StringUtil.populatePattern("${{ 2*MISSING :-1}}",map);
       assertEquals("expected default value when missing value","-1",response);
    }

    @Test
    public void populatePattern_two_values(){
        Map<Object,Object> map = new HashMap<>();
        map.put("FOO","foo");
        map.put("BAR","bar");
        String response  = StringUtil.populatePattern("${{FOO}}${{BAR}}",map);
        assertEquals("foobar",response);
    }
    @Test
    public void populatePattern_default_is_pattern(){
        Map<Object,Object> map = new HashMap<>();
        map.put("FOO","foo");
        map.put("BAR","bar");
        String response  = StringUtil.populatePattern("${{BIZ:${{FOO}}}}${{BAR}}",map);
        assertEquals("foobar",response);
    }
    @Test
    public void populatePattern_name_and_default_patterns(){
        Map<Object,Object> map = new HashMap<>();
        map.put("FOO","${{BAR}}");
        map.put("BAR","bar");
        String response  = StringUtil.populatePattern("${{FOO:${{BIZ:biz}}}}${{BIZ:${{BAR}}}}",map);
        assertEquals("barbar",response);
    }
    @Test
    public void populatePattern_use_default(){
        Map<Object,Object> map = new HashMap<>();
        map.put("FOO","foo");
        map.put("BAR","bar");
        String response  = StringUtil.populatePattern("${{FOO}}.${{BAR}}.${{BIZ:biz}}",map);
        assertEquals("foo.bar.biz",response);
    }
   @Test
   public void populatePattern_use_js_on_value(){
      Map<Object,Object> map = new HashMap<>();
      map.put("FOO","foo");
      String response  = StringUtil.populatePattern("${{\"${{FOO}}\".toUpperCase()}}",map);
      assertEquals("FOO",response);
   }

    @Test
    public void populatePattern_default_over_empty_value(){
        Map<Object,Object> map = new HashMap<>();
        map.put("FOO","");
        String response = StringUtil.populatePattern("${{FOO:foo}}",map);
        assertEquals("foo",response);

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
