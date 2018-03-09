package perf.yaup.xml;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class XmlOperationTest {

    @Test
    public void lastPathIndex(){

        assertEquals("attribute",9, XmlOperation.lastPathIndex("/foo/bar/@foo"));
        assertEquals("fragment",9,XmlOperation.lastPathIndex("/foo/bar/foo"));
        assertEquals("single fragment",1,XmlOperation.lastPathIndex("/foo"));
        assertEquals("relative fragment",-1,XmlOperation.lastPathIndex("foo"));
        assertEquals("with criteria",
                5,
                XmlOperation.lastPathIndex("/foo/foo[@bar]")
        );
        assertEquals("with path criteria",
                5,
                XmlOperation.lastPathIndex("/foo/foo[bar/biz]"));
    }

}
