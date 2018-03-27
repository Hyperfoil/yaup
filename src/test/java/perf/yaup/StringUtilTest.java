package perf.yaup;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {


    @Test
    public void countOccurrances_nonOverlapping(){


        assertEquals("don't let pattern overlap",2,StringUtil.countOccurances("{{{{","{{"));
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

        assertEquals(". literals","foo\\.bar\\.biz\\.buz",StringUtil.escapeRegex("foo.bar.biz.buz"));
    }
}
