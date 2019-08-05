package perf.yaup;

import io.hyperfoil.tools.yaup.StringUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StringUtilTest {


    @Test
    public void countOccurrances_nonOverlapping(){


        assertEquals("don't let pattern overlap",2,StringUtil.countOccurances("{{{{","{{"));
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
