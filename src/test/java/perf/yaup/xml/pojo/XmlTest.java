package perf.yaup.xml.pojo;

import org.junit.Test;
import perf.yaup.file.FileUtility;
import perf.yaup.xml.XmlOperation;

import java.util.List;

import static org.junit.Assert.*;

public class XmlTest {

    @Test
    public void apply_readTagText(){
        String xmlContent = "<foo><bar>bizz</bar></foo>";
        Xml xml = Xml.parse(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar");
        String response = xml.apply(op);

        assertEquals("should read value of tag","bizz",response);
    }

    @Test
    public void apply_readAttribute(){
        String xmlContent = "<foo><bar key=\"value\">bizz</bar></foo>";
        Xml xml = Xml.parse(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar/@key");
        String response = xml.apply(op);

        assertEquals("value",response);
    }
    @Test
    public void apply_setAttribute(){
        String xmlContent = "<foo><bar key=\"value\">bizz</bar></foo>";
        Xml xml = Xml.parse(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar/@key "+ FileUtility.SET_OPERATION+" changed");
        String response = xml.apply(op);
        assertTrue(xml.firstChild("bar").attribute("key").exists());
        assertEquals("changed",xml.firstChild("bar").attribute("key").getValue());
        assertEquals("changed",xml.firstChild("foo").firstChild("bar").attribute("key").getValue());

    }
    @Test
    public void apply_addAttribute(){
        String xmlContent = "<foo></foo>";
        Xml xml = Xml.parse(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo "+FileUtility.ADD_OPERATION+" @key = changed");
        String response = xml.apply(op);
        assertTrue("attribute added:\n"+xml.documentString(),
                xml.attribute("key").exists()
        );
        assertEquals("attribute value","changed",xml.attribute("key").getValue());
    }

    @Test
    public void apply_addChild(){
        String xmlContent = "<foo><bar>bizz</bar></foo>";
        Xml xml = Xml.parse(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar "+FileUtility.ADD_OPERATION+" <buzz>fuzz</buzz>");
        String response = xml.apply(op);

        assertTrue("should contain 'buzz':\n"+xml.documentString(4),xml.documentString().contains("buzz"));
        assertTrue("bar should have buzz child:\n"+xml.documentString(4),xml.firstChild("bar").firstChild("buzz").exists());
        assertEquals("fuzz",xml.firstChild("bar").firstChild("buzz").getValue());

    }

    @Test
    public void apply_setMissingAttribute(){
        String xmlContent = "<foo>bizz</foo>";
        Xml xml = Xml.parse(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/@key "+FileUtility.SET_OPERATION+" value with space");
        String response = xml.apply(op);

        assertTrue("should add path's @key to value when missing\n"+xml.documentString(),xml.attribute("key").exists());
        assertEquals("value with space",xml.attribute("key").getValue());

    }

    @Test
    public void apply_setMissingTag(){
        String xmlContent = "<foo>fizz</foo>";
        Xml xml = Xml.parse(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar "+FileUtility.SET_OPERATION+" buzz");
        String response = xml.apply(op);

        assertTrue("xml should now have bar child",xml.firstChild("foo").firstChild("bar").exists());
        assertEquals("expect bar to have text content","buzz",xml.firstChild("foo").firstChild("bar").getValue());
    }

    @Test
    public void appply_setMissingTagWithAttribute(){
        String xmlContent = "<foo>fizz</foo>";
        Xml xml = Xml.parse(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar[@key='value' and @set] "+FileUtility.SET_OPERATION+" <hi>mom</hi>");
        String response = xml.apply(op);

        assertTrue(xml.firstChild("bar").exists());
        assertEquals("value",xml.firstChild("bar").attribute("key").getValue());
        assertTrue(xml.firstChild("bar").attribute("set").exists());

    }

    @Test
    public void getAll_nameWithNamespace(){
        String xmlContent = "<foo xmlns:fa=\"http://faban.sunsource.net/ns/faban\"><fa:bar>bizz</fa:bar></foo>";
        Xml xml = Xml.parse(xmlContent);
        List<Xml> found = xml.getAll("/foo/fa:bar");

        assertEquals("should find 1 entry:\n"+found,1,found.size());
        assertEquals("bar should contain bizz","bizz",found.get(0).getValue());
    }
    @Test
    public void get_attribute(){
        String xmlContent = "<foo bar=\"bar\"></foo>";
        Xml xml = Xml.parse(xmlContent);
        Xml found = xml.get("@bar");
        assertTrue("@bar exists",found.exists());
        assertEquals("bar",found.getValue());
    }
    @Test
    public void get_tag_get_attribute(){
        String xmlContent = "<foo><bar bar=\"bar\"></bar></foo>";
        Xml xml = Xml.parse(xmlContent);
        Xml foo = xml.get("foo");
        System.out.println("foo.isDocument="+foo.isDocument());
        Xml bar = foo.get("bar/@bar");
        System.out.println(foo.exists()+" "+foo.documentString(0));
        System.out.println(bar.exists()+" "+bar.documentString(0));

    }
}
