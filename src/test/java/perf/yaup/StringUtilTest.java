package perf.yaup;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringUtilTest {

    @Test
    public void escapeRegex(){

        assertEquals(". literals","foo\\.bar\\.biz\\.buz",StringUtil.escapeRegex("foo.bar.biz.buz"));
    }
}
