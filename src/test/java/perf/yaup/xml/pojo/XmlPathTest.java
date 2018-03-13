package perf.yaup.xml.pojo;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XmlPathTest {


    @Test @Ignore
    public void parseChildIndex(){
        XmlPath path = XmlPath.parse("/foo/[2]");
        System.out.println(path);
    }
    @Test @Ignore
    public void parseTagIndex(){
        XmlPath path = XmlPath.parse("/foo/bar[2]");
        System.out.println(path);
    }

    @Test
    public void parsePath(){
        XmlPath path = XmlPath.parse("/foo/bar");

        assertTrue(path.hasNext());
        path = path.getNext();
        assertEquals("first path","foo",path.getName());
        assertTrue("tag matching",path.getType().equals(XmlPath.Type.Tag));
        assertFalse("should not be deep scanning",path.isDescendant());
        assertTrue(path.hasNext());

        path = path.getNext();
        assertEquals("second path","bar",path.getName());
        assertTrue("tag matching",path.getType().equals(XmlPath.Type.Tag));
        assertFalse("should not be deep scanning",path.isDescendant());
        assertFalse(path.hasNext());
    }
    @Test
    public void parsePathWithAttribute(){
        XmlPath path = XmlPath.parse("/foo/@bar");
        assertTrue(path.hasNext());
        path = path.getNext();
        assertEquals("first path","foo",path.getName());
        assertTrue("tag matching",path.getType().equals(XmlPath.Type.Tag));
        assertFalse("should not be deep scanning",path.isDescendant());
        assertTrue(path.hasNext());
        path = path.getNext();
        assertEquals("second path","bar",path.getName());
        assertTrue("attribute matching: "+path,path.getType().equals(XmlPath.Type.Attribute));
        assertFalse("should not be deep scanning",path.isDescendant());
        assertFalse(path.hasNext());

    }

    @Test
    public void parseAttributeCriteria(){
        XmlPath path = XmlPath.parse("/foo[@name=\"value\" and @key=\"bar\"]");
        assertTrue(path.hasNext());
        path = path.getNext();
        assertEquals("first path","foo",path.getName());
        assertTrue("tag matching",path.getType().equals(XmlPath.Type.Tag));
        assertFalse("should not be deep scanning",path.isDescendant());
        List<XmlPath> children = path.getChildren();
        assertEquals("foo should have 2 children",2,children.size());

        XmlPath firstCriteria = children.get(0).getNext();
        assertEquals("name",firstCriteria.getName());
        assertEquals("value",firstCriteria.getValue());
        assertTrue("@name is an attribute: "+firstCriteria,firstCriteria.getType().equals(XmlPath.Type.Attribute));

        XmlPath secondCriteria = children.get(1).getNext();
        assertEquals("key",secondCriteria.getName());
        assertEquals("bar",secondCriteria.getValue());
        assertTrue("@key is an attribute: "+secondCriteria,secondCriteria.getType().equals(XmlPath.Type.Attribute));
    }

    @Test
    public void parsePathCriteria(){
        XmlPath path = XmlPath.parse("/foo[/bar/biz = \"fun\"]");
        assertTrue(path.hasNext());
        path = path.getNext();
        assertEquals("first path","foo",path.getName());
        List<XmlPath> children = path.getChildren();
        assertEquals("foo should have 1 child",1,children.size());

        XmlPath firstCriteria = children.get(0);
        assertTrue(firstCriteria.hasNext());
        firstCriteria = firstCriteria.getNext();
        assertEquals("bar",firstCriteria.getName());
        assertTrue("/bar/biz is a tag: "+firstCriteria,firstCriteria.getType().equals(XmlPath.Type.Tag));
        assertTrue(firstCriteria.hasNext());
        firstCriteria = firstCriteria.getNext();
        assertEquals("biz",firstCriteria.getName());
        assertEquals("fun",firstCriteria.getValue());

    }
    @Test
    public void parseTwoPathCriteria(){
        XmlPath path = XmlPath.parse("/foo[ /bar = \"fun\" and /buz[@value=\"bee\"] ]");
        assertTrue(path.hasNext());
        path = path.getNext();
        assertEquals("first path","foo",path.getName());
        List<XmlPath> children = path.getChildren();
        assertEquals("foo should have 2 children "+path.getChildren(),2,children.size());

        XmlPath bar = children.get(0);
        assertTrue(bar.hasNext());
        bar = bar.getNext();
        assertEquals("bar",bar.getName());
        assertEquals("fun",bar.getValue());

        XmlPath buz = children.get(1);
        assertTrue(buz.hasNext());
        buz = buz.getNext();
        assertEquals("buz",buz.getName());
        assertTrue(!buz.getChildren().isEmpty());

        XmlPath value = buz.getChildren().get(0);

        assertTrue("value is Start and has a next path",value.hasNext());
        value = value.getNext();

        assertEquals("value",value.getName());
        assertEquals("bee",value.getValue());
        assertTrue(value.getType().equals(XmlPath.Type.Attribute));

    }


    @Test
    public void matchTags(){
        Xml xml = Xml.parse("<foo><bar>biz</bar><bar>buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar");
        List<Xml> matches = path.getMatches(xml);

        assertEquals("should find 2",2,matches.size());
    }

    @Test
    public void matchAttribute(){
        Xml xml = Xml.parse("<foo><bar>biz</bar><bar key=\"one\">buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar/@key");
        List<Xml> matches = path.getMatches(xml);

        assertEquals("should find 1",1,matches.size());
        assertEquals("result name should be key","key",matches.get(0).getName());
        assertEquals("result value should be one","one",matches.get(0).getValue());

    }

    @Test
    public void matchAttributeEquals(){
        Xml xml = Xml.parse("<foo><bar>biz</bar><bar key=\"one\">buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar[@key='one']");
        List<Xml> matches = path.getMatches(xml);

        assertEquals("should find 1",1,matches.size());
        assertEquals("result name should be bar","bar",matches.get(0).getName());
        assertEquals("result value should be buz","buz",matches.get(0).getValue());

    }
    @Test
    public void matchAttributeStartsWith(){
        Xml xml = Xml.parse("<foo><bar>biz</bar><bar key=\"one\">buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar[@key^'one']");
        List<Xml> matches = path.getMatches(xml);

        assertEquals("should find 1",1,matches.size());
        assertEquals("result name should be bar","bar",matches.get(0).getName());
        assertEquals("result value should be buz","buz",matches.get(0).getValue());

    }

    @Test
    public void matchTagTextEquals(){
        Xml xml = Xml.parse("<foo><bar key=\"one\">biz</bar><bar key=\"two\">buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar = 'biz'");

        List<Xml> matches = path.getMatches(xml);
        assertEquals("should find 1",1,matches.size());
        assertEquals("result name should be bar","bar",matches.get(0).getName());
        assertEquals("result name should be one","one",matches.get(0).attribute("key").getValue());

    }
    @Test
    public void matchTagTextStartsWith(){
        Xml xml = Xml.parse("<foo><bar key=\"one\">biz</bar><bar key=\"two\">buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar ^ 'bi'");
        List<Xml> matches = path.getMatches(xml);
        assertEquals("should find 1",1,matches.size());
        assertEquals("result name should be bar","bar",matches.get(0).getName());
        assertEquals("result value should be biz","biz",matches.get(0).getValue());
        assertEquals("result name should be one","one",matches.get(0).attribute("key").getValue());
    }

    @Test
    public void childIndex(){
        Xml xml = Xml.parse("<foo><bar key=\"1\"><buz key=\"1\"/><buz key=\"2\"/></bar><bar key=\"2\"><buz key=\"1\"/><buz key=\"2\"/></bar></foo>");

        XmlPath path = XmlPath.parse("/foo/bar/buz");
        List<Xml> matches = path.getMatches(xml);

        System.out.println(matches);

    }

}
