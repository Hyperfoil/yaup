package perf.yaup.xml.pojo;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class XmlPathTest {

    @Test
    public void parse_reveredCriteria(){ // Not supported
        XmlPath path = XmlPath.parse("/foo/bar[text() = 'fiz' and 'buz' = text()]");
        assertTrue(XmlPath.Type.Undefined.equals(path.getType()));
        XmlPath work = XmlPath.parse("/foo/bar[text() = 'fiz' and text() = 'buz']");
        System.out.println(work);
    }

    @Test
    public void parse_nestedCriteria(){
        XmlPath path = XmlPath.parse("/foo[/bar[/biz = 'buz']]");
        System.out.println(path);
    }

    @Test
    public void parse_function(){
        XmlPath path = XmlPath.parse("/text()");
        assertTrue("xmlpaths still start with start",XmlPath.Type.Start.equals(path.getType()));
        assertTrue(path.hasNext());
        path = path.getNext();
        assertTrue(XmlPath.Type.Function.equals(path.getType()));
    }

    @Test
    public void parse_descendant(){
        XmlPath path = XmlPath.parse("//foo");
        assertTrue("xmlpaths still start with start",XmlPath.Type.Start.equals(path.getType()));
        assertTrue(path.hasNext());
        path = path.getNext();
        assertTrue(path.isDescendant());
    }
    @Test
    public void parse_secondDescendant(){
        XmlPath path = XmlPath.parse("/foo//bar");
        assertTrue("xmlpaths still start with start",XmlPath.Type.Start.equals(path.getType()));
        assertTrue(path.hasNext());
        path = path.getNext();
        assertTrue(path.hasNext());
        path = path.getNext();
        System.out.println(path.toString(false));
        assertTrue(path.isDescendant());
        assertEquals("bar",path.getName());
    }


    @Test @Ignore
    public void match_childIndex(){
        XmlPath path = XmlPath.parse("/foo/[1]");
        Xml xml = Xml.parse("<foo><first/><second/><third/></foo>");
        System.out.println(path);

        List<Xml> matches = path.getMatches(xml);

        System.out.println(matches);

        //assertEquals("/[#] is not a valid xpath",XmlPath.Type.Undefined,path.getType());
    }
    @Test
    public void match_tagIndex(){
        XmlPath path = XmlPath.parse("/foo/bar[1]");
        Xml xml = Xml.parse("<foo><biz/><bar>one</bar><bar>two</bar><bar>three</bar></foo>");
        System.out.println(path);
        List<Xml> matches = path.getMatches(xml);

        matches.forEach(match->{
            System.out.println(match.documentString(2));
        });

    }
    @Test
    public void match_position(){
        XmlPath path = XmlPath.parse("/foo/bar[/position() > 0 and /position() < 3]");
        Xml xml = Xml.parse("<foo><biz/><bar>one</bar><bar>two</bar><bar>three</bar></foo>");
        System.out.println(path);
        List<Xml> matches = path.getMatches(xml);
        matches.forEach(match->{
            System.out.println(match.documentString(2));
        });

    }

    @Test
    public void parse_path(){
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
    public void parse_pathWithAttribute(){
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
    public void parse_attributeCriteria(){
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
    public void parse_pathCriteria(){
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
    public void parse_twoPathCriteria(){
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
    public void getMatches_textFunctionCriteria(){
        Xml xml = Xml.parse("<foo><bar>biz<f/>buz<f/>fuz</bar><bar>buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar[text()='buz']");
        System.out.println(path);
        List<Xml> matches = path.getMatches(xml);
        System.out.println(matches);
        assertEquals("match count\n"+matches.toString(),1,matches.size());
        Xml m = matches.get(0);
        assertNotNull("has parent"+m,m.parent());
        assertEquals("value "+m,"buz",m.getValue());
    }
    @Test
    public void getMatches_textFunction(){
        Xml xml = Xml.parse("<foo><bar>biz<f/>buz<f/>fuz</bar><bar>buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar/text()");
        List<Xml> matches = path.getMatches(xml);
        assertEquals("match count\n"+matches.toString(),2,matches.size());
        Xml m = matches.get(0);
        assertTrue("expect text",m.isText());
        assertNull("no parent"+m,m.parent());
        assertEquals("value "+m,"bizbuzfuz",m.getValue());
        m = matches.get(1);
        assertTrue("expect text",m.isText());
        assertNull("no parent"+m,m.parent());
        assertEquals("value "+m,"buz",m.getValue());

    }


    @Test
    public void getMatches_tag(){
        Xml xml = Xml.parse("<foo><bar>biz</bar><bar>buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar");
        List<Xml> matches = path.getMatches(xml);

        assertEquals("should find 2",2,matches.size());
    }

    @Test
    public void getMatches_attribute(){
        Xml xml = Xml.parse("<foo><bar>biz</bar><bar key=\"one\">buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar/@key");
        List<Xml> matches = path.getMatches(xml);

        assertEquals("should find 1",1,matches.size());
        assertEquals("result name should be key","key",matches.get(0).getName());
        assertEquals("result value should be one","one",matches.get(0).getValue());

    }

    @Test
    public void getMatches_attributeEquals(){
        Xml xml = Xml.parse("<foo><bar>biz</bar><bar key=\"one\">buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar[@key='one']");
        List<Xml> matches = path.getMatches(xml);

        assertEquals("should find 1",1,matches.size());
        assertEquals("result name should be bar","bar",matches.get(0).getName());
        assertEquals("result value should be buz","buz",matches.get(0).getValue());

    }
    @Test
    public void getMatches_attributeStartsWith(){
        Xml xml = Xml.parse("<foo><bar>biz</bar><bar key=\"one\">buz</bar></foo>");
        XmlPath path = XmlPath.parse("/foo/bar[@key^'one']");
        List<Xml> matches = path.getMatches(xml);

        assertEquals("should find 1",1,matches.size());
        assertEquals("result name should be bar","bar",matches.get(0).getName());
        assertEquals("result value should be buz","buz",matches.get(0).getValue());

    }

    @Test
    public void getMatches_tagTextEquals(){
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

    @Test @Ignore
    public void childIndex(){
        Xml xml = Xml.parse(
                "<foo>" +
                "   <bar key=\"1\">" +
                "       <buz key=\"1\"/>" +
                "       <buz key=\"2\"/>" +
                "   </bar>" +
                "   <bar key=\"2\">" +
                "       <buz key=\"1\"/>" +
                "       <buz key=\"2\"/>" +
                "   </bar>" +
                "</foo>");
        XmlPath path = XmlPath.parse("/foo/bar/buz");
        List<Xml> matches = path.getMatches(xml);
        assertEquals("/foo/bar/buz should match 4 entries:\n"+xml.documentString(2),4,matches.size());
    }

    @Test
    public void descendantSearch(){
        Xml xml = Xml.parse("<a><b><c key='1'/><c key='2'/></b><b><c key='1'/><c key='2'/></b></a>");

        XmlPath path = XmlPath.parse("//c");
        List<Xml> found = path.getMatches(xml);
        assertEquals("should find all 4 c's",4,found.size());

        path = XmlPath.parse("a//c");
        found = path.getMatches(xml);
        assertEquals("should find all 4 c's",4,found.size());

    }

}
