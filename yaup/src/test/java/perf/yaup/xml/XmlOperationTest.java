package perf.yaup.xml;

import io.hyperfoil.tools.yaup.xml.Xml;
import io.hyperfoil.tools.yaup.xml.XmlLoader;
import io.hyperfoil.tools.yaup.xml.XmlOperation;
import org.junit.Test;
import io.hyperfoil.tools.yaup.file.FileUtility;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class XmlOperationTest {

    @Test
    public void parse_read(){
        XmlOperation operation = XmlOperation.parse("/foo/bar");
        assertTrue("expect read",operation.isRead());
        assertEquals("path","/foo/bar",operation.getPath());
        assertFalse("no value",operation.hasValue());
    }
    @Test
    public void parse_set(){
        XmlOperation operation = XmlOperation.parse("/foo/bar "+ FileUtility.SET_OPERATION+" bizz");
        assertTrue("expect set",operation.isSet());
        assertEquals("path","/foo/bar",operation.getPath());
        assertEquals("value","bizz",operation.getValue());
    }
    @Test
    public void parse_add(){
        XmlOperation operation = XmlOperation.parse("/foo/bar "+ FileUtility.ADD_OPERATION+" @key = bizz");
        assertTrue("expect add",operation.isAdd());
        assertEquals("path","/foo/bar",operation.getPath());
        assertEquals("value","@key = bizz",operation.getValue());
    }

    @Test
    public void apply_concat(){
        Xml xml = new XmlLoader().loadXml(
            """
            <foo>
                <bar><biz>one</biz><buz>two</buz></bar>
                <bar><biz>uno</biz><buz>dos</buz></bar>
            </foo>
            """
        );
        XmlOperation op = XmlOperation.parse("concat(/foo/bar[1]/biz/text(),' ',/foo/bar[1]/buz/text())");
        String response = op.apply(xml);
        assertEquals("one two",response);
    }

    @Test
    public void apply_read_tag_text_jenkins_path(){
        Xml xml = new XmlLoader().loadXml(
            """
            <flow-build>
                <actions>
                    <hudson.model.CauseAction>
                        <causeBag>
                            <entry>
                                <hudson.model.Cause_-UserIdCause>
                                    <userId>found</userId>
                                </hudson.model.Cause_-UserIdCause>
                            </entry>
                            <entry>
                                <hudson.model.Cause_-UserIdCause>
                                    <userId>wrong</userId>
                                </hudson.model.Cause_-UserIdCause>
                            </entry>
                        </causeBag>
                    </hudson.model.CauseAction>
                </actions>
            </flow-build>
            """
        );

        XmlOperation op = XmlOperation.parse("/flow-build/actions/hudson.model.CauseAction/causeBag/entry[1]/hudson.model.Cause_-UserIdCause/userId/text()");
        String response = op.apply(xml);

        assertEquals("found",response);
    }
    @Test
    public void apply_read_tag_text(){
        String xmlContent = "<foo><bar>bizz</bar></foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);

        XmlOperation op = XmlOperation.parse("/foo/bar");
        String response = op.apply(xml);

        assertEquals("bizz",response);
    }
    @Test
    public void apply_read_attribute(){
        String xmlContent = "<foo><bar key=\"value\">bizz</bar></foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);

        XmlOperation op = XmlOperation.parse("/foo/bar/@key");
        String response = op.apply(xml);

        assertEquals("value",response);
    }
    @Test
    public void apply_set_attribute(){
        String xmlContent = "<foo><bar key=\"value\">bizz</bar></foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);


        XmlOperation op = XmlOperation.parse("/foo/bar/@key "+FileUtility.SET_OPERATION+" changed");
        op.apply(xml);

        assertFalse(xml.firstChild("bar").attribute("key").isEmpty());
        assertEquals("changed",xml.firstChild("bar").attribute("key").toString());
    }
    @Test
    public void apply_add_attribute(){
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
    public void apply_add_child(){
        String xmlContent = "<foo><bar>bizz</bar></foo>";

        Xml xml = new XmlLoader().loadXml(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar "+FileUtility.ADD_OPERATION+" <buzz>fuzz</buzz>");
        assertTrue(op.isAdd());
        op.apply(xml);

        assertTrue("should contain 'buzz':\n"+xml.documentString(4),xml.documentString().contains("buzz"));
        assertFalse(xml.firstChild("bar").firstChild("buzz").isEmpty());
        assertEquals("fuzz",xml.firstChild("bar").firstChild("buzz").toString());
    }
    @Test
    public void apply_set_missing_attribute(){
        String xmlContent = "<foo>bizz</foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/@key "+FileUtility.SET_OPERATION+" value with space");
        op.apply(xml);
        assertFalse("should add path's @key to value when missing",xml.attribute("key").isEmpty());
        assertEquals("value with space",xml.attribute("key").toString());
    }
    @Test
    public void appply_set_missing_tag(){
        String xmlContent = "<foo>fizz</foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar "+FileUtility.SET_OPERATION+" buzz");
        op.apply(xml);

    }
    @Test
    public void apply_set_missing_tag_with_attribute(){
        String xmlContent = "<foo>fizz</foo>";
        Xml xml = new XmlLoader().loadXml(xmlContent);
        XmlOperation op = XmlOperation.parse("/foo/bar[@key='value' and @set] "+FileUtility.SET_OPERATION+" <hi>mom</hi>");
        op.apply(xml);
        assertFalse(xml.firstChild("bar").isEmpty());
        assertEquals("value",xml.firstChild("bar").attribute("key").toString());
        assertFalse(xml.firstChild("bar").attribute("set").isEmpty());

    }
    @Test
    public void replace_xmlns_attribute(){
        String search = "/foo[@xmlns=urn:foo:bar:biz]/bar[@xmlns:biz=foo:biz:buz]";
        String replaced = XmlOperation.replaceXmlnsAttribute(search);

        assertFalse("should not find @xmlns but found: "+replaced,replaced.contains("@xmlns"));
        assertEquals("shoudl replace both references",
                "/foo[starts-with(namespace::*[name()=\"\"],\"urn:foo:bar:biz\")]/bar[starts-with(namespace::*[name()=\"biz\"],\"foo:biz:buz\")]",
                replaced);
    }


    @Test
    public void last_path_index(){

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
