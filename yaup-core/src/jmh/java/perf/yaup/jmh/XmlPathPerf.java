package jmh.java.perf.yaup.jmh;

import io.hyperfoil.tools.yaup.xml.Xml;
import io.hyperfoil.tools.yaup.xml.XmlLoader;
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

    io.hyperfoil.tools.yaup.xml.pojo.Xml pojoXml = io.hyperfoil.tools.yaup.xml.pojo.Xml.parseFile("<todo:addfile/>");
    Xml nodeXml = new XmlLoader().loadXml(Paths.get("<todo:addfile/>"));

    @Setup(Level.Iteration)
    public void doSetup(){}

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Object pojoXmlns(){
        List<io.hyperfoil.tools.yaup.xml.pojo.Xml> found = pojoXml.getAll("/server/profile/subsystem[@xmlns='urn:jboss:domain:deployment-scanner:2.0']");
        assert found.size()==1;
        return found;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Object nodeXmlns(){
        List<Xml> found = nodeXml.getAll("/server/profile/subsystem[starts-with(namespace::*[name()=''],'urn:jboss:domain:deployment-scanner:2.0')]");
        assert found.size()==1;
        return found;

    }
}
