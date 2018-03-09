package perf.yaup.xml;

import org.junit.Test;
import perf.yaup.file.FileUtility;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class XmlOperationTest {

    @Test
    public void parseRead(){
        XmlOperation operation = XmlOperation.parse("/foo/bar");
        assertTrue("expect read",operation.isRead());
        assertEquals("path","/foo/bar",operation.getPath());
        assertFalse("no value",operation.hasValue());
    }
    @Test
    public void parseSet(){
        XmlOperation operation = XmlOperation.parse("/foo/bar "+ FileUtility.SET_OPERATION+" bizz");
        assertTrue("expect set",operation.isSet());
        assertEquals("path","/foo/bar",operation.getPath());
        assertEquals("value","bizz",operation.getValue());
    }
    @Test
    public void parseAdd(){
        XmlOperation operation = XmlOperation.parse("/foo/bar "+ FileUtility.ADD_OPERATION+" @key = bizz");
        assertTrue("expect add",operation.isAdd());
        assertEquals("path","/foo/bar",operation.getPath());
        assertEquals("value","@key = bizz",operation.getValue());
    }
    @Test
    public void applyReadTagText(){
        String xmlContent = "<foo><bar>bizz</bar></foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);

        XmlOperation op = XmlOperation.parse("/foo/bar");
        String response = op.apply(xml);

        assertEquals("bizz",response);
    }
    @Test
    public void applyReadAttribute(){
        String xmlContent = "<foo><bar key=\"value\">bizz</bar></foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);

        XmlOperation op = XmlOperation.parse("/foo/bar/@key");
        String response = op.apply(xml);

        assertEquals("value",response);
    }
    @Test
    public void applySetAttribute(){
        String xmlContent = "<foo><bar key=\"value\">bizz</bar></foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);


        XmlOperation op = XmlOperation.parse("/foo/bar/@key "+FileUtility.SET_OPERATION+" changed");
        op.apply(xml);

        assertFalse(xml.firstChild("bar").attribute("key").isEmpty());
        assertEquals("changed",xml.firstChild("bar").attribute("key").toString());
    }
    @Test
    public void applyAddAttribute(){
        String xmlContent = "<foo></foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);

        XmlOperation op = XmlOperation.parse("/foo "+FileUtility.ADD_OPERATION+" @key = changed");
        op.apply(xml);

        assertFalse("attribute added:\n"+xml.documentString(4),
                xml.attribute("key").isEmpty()
        );
        assertEquals("attribute value","changed",xml.attribute("key").toString());
    }
    @Test
    public void applyAddChild(){
        String xmlContent = "<foo><bar>bizz</bar></foo>";

        Xml xml = new XmlLoader().loadXml(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar "+FileUtility.ADD_OPERATION+" <buzz>fuzz</buzz>");
        op.apply(xml);

        assertTrue("should contain 'buzz':\n"+xml.documentString(4),xml.documentString().contains("buzz"));
        assertFalse(xml.firstChild("bar").firstChild("buzz").isEmpty());
        assertEquals("fuzz",xml.firstChild("bar").firstChild("buzz").toString());
    }
    @Test
    public void applySetMissingAttribute(){
        String xmlContent = "<foo>bizz</foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/@key "+FileUtility.SET_OPERATION+" value");
        op.apply(xml);
        assertFalse("should add path's @key to value when missing",xml.attribute("key").isEmpty());
        assertEquals("value",xml.attribute("key").toString());
    }
    @Test
    public void appplySetMissingTag(){
        String xmlContent = "<foo>fizz</foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar "+FileUtility.SET_OPERATION+" buzz");
        op.apply(xml);
        System.out.println(xml.documentString(2));
    }
    @Test
    public void applySetMissingTagWithAttribute(){
        String xmlContent = "<foo>fizz</foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar[@key='value' and @set] "+FileUtility.SET_OPERATION+" <hi>mom</hi>");
        op.apply(xml);
        System.out.println(xml.documentString(2));
        assertFalse(xml.firstChild("bar").isEmpty());
        assertEquals("value",xml.firstChild("bar").attribute("key").toString());
        assertFalse(xml.firstChild("bar").attribute("set").isEmpty());

    }
    @Test
    public void replaceXmlnsAttribute(){
        String search = "/foo[@xmlns=urn:foo:bar:biz]/bar[@xmlns:biz=foo:biz:buz]";

        String replaced = XmlOperation.replaceXmlnsAttribute(search);



        assertFalse("should not find @xmlns but found: "+replaced,replaced.contains("@xmlns"));
        assertEquals("shoudl replace both references",
                "/foo[starts-with(namespace::*[name()=\"\"]=\"urn:foo:bar:biz\"]/bar[starts-with(namespace::*[name()=\"biz\"]=\"foo:biz:buz\"]",
                replaced);
    }


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
