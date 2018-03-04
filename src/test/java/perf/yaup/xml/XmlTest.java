package perf.yaup.xml;

import org.junit.Test;

public class XmlTest {

    @Test
    public void toStringTest(){
        XmlLoader loader = new XmlLoader();

        Xml loaded = loader.loadXml("<foo><bar>one</bar></foo>");
        System.out.println(loaded.toString());
    }


    @Test
    public void optChild(){
        XmlLoader loader = new XmlLoader();

        Xml loaded = loader.loadXml("<foo><bar>one</bar></foo>");
        Xml bar = loaded.optChild("bar").orElse(null);
        System.out.println(bar);
    }
}
