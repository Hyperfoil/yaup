package perf.yaup.xml.pojo;

import io.hyperfoil.tools.yaup.xml.pojo.Xml;
import io.hyperfoil.tools.yaup.xml.pojo.XmlComparison;
import org.junit.Test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class XmlComparisonTest {

    private static final Function<List<XmlComparison.Entry>,String> diffsToString= (diffs)->{
        return diffs.stream()
                .map(e->"\n"+e.getPath()+e.keys().stream().map(key->"\n  "+e.value(key)).collect(Collectors.joining("")))
                .collect(Collectors.joining(""));
    };

    private static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    @Test
    public void diff_missingChild(){
        String xml1="<foo><bar><biz>biz</biz></bar></foo>";
        String xml2="<foo><bar></bar></foo>";

        XmlComparison comparison = new XmlComparison();

        comparison.load("1", Xml.parse(HEADER+xml1));
        comparison.load("2",Xml.parse(HEADER+xml2));

        List<XmlComparison.Entry> diffs = comparison.getDiffs();
        String diffListString = diffsToString.apply(diffs);

        assertEquals("unexpected number of diffs"+
                        diffListString,
                1,
                diffs.size());
        XmlComparison.Entry entry = diffs.get(0);

        assertEquals("path","/foo/bar",entry.getPath());
        assertEquals("child documents for 1","<biz>biz</biz>",entry.value("1"));
        assertEquals("nothing for 2","",entry.value("2"));
    }
    @Test
    public void diff_valueDifference(){
        String xml1="<foo><bar>biz</bar></foo>";
        String xml2="<foo><bar>buz</bar></foo>";

        XmlComparison comparison = new XmlComparison();

        comparison.load("1",Xml.parse(HEADER+xml1));
        comparison.load("2",Xml.parse(HEADER+xml2));

        List<XmlComparison.Entry> diffs = comparison.getDiffs();
        String diffListString = diffsToString.apply(diffs);

        assertEquals("unexpected number of diffs"+
            diffListString,
            1,
            diffs.size());
        XmlComparison.Entry entry = diffs.get(0);

        assertEquals("path"+diffListString,"/foo/bar",entry.getPath());
        assertEquals("child documents for 1"+diffListString,"biz",entry.value("1"));
        assertEquals("child documents for 2"+diffListString,"buz",entry.value("2"));

    }
    @Test
    public void diff_missingAttribute(){
        String xml1="<foo><bar attr=\"value\"></bar></foo>";
        String xml2="<foo><bar></bar></foo>";

        XmlComparison comparison = new XmlComparison();

        comparison.load("1",Xml.parse(HEADER+xml1));
        comparison.load("2",Xml.parse(HEADER+xml2));

        List<XmlComparison.Entry> diffs = comparison.getDiffs();
        String diffListString = diffsToString.apply(diffs);

        assertEquals("unexpected number of diffs"+
                        diffListString,
                1,
                diffs.size());
        XmlComparison.Entry entry = diffs.get(0);

        assertEquals("path"+diffListString,"/foo/bar/@attr",entry.getPath());
        assertEquals("child documents for 1"+diffListString,"value",entry.value("1"));
        assertEquals("child documents for 2"+diffListString,"",entry.value("2"));

    }
    @Test
    public void diff_attributeDifference(){
        String xml1="<foo><bar attr=\"value\"></bar></foo>";
        String xml2="<foo><bar attr=\"attr\"></bar></foo>";

        XmlComparison comparison = new XmlComparison();

        comparison.load("1",Xml.parse(HEADER+xml1));
        comparison.load("2",Xml.parse(HEADER+xml2));

        List<XmlComparison.Entry> diffs = comparison.getDiffs();
        String diffListString = diffsToString.apply(diffs);
        assertEquals("unexpected number of diffs"+
                        diffListString,
                1,
                diffs.size());
        XmlComparison.Entry entry = diffs.get(0);

        assertEquals("path"+diffListString,"/foo/bar/@attr".replaceAll(" ",""),entry.getPath().replaceAll(" ",""));
        assertEquals("child documents for 1"+diffListString,"value",entry.value("1"));
        assertEquals("child documents for 2"+diffListString,"attr",entry.value("2"));

    }
    @Test
    public void criteria_changeOrderOfMatch(){
        String xml1="<foo><bar attr=\"value\">biz</bar><bar attr=\"attr\">buz</bar></foo>";
        String xml2="<foo><bar attr=\"attr\">biz</bar><bar attr=\"value\">buz</bar></foo>";

        XmlComparison comparison = new XmlComparison();
        comparison.addCriteria("@attr",0);

        comparison.load("1",Xml.parse(HEADER+xml1));
        comparison.load("2",Xml.parse(HEADER+xml2));

        List<XmlComparison.Entry> diffs = comparison.getDiffs();
        String diffListString = diffsToString.apply(diffs);

        assertEquals("unexpected number of diffs"+
                diffListString
                ,
                2,
                diffs.size());
        XmlComparison.Entry entry = diffs.get(0);
        assertEquals("path"+diffListString,"/foo/bar[@attr=\"value\"]",entry.getPath().replaceAll(" ",""));
        assertEquals("child documents for 1"+diffListString,"biz",entry.value("1"));
        assertEquals("child documents for 2"+diffListString,"buz",entry.value("2"));
        entry = diffs.get(1);
        assertEquals("path"+diffListString,"/foo/bar[@attr=\"attr\"]",entry.getPath().replaceAll(" ",""));
        assertEquals("child documents for 1"+diffListString,"buz",entry.value("1"));
        assertEquals("child documents for 2"+diffListString,"biz",entry.value("2"));

    }
    @Test
    public void criteria_belowLimit(){
        String xml1="<foo><bar attr=\"value2\">fiz</bar></foo>";
        String xml2="<foo><bar attr=\"value1\">fuz</bar></foo>";

        XmlComparison comparison = new XmlComparison();

        comparison.addCriteria("@attr",1);
        comparison.load("1",Xml.parse(HEADER+xml1));
        comparison.load("2",Xml.parse(HEADER+xml2));

        List<XmlComparison.Entry> diffs = comparison.getDiffs();
        String diffListString = diffsToString.apply(diffs);
        assertEquals("unexpected number of diffs"+
                        diffListString,
                2,
                diffs.size());
        XmlComparison.Entry entry = diffs.get(0);

        assertEquals("path shoudl show starts with"+diffListString,"/foo/bar[ @attr^\"value\" ]/@attr".replaceAll(" ",""),entry.getPath().replaceAll(" ",""));
        assertEquals("child documents for 1"+diffListString,"value2",entry.value("1"));
        assertEquals("child documents for 2"+diffListString,"value1",entry.value("2"));
        entry = diffs.get(1);
        assertEquals("path"+diffListString,"/foo/bar[ @attr^\"value\" ]".replaceAll(" ",""),entry.getPath().replaceAll(" ",""));
        assertEquals("child documents for 1"+diffListString,"fiz",entry.value("1"));
        assertEquals("child documents for 2"+diffListString,"fuz",entry.value("2"));

    }
    @Test
    public void criteria_aboveLimit(){
        String xml1="<foo><bar attr=\"value23\">fiz</bar></foo>";
        String xml2="<foo><bar attr=\"value12\">fuz</bar></foo>";

        XmlComparison comparison = new XmlComparison();

        comparison.addCriteria("@attr",1);
        comparison.load("1",Xml.parse(HEADER+xml1));
        comparison.load("2",Xml.parse(HEADER+xml2));

        List<XmlComparison.Entry> diffs = comparison.getDiffs();
        String diffListString = diffsToString.apply(diffs);
        assertEquals("unexpected number of diffs"+
                        diffListString,
                2,
                diffs.size());
        XmlComparison.Entry entry = diffs.get(0);

        assertEquals("path should show starts with"+diffListString,"/foo".replaceAll(" ",""),entry.getPath().replaceAll(" ",""));
        assertEquals("child documents for 1"+diffListString,"<bar attr=\"value23\">fiz</bar>".replaceAll(" ",""),entry.value("1").replaceAll(" ",""));
        assertEquals("child documents for 2"+diffListString,"",entry.value("2"));
        entry = diffs.get(1);
        assertEquals("path"+diffListString,"/foo".replaceAll(" ",""),entry.getPath().replaceAll(" ",""));
        assertEquals("child documents for 1"+diffListString,"",entry.value("1"));
        assertEquals("child documents for 2"+diffListString,"<bar attr=\"value12\">fuz</bar>".replaceAll(" ",""),entry.value("2").replaceAll(" ",""));

    }
    @Test
    public void criter_ChildContentMatch(){
        String xml1="<foo><bar><biz>biz</biz><buz>one</buz></bar><bar><biz>buz</biz><buz>two</buz></bar></foo>";
        String xml2="<foo><bar><biz>buz</biz><buz>two</buz></bar><bar><biz>biz</biz><buz>one</buz></bar></foo>";

        XmlComparison comparison = new XmlComparison();

        comparison.addCriteria("/bar/biz",0);

        comparison.load("1",Xml.parse(HEADER+xml1));
        comparison.load("2",Xml.parse(HEADER+xml2));


        List<XmlComparison.Entry> diffs = comparison.getDiffs();
        String diffListString = diffsToString.apply(diffs);
        assertEquals("unexpected number of diffs"+
                        diffListString,
                0,
                diffs.size());
    }
    @Test
    public void criter_ChildContentDiff(){
        String xml1="<foo><bar><biz>biz</biz><buz>one</buz></bar><bar><biz>buz</biz><buz>two</buz></bar></foo>";
        String xml2="<foo><bar><biz>buz</biz><buz>one</buz></bar><bar><biz>biz</biz><buz>one</buz></bar></foo>";

        XmlComparison comparison = new XmlComparison();

        comparison.addCriteria("/bar/biz",0);


        comparison.load("1",Xml.parse(HEADER+xml1));
        comparison.load("2",Xml.parse(HEADER+xml2));

        List<XmlComparison.Entry> diffs = comparison.getDiffs();
        String diffListString = diffsToString.apply(diffs);
        assertEquals("unexpected number of diffs"+
                        diffListString,
                1,
                diffs.size());
        XmlComparison.Entry entry = diffs.get(0);

        assertEquals("path"+diffListString,"/foo/bar[biz=\"buz\"]/buz".replaceAll(" ",""),entry.getPath().replaceAll(" ",""));
        assertEquals("child documents for 1"+diffListString,"two",entry.value("1"));
        assertEquals("child documents for 2"+diffListString,"one",entry.value("2"));
    }

    @Test
    public void test() {
        String xml1 =
                "            <subsystem xmlns=\"urn:jboss:domain:datasources:4.0\">\n" +
                        "                <datasources>\n" +
                        "                    <datasource jndi-name=\"java:jboss/datasources/ExampleDS\" pool-name=\"ExampleDS\" enabled=\"true\" use-java-context=\"true\">\n" +
                        "                        <connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE</connection-url>\n" +
                        "                        <driver>h2</driver>\n" +
                        "                        <security>\n" +
                        "                            <user-name>sa</user-name>\n" +
                        "                            <password>sa</password>\n" +
                        "                        </security>\n" +
                        "                    </datasource>\n" +
                        "                    <datasource jndi-name=\"java:/jdbc/SPECjSupplierDS\" pool-name=\"SPECjSupplierNonXADS\" enabled=\"true\" connectable=\"true\">\n" +
                        "                        <connection-url>jdbc:postgresql://benchserver3G2:5433/specdb</connection-url>\n" +
                        "                        <driver>postgresql</driver>\n" +
                        "                        <connection-property name=\"tcpKeepAlive\">true</connection-property>\n" +
                        "                            <connection-property name=\"logLevel\">0</connection-property>\n" +
                        "                            <connection-property name=\"disableColumnSanitiser\">true</connection-property>\n" +
                        "                        <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>\n" +
                        "                        <pool>\n" +
                        "                            <min-pool-size>25</min-pool-size>\n" +
                        "                            <max-pool-size>25</max-pool-size>\n" +
                        "                            <prefill>true</prefill>\n" +
                        "                        </pool>\n" +
                        "                        <security>\n" +
                        "                            <user-name>specdb</user-name>\n" +
                        "                            <password>specdb</password>\n" +
                        "                        </security>\n" +
                        "                        <validation>\n" +
                        "                            <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\"/>\n" +
                        "                            <validate-on-match>false</validate-on-match>\n" +
                        "                            <background-validation>true</background-validation>\n" +
                        "                            <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\"/>\n" +
                        "                        </validation>\n" +
                        "                        <timeout>\n" +
                        "                            <blocking-timeout-millis>20000</blocking-timeout-millis>\n" +
                        "                            <idle-timeout-minutes>120</idle-timeout-minutes>\n" +
                        "                        </timeout>\n" +
                        "                        <statement>\n" +
                        "                            <track-statements>false</track-statements>\n" +
                        "                            <prepared-statement-cache-size>64</prepared-statement-cache-size>\n" +
                        "                            <share-prepared-statements>true</share-prepared-statements>\n" +
                        "                        </statement>\n" +
                        "                    </datasource>\n" +
                        "                    <datasource jndi-name=\"java:/jdbc/SPECjOrderDS\" pool-name=\"SPECjOrderNonXADS\" enabled=\"true\" connectable=\"true\">\n" +
                        "                        <connection-url>jdbc:postgresql://benchserver3G1:5432/specdb</connection-url>\n" +
                        "                        <driver>postgresql</driver>\n" +
                        "                        <connection-property name=\"tcpKeepAlive\">true</connection-property>\n" +
                        "                        <connection-property name=\"logLevel\">0</connection-property>\n" +
                        "                        <connection-property name=\"disableColumnSanitiser\">true</connection-property>\n" +
                        "                         <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>\n" +
                        "                        <pool>\n" +
                        "                            <min-pool-size>29</min-pool-size>\n" +
                        "                            <max-pool-size>29</max-pool-size>\n" +
                        "                            <prefill>true</prefill>\n" +
                        "                        </pool>\n" +
                        "                        <security>\n" +
                        "                            <user-name>specdb</user-name>\n" +
                        "                            <password>specdb</password>\n" +
                        "                        </security>\n" +
                        "                        <validation>\n" +
                        "                            <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\"/>\n" +
                        "                            <validate-on-match>false</validate-on-match>\n" +
                        "                            <background-validation>true</background-validation>\n" +
                        "                            <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\"/>\n" +
                        "                        </validation>\n" +
                        "                        <timeout>\n" +
                        "                            <blocking-timeout-millis>20000</blocking-timeout-millis>\n" +
                        "                            <idle-timeout-minutes>120</idle-timeout-minutes>\n" +
                        "                        </timeout>\n" +
                        "                        <statement>\n" +
                        "                            <track-statements>false</track-statements>\n" +
                        "                            <prepared-statement-cache-size>64</prepared-statement-cache-size>\n" +
                        "                            <share-prepared-statements>true</share-prepared-statements>\n" +
                        "                        </statement>\n" +
                        "                    </datasource>\n" +
                        "                    <datasource jndi-name=\"java:/jdbc/SPECjMfgDS\" pool-name=\"SPECjMfgNonXADS\" enabled=\"true\" connectable=\"true\">\n" +
                        "                        <connection-url>jdbc:postgresql://benchserver3G2:5433/specdb</connection-url>\n" +
                        "                        <driver>postgresql</driver>\n" +
                        "                        <connection-property name=\"tcpKeepAlive\">true</connection-property>\n" +
                        "                         <connection-property name=\"logLevel\">0</connection-property>\n" +
                        "                        <connection-property name=\"disableColumnSanitiser\">true</connection-property>\n" +
                        "                         <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>\n" +
                        "                        <pool>\n" +
                        "                            <min-pool-size>36</min-pool-size>\n" +
                        "                            <max-pool-size>36</max-pool-size>\n" +
                        "                            <prefill>true</prefill>\n" +
                        "                        </pool>\n" +
                        "                        <security>\n" +
                        "                            <user-name>specdb</user-name>\n" +
                        "                            <password>specdb</password>\n" +
                        "                        </security>\n" +
                        "                        <validation>\n" +
                        "                            <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\"/>\n" +
                        "                            <validate-on-match>false</validate-on-match>\n" +
                        "                            <background-validation>true</background-validation>\n" +
                        "                            <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\"/>\n" +
                        "                        </validation>\n" +
                        "                        <timeout>\n" +
                        "                            <blocking-timeout-millis>20000</blocking-timeout-millis>\n" +
                        "                            <idle-timeout-minutes>120</idle-timeout-minutes>\n" +
                        "                        </timeout>\n" +
                        "                        <statement>\n" +
                        "                            <track-statements>false</track-statements>\n" +
                        "                            <prepared-statement-cache-size>64</prepared-statement-cache-size>\n" +
                        "                            <share-prepared-statements>true</share-prepared-statements>\n" +
                        "                        </statement>\n" +
                        "                    </datasource>\n" +
                        "                    <xa-datasource jndi-name=\"java:/jdbc/SPECjOrderXADS\" pool-name=\"SPECjOrderDS\" enabled=\"false\" use-ccm=\"false\" mcp=\"org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreConcurrentLinkedDequeManagedConnectionPool\" enlistment-trace=\"false\" statistics-enabled=\"true\">\n" +
                        "                        <xa-datasource-property name=\"ServerName\">\n" +
                        "                            benchserver3G1\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"PortNumber\">\n" +
                        "                            5432\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"DatabaseName\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"User\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"Password\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"tcpKeepAlive\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"logLevel\">\n" +
                        "                            0\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"disableColumnSanitiser\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <driver>postgresql-xa</driver>\n" +
                        "                        <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>\n" +
                        "                        <xa-pool>\n" +
                        "                            <min-pool-size>35</min-pool-size>\n" +
                        "                            <max-pool-size>35</max-pool-size>\n" +
                        "                            <prefill>true</prefill>\n" +
                        "                            <fair>false</fair>\n" +
                        "                        </xa-pool>\n" +
                        "                        <security>\n" +
                        "                            <user-name>specdb</user-name>\n" +
                        "                            <password>specdb</password>\n" +
                        "                        </security>\n" +
                        "                        <validation>\n" +
                        "                            <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\"/>\n" +
                        "                            <validate-on-match>false</validate-on-match>\n" +
                        "                            <background-validation>true</background-validation>\n" +
                        "                            <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\"/>\n" +
                        "                        </validation>\n" +
                        "                        <timeout>\n" +
                        "                            <idle-timeout-minutes>120</idle-timeout-minutes>\n" +
                        "                        </timeout>\n" +
                        "                        <statement>\n" +
                        "                            <track-statements>FALSE</track-statements>\n" +
                        "                            <prepared-statement-cache-size>64</prepared-statement-cache-size>\n" +
                        "                            <share-prepared-statements>true</share-prepared-statements>\n" +
                        "                        </statement>\n" +
                        "                    </xa-datasource>\n" +
                        "                    <xa-datasource jndi-name=\"java:/jdbc/SPECjMfgXADS\" pool-name=\"SPECjMfgDS\" use-ccm=\"false\" enabled=\"false\" mcp=\"org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreConcurrentLinkedDequeManagedConnectionPool\" enlistment-trace=\"false\" statistics-enabled=\"true\">\n" +
                        "                        <xa-datasource-property name=\"ServerName\">\n" +
                        "                            benchserver3G2\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"PortNumber\">\n" +
                        "                            5433\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"DatabaseName\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"User\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"Password\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"tcpKeepAlive\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"disableColumnSanitiser\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <driver>postgresql-xa</driver>\n" +
                        "                        <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>\n" +
                        "                        <xa-pool>\n" +
                        "                            <min-pool-size>30</min-pool-size>\n" +
                        "                            <max-pool-size>30</max-pool-size>\n" +
                        "                            <prefill>true</prefill>\n" +
                        "                            <fair>false</fair>\n" +
                        "                        </xa-pool>\n" +
                        "                        <security>\n" +
                        "                            <user-name>specdb</user-name>\n" +
                        "                            <password>specdb</password>\n" +
                        "                        </security>\n" +
                        "                        <validation>\n" +
                        "                            <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\"/>\n" +
                        "                            <validate-on-match>false</validate-on-match>\n" +
                        "                            <background-validation>true</background-validation>\n" +
                        "                            <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\"/>\n" +
                        "                        </validation>\n" +
                        "                        <timeout>\n" +
                        "                            <blocking-timeout-millis>20000</blocking-timeout-millis>\n" +
                        "                            <idle-timeout-minutes>120</idle-timeout-minutes>\n" +
                        "                        </timeout>\n" +
                        "                        <statement>\n" +
                        "                            <track-statements>FALSE</track-statements>\n" +
                        "                            <prepared-statement-cache-size>64</prepared-statement-cache-size>\n" +
                        "                            <share-prepared-statements>true</share-prepared-statements>\n" +
                        "                        </statement>\n" +
                        "                    </xa-datasource>\n" +
                        "                    <xa-datasource jndi-name=\"java:/jdbc/SPECjSupplierDS\" pool-name=\"SPECjSupplierXADS\" use-ccm=\"false\" enabled=\"false\" mcp=\"org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreConcurrentLinkedDequeManagedConnectionPool\" enlistment-trace=\"false\" statistics-enabled=\"true\">\n" +
                        "                        <xa-datasource-property name=\"ServerName\">\n" +
                        "                            benchserver3G2\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"PortNumber\">\n" +
                        "                            5433\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"DatabaseName\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"User\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"Password\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"tcpKeepAlive\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"logLevel\">\n" +
                        "                            0\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"disableColumnSanitiser\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <driver>postgresql-xa</driver>\n" +
                        "                        <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>\n" +
                        "                        <xa-pool>\n" +
                        "                            <min-pool-size>19</min-pool-size>\n" +
                        "                            <max-pool-size>19</max-pool-size>\n" +
                        "                            <prefill>true</prefill>\n" +
                        "                            <fair>false</fair>\n" +
                        "                        </xa-pool>\n" +
                        "                        <security>\n" +
                        "                            <user-name>specdb</user-name>\n" +
                        "                            <password>specdb</password>\n" +
                        "                        </security>\n" +
                        "                        <validation>\n" +
                        "                            <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\"/>\n" +
                        "                            <validate-on-match>false</validate-on-match>\n" +
                        "                            <background-validation>true</background-validation>\n" +
                        "                            <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\"/>\n" +
                        "                        </validation>\n" +
                        "                        <timeout>\n" +
                        "                            <idle-timeout-minutes>120</idle-timeout-minutes>\n" +
                        "                        </timeout>\n" +
                        "                        <statement>\n" +
                        "                            <track-statements>FALSE</track-statements>\n" +
                        "                            <prepared-statement-cache-size>64</prepared-statement-cache-size>\n" +
                        "                            <share-prepared-statements>true</share-prepared-statements>\n" +
                        "                        </statement>\n" +
                        "                    </xa-datasource>\n" +
                        "                    <xa-datasource jndi-name=\"java:/jdbc/SPECjLoaderDS\" pool-name=\"SPECjLoaderDS\" use-ccm=\"false\" mcp=\"org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreConcurrentLinkedDequeManagedConnectionPool\" enlistment-trace=\"false\">\n" +
                        "                        <xa-datasource-property name=\"ServerName\">\n" +
                        "                            benchserver3G1\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"PortNumber\">\n" +
                        "                            5432\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"DatabaseName\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"User\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"Password\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"tcpKeepAlive\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <driver>postgresql-xa</driver>\n" +
                        "                        <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>\n" +
                        "                        <xa-pool>\n" +
                        "                            <min-pool-size>1</min-pool-size>\n" +
                        "                            <max-pool-size>1</max-pool-size>\n" +
                        "                            <prefill>true</prefill>\n" +
                        "                            <fair>false</fair>\n" +
                        "                        </xa-pool>\n" +
                        "                        <security>\n" +
                        "                            <user-name>specdb</user-name>\n" +
                        "                            <password>specdb</password>\n" +
                        "                        </security>\n" +
                        "                        <validation>\n" +
                        "                            <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\"/>\n" +
                        "                            <validate-on-match>false</validate-on-match>\n" +
                        "                            <background-validation>true</background-validation>\n" +
                        "                            <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\"/>\n" +
                        "                        </validation>\n" +
                        "                        <timeout>\n" +
                        "                            <idle-timeout-minutes>120</idle-timeout-minutes>\n" +
                        "                        </timeout>\n" +
                        "                        <statement>\n" +
                        "                            <track-statements>FALSE</track-statements>\n" +
                        "                            <prepared-statement-cache-size>64</prepared-statement-cache-size>\n" +
                        "                            <share-prepared-statements>true</share-prepared-statements>\n" +
                        "                        </statement>\n" +
                        "                    </xa-datasource>\n" +
                        "                    <drivers>\n" +
                        "                        <driver name=\"h2\" module=\"com.h2database.h2\">\n" +
                        "                            <xa-datasource-class>org.h2.jdbcx.JdbcDataSource</xa-datasource-class>\n" +
                        "                        </driver>\n" +
                        "                        <driver name=\"postgresql\" module=\"org.postgresql\">\n" +
                        "                            <driver-class>org.postgresql.Driver</driver-class>\n" +
                        "                        </driver>\n" +
                        "                        <driver name=\"postgresql-xa\" module=\"org.postgresql\">\n" +
                        "                            <xa-datasource-class>org.postgresql.xa.PGXADataSource</xa-datasource-class>\n" +
                        "                        </driver>\n" +
                        "                    </drivers>\n" +
                        "                </datasources>\n" +
                        "            </subsystem>\n";
        String xml2 =
                "            <subsystem xmlns=\"urn:jboss:domain:datasources:4.0\">\n" +
                        "                <datasources>\n" +
                        "                    <datasource jndi-name=\"java:jboss/datasources/ExampleDS\" pool-name=\"ExampleDS\" enabled=\"true\" use-java-context=\"true\">\n" +
                        "                        <connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE</connection-url>\n" +
                        "                        <driver>h2</driver>\n" +
                        "                        <security>\n" +
                        "                            <user-name>sa</user-name>\n" +
                        "                            <password>sa</password>\n" +
                        "                        </security>\n" +
                        "                    </datasource>\n" +
                        "                    <xa-datasource jndi-name=\"java:/jdbc/SPECjOrderDS\" pool-name=\"SPECjOrderDS\" use-ccm=\"false\" mcp=\"org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreConcurrentLinkedDequeManagedConnectionPool\" enlistment-trace=\"false\" statistics-enabled=\"true\">\n" +
                        "                        <xa-datasource-property name=\"ServerName\">\n" +
                        "                            w520\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"PortNumber\">\n" +
                        "                            5432\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"DatabaseName\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"User\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"Password\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"tcpKeepAlive\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"logLevel\">\n" +
                        "                            0\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"disableColumnSanitiser\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <driver>postgresql-xa</driver>\n" +
                        "                        <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>\n" +
                        "                        <xa-pool>\n" +
                        "                            <min-pool-size>10</min-pool-size>\n" +
                        "                            <max-pool-size>75</max-pool-size>\n" +
                        "                            <prefill>true</prefill>\n" +
                        "                        </xa-pool>\n" +
                        "                        <security>\n" +
                        "                            <user-name>specdb</user-name>\n" +
                        "                            <password>specdb</password>\n" +
                        "                        </security>\n" +
                        "                        <validation>\n" +
                        "                            <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\"/>\n" +
                        "                            <validate-on-match>false</validate-on-match>\n" +
                        "                            <background-validation>true</background-validation>\n" +
                        "                            <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\"/>\n" +
                        "                        </validation>\n" +
                        "                        <timeout>\n" +
                        "                            <idle-timeout-minutes>120</idle-timeout-minutes>\n" +
                        "                        </timeout>\n" +
                        "                        <statement>\n" +
                        "                            <track-statements>FALSE</track-statements>\n" +
                        "                            <prepared-statement-cache-size>64</prepared-statement-cache-size>\n" +
                        "                            <share-prepared-statements>true</share-prepared-statements>\n" +
                        "                        </statement>\n" +
                        "                    </xa-datasource>\n" +
                        "                    <xa-datasource jndi-name=\"java:/jdbc/SPECjMfgDS\" pool-name=\"SPECjMfgDS\" use-ccm=\"false\" mcp=\"org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreConcurrentLinkedDequeManagedConnectionPool\" enlistment-trace=\"false\" statistics-enabled=\"true\">\n" +
                        "                        <xa-datasource-property name=\"ServerName\">\n" +
                        "                            w520\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"PortNumber\">\n" +
                        "                            5432\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"DatabaseName\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"User\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"Password\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"tcpKeepAlive\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"disableColumnSanitiser\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <driver>postgresql-xa</driver>\n" +
                        "                        <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>\n" +
                        "                        <xa-pool>\n" +
                        "                            <min-pool-size>10</min-pool-size>\n" +
                        "                            <max-pool-size>75</max-pool-size>\n" +
                        "                            <prefill>true</prefill>\n" +
                        "                        </xa-pool>\n" +
                        "                        <security>\n" +
                        "                            <user-name>specdb</user-name>\n" +
                        "                            <password>specdb</password>\n" +
                        "                        </security>\n" +
                        "                        <validation>\n" +
                        "                            <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\"/>\n" +
                        "                            <validate-on-match>false</validate-on-match>\n" +
                        "                            <background-validation>true</background-validation>\n" +
                        "                            <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\"/>\n" +
                        "                        </validation>\n" +
                        "                        <timeout>\n" +
                        "                            <blocking-timeout-millis>20000</blocking-timeout-millis>\n" +
                        "                            <idle-timeout-minutes>120</idle-timeout-minutes>\n" +
                        "                        </timeout>\n" +
                        "                        <statement>\n" +
                        "                            <track-statements>FALSE</track-statements>\n" +
                        "                            <prepared-statement-cache-size>64</prepared-statement-cache-size>\n" +
                        "                            <share-prepared-statements>true</share-prepared-statements>\n" +
                        "                        </statement>\n" +
                        "                    </xa-datasource>\n" +
                        "                    <xa-datasource jndi-name=\"java:/jdbc/SPECjSupplierDS\" pool-name=\"SPECjSupplierDS\" use-ccm=\"false\" mcp=\"org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreConcurrentLinkedDequeManagedConnectionPool\" enlistment-trace=\"false\" statistics-enabled=\"true\">\n" +
                        "                        <xa-datasource-property name=\"ServerName\">\n" +
                        "                            w520\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"PortNumber\">\n" +
                        "                            5432\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"DatabaseName\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"User\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"Password\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"tcpKeepAlive\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"logLevel\">\n" +
                        "                            0\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"disableColumnSanitiser\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <driver>postgresql-xa</driver>\n" +
                        "                        <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>\n" +
                        "                        <xa-pool>\n" +
                        "                            <min-pool-size>10</min-pool-size>\n" +
                        "                            <max-pool-size>40</max-pool-size>\n" +
                        "                            <prefill>true</prefill>\n" +
                        "                        </xa-pool>\n" +
                        "                        <security>\n" +
                        "                            <user-name>specdb</user-name>\n" +
                        "                            <password>specdb</password>\n" +
                        "                        </security>\n" +
                        "                        <validation>\n" +
                        "                            <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\"/>\n" +
                        "                            <validate-on-match>false</validate-on-match>\n" +
                        "                            <background-validation>true</background-validation>\n" +
                        "                            <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\"/>\n" +
                        "                        </validation>\n" +
                        "                        <timeout>\n" +
                        "                            <idle-timeout-minutes>120</idle-timeout-minutes>\n" +
                        "                        </timeout>\n" +
                        "                        <statement>\n" +
                        "                            <track-statements>FALSE</track-statements>\n" +
                        "                            <prepared-statement-cache-size>64</prepared-statement-cache-size>\n" +
                        "                            <share-prepared-statements>true</share-prepared-statements>\n" +
                        "                        </statement>\n" +
                        "                    </xa-datasource>\n" +
                        "                    <xa-datasource jndi-name=\"java:/jdbc/SPECjLoaderDS\" pool-name=\"SPECjLoaderDS\" use-ccm=\"false\" mcp=\"org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreConcurrentLinkedDequeManagedConnectionPool\" enlistment-trace=\"false\">\n" +
                        "                        <xa-datasource-property name=\"ServerName\">\n" +
                        "                            w520\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"PortNumber\">\n" +
                        "                            5432\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"DatabaseName\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"User\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"Password\">\n" +
                        "                            specdb\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <xa-datasource-property name=\"tcpKeepAlive\">\n" +
                        "                            true\n" +
                        "                        </xa-datasource-property>\n" +
                        "                        <driver>postgresql-xa</driver>\n" +
                        "                        <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>\n" +
                        "                        <xa-pool>\n" +
                        "                            <min-pool-size>1</min-pool-size>\n" +
                        "                            <max-pool-size>1</max-pool-size>\n" +
                        "                            <prefill>true</prefill>\n" +
                        "                        </xa-pool>\n" +
                        "                        <security>\n" +
                        "                            <user-name>specdb</user-name>\n" +
                        "                            <password>specdb</password>\n" +
                        "                        </security>\n" +
                        "                        <validation>\n" +
                        "                            <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\"/>\n" +
                        "                            <validate-on-match>false</validate-on-match>\n" +
                        "                            <background-validation>true</background-validation>\n" +
                        "                            <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\"/>\n" +
                        "                        </validation>\n" +
                        "                        <timeout>\n" +
                        "                            <idle-timeout-minutes>120</idle-timeout-minutes>\n" +
                        "                        </timeout>\n" +
                        "                        <statement>\n" +
                        "                            <track-statements>FALSE</track-statements>\n" +
                        "                            <prepared-statement-cache-size>64</prepared-statement-cache-size>\n" +
                        "                            <share-prepared-statements>true</share-prepared-statements>\n" +
                        "                        </statement>\n" +
                        "                    </xa-datasource>\n" +
                        "                    <drivers>\n" +
                        "                        <driver name=\"h2\" module=\"com.h2database.h2\">\n" +
                        "                            <xa-datasource-class>org.h2.jdbcx.JdbcDataSource</xa-datasource-class>\n" +
                        "                        </driver>\n" +
                        "                        <driver name=\"postgresql\" module=\"org.postgresql\">\n" +
                        "                            <driver-class>org.postgresql.Driver</driver-class>\n" +
                        "                        </driver>\n" +
                        "                        <driver name=\"postgresql-xa\" module=\"org.postgresql\">\n" +
                        "                            <xa-datasource-class>org.postgresql.xa.PGXADataSource</xa-datasource-class>\n" +
                        "                        </driver>\n" +
                        "                    </drivers>\n" +
                        "                </datasources>\n" +
                        "            </subsystem>";

        XmlComparison diff = new XmlComparison();

        diff.addCriteria("@name", 0);
        diff.addCriteria("@jndi-name", 0);
        diff.addCriteria("@xmlns", 3);
        diff.addCriteria("@module", 0);
        diff.addCriteria("@category", 0);

        diff.load("L", Xml.parse(xml1));
        diff.load("R", Xml.parse(xml2));

        List<XmlComparison.Entry> diffs = diff.getDiffs();

//        diffs.forEach(entry -> {
//            System.out.println(entry.getPath());
//            for (String key : entry.keys()) {
//                System.out.println("  " + key + "  " + entry.value(key));
//            }
//        });

    }
}