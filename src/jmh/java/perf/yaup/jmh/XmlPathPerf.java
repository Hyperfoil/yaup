package perf.yaup.jmh;

import org.openjdk.jmh.annotations.*;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class XmlPathPerf {

    /*
    @Param({"/server/profile/subsystem[@xmlns='urn:jboss:domain:deployment-scanner:2.0']"})
    public String path;
    */

    perf.yaup.xml.pojo.Xml pojoXml = perf.yaup.xml.pojo.Xml.parseFile("<todo:addfile/>");
    perf.yaup.xml.Xml nodeXml = new perf.yaup.xml.XmlLoader().loadXml(Paths.get("<todo:addfile/>"));

    @Setup(Level.Iteration)
    public void doSetup(){}

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Object pojoXmlns(){
        List<perf.yaup.xml.pojo.Xml> found = pojoXml.getAll("/server/profile/subsystem[@xmlns='urn:jboss:domain:deployment-scanner:2.0']");
        assert found.size()==1;
        return found;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Object nodeXmlns(){
        List<perf.yaup.xml.Xml> found = nodeXml.getAll("/server/profile/subsystem[starts-with(namespace::*[name()=''],'urn:jboss:domain:deployment-scanner:2.0')]");
        assert found.size()==1;
        return found;

    }
}
