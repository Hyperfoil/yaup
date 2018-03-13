package perf.yaup.xml;

import org.junit.Ignore;
import org.junit.Test;

public class XmlTest {

    @Test @Ignore
    public void toStringTest(){
        XmlLoader loader = new XmlLoader();
        Xml loaded = loader.loadXml("<foo><bar>one</bar></foo>");
    }


    @Test @Ignore
    public void optChild(){
        XmlLoader loader = new XmlLoader();

        Xml loaded = loader.loadXml("<foo><bar>one</bar></foo>");
        Xml bar = loaded.optChild("bar").orElse(null);

    }
}
