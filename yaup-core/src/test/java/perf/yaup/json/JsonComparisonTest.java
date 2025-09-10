package perf.yaup.json;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.JsonComparison;
import org.junit.Test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonComparisonTest {

    private static final Function<List<JsonComparison.Entry>,String> diffsToString = (diffs)->{
        return diffs.stream()
                .map(e->"\n"+e.getPath()+e.keys().stream().map(key->"\n  "+key+" : "+e.value(key)).collect(Collectors.joining("")))
                .collect(Collectors.joining(""));

    };

    @Test
    public void array_same(){
        Json a = Json.fromString("['uno','dos']");
        Json b = Json.fromString("['uno','dos']");

        JsonComparison comparison = new JsonComparison();

        comparison.load("1",a);
        comparison.load("2",b);

        List<JsonComparison.Entry> diffs = comparison.getDiffs();

        assertEquals("diffs should be empty\n"+diffsToString.apply(diffs),0,diffs.size());
    }
    @Test
    public void object_same(){
        Json a = Json.fromString("{one:'uno',two:'dos'}");
        Json b = Json.fromString("{one:'uno',two:'dos'}");

        JsonComparison comparison = new JsonComparison();

        comparison.load("1",a);
        comparison.load("2",b);

        List<JsonComparison.Entry> diffs = comparison.getDiffs();

        assertEquals("diffs should be empty\n"+diffsToString.apply(diffs),0,diffs.size());
    }

    @Test
    public void array_diff_ordered_entry_value(){
        Json a = Json.fromString("['uno','dos']");
        Json b = Json.fromString("['uno','two']");

        JsonComparison comparison = new JsonComparison();
        comparison.load("1",a);
        comparison.load("2",b);

        List<JsonComparison.Entry> diffs = comparison.getDiffs();
        assertEquals("expect 1 diff\n"+diffsToString.apply(diffs),1,diffs.size());

        JsonComparison.Entry diff = diffs.get(0);
        assertEquals("path","$[1]",diff.getPath());
        assertEquals("value count",2,diff.keys().size());
        assertEquals("diff[1]","dos",diff.value("1"));
        assertEquals("diff[2]","two",diff.value("2"));
    }
    @Test
    public void object_diff_value(){
        Json a = Json.fromString("{one:'uno',two:'dos'}");
        Json b = Json.fromString("{one:'uno',two:'two'}");

        JsonComparison comparison = new JsonComparison();

        comparison.load("1",a);
        comparison.load("2",b);

        List<JsonComparison.Entry> diffs = comparison.getDiffs();
        assertEquals("expect 1 diff\n"+diffsToString.apply(diffs),1,diffs.size());

        JsonComparison.Entry diff = diffs.get(0);
        assertEquals("path","$.two",diff.getPath());
        assertEquals("value count",2,diff.keys().size());
        assertEquals("diff[1]","dos",diff.value("1"));
        assertEquals("diff[2]","two",diff.value("2"));
    }

    @Test
    public void array_diff_ordered_missing_entry(){
        Json a = Json.fromString("['uno','dos']");
        Json b = Json.fromString("['uno']");

        JsonComparison comparison = new JsonComparison();
        comparison.load("1",a);
        comparison.load("2",b);

        List<JsonComparison.Entry> diffs = comparison.getDiffs();
        assertEquals("expect 1 diff\n"+diffsToString.apply(diffs),1,diffs.size());

        JsonComparison.Entry diff = diffs.get(0);
        assertEquals("path","$[1]",diff.getPath());
        assertEquals("value count",1,diff.keys().size());
        assertEquals("diff[1]","dos",diff.value("1"));

    }
    @Test
    public void object_diff_missing_key(){
        Json a = Json.fromString("{one:'uno',two:'dos'}");
        Json b = Json.fromString("{one:'uno'}");

        JsonComparison comparison = new JsonComparison();

        comparison.load("1",a);
        comparison.load("2",b);

        List<JsonComparison.Entry> diffs = comparison.getDiffs();
        assertEquals("expect 1 diff\n"+diffsToString.apply(diffs),1,diffs.size());

        JsonComparison.Entry diff = diffs.get(0);
        assertEquals("path","$.two",diff.getPath());
        assertEquals("value count",1,diff.keys().size());
        assertEquals("diff[1]","dos",diff.value("1"));
    }
    @Test
    public void array_diff_ordered_extra_entry(){
        Json a = Json.fromString("['uno']");
        Json b = Json.fromString("['uno','dos']");

        JsonComparison comparison = new JsonComparison();
        comparison.load("1",a);
        comparison.load("2",b);

        List<JsonComparison.Entry> diffs = comparison.getDiffs();
        assertEquals("expect 1 diff\n"+diffsToString.apply(diffs),1,diffs.size());

        JsonComparison.Entry diff = diffs.get(0);
        assertEquals("path","$[1]",diff.getPath());
        assertEquals("value count",1,diff.keys().size());
        assertEquals("diff[1]","dos",diff.value("2"));
    }


    @Test
    public void object_diff_extra_key(){
        Json a = Json.fromString("{one:'uno'}");
        Json b = Json.fromString("{one:'uno',two:'two'}");

        JsonComparison comparison = new JsonComparison();

        comparison.load("1",a);
        comparison.load("2",b);

        List<JsonComparison.Entry> diffs = comparison.getDiffs();
        assertEquals("expect 1 diff\n"+diffsToString.apply(diffs),1,diffs.size());

        JsonComparison.Entry diff = diffs.get(0);
        assertEquals("path","$.two",diff.getPath());
        assertEquals("value count",1,diff.keys().size());
        assertEquals("diff[2]","two",diff.value("2"));
    }

    @Test
    public void object_diff_nested_value(){
        Json a = Json.fromString("{one:{uno:true}}");
        Json b = Json.fromString("{one:{uno:false}}");

        JsonComparison comparison = new JsonComparison();

        comparison.load("1",a);
        comparison.load("2",b);

        List<JsonComparison.Entry> diffs = comparison.getDiffs();
        assertEquals("expect 1 diff\n"+diffsToString.apply(diffs),1,diffs.size());

        JsonComparison.Entry diff = diffs.get(0);
        assertEquals("path","$.one.uno",diff.getPath());
        assertEquals("value count",2,diff.keys().size());
        assertEquals("diff[1]","true",diff.value("1"));
        assertEquals("diff[2]","false",diff.value("2"));

    }
    @Test
    public void object_diff_nested_value_type(){
        Json a = Json.fromString("{one:{uno:true}}");
        Json b = Json.fromString("{one:{uno:{found:false}}}");

        JsonComparison comparison = new JsonComparison();

        comparison.load("1",a);
        comparison.load("2",b);

        List<JsonComparison.Entry> diffs = comparison.getDiffs();
        assertEquals("expect 1 diff\n"+diffsToString.apply(diffs),1,diffs.size());

        JsonComparison.Entry diff = diffs.get(0);
        assertEquals("path","$.one.uno",diff.getPath());
        assertEquals("value count",2,diff.keys().size());
        assertEquals("diff[1]","true",diff.value("1"));
        assertEquals("diff[2]",Json.fromString("{found:false}").toString(),diff.value("2"));

    }
    @Test
    public void array_diff_entry_key(){
        Json a = Json.fromString("['uno',{dos:'two'}]");
        Json b = Json.fromString("['uno',{dos:false}]");

        JsonComparison comparison = new JsonComparison();
        comparison.load("1",a);
        comparison.load("2",b);

        List<JsonComparison.Entry> diffs = comparison.getDiffs();
        assertEquals("expect 1 diff\n"+diffsToString.apply(diffs),1,diffs.size());

        JsonComparison.Entry diff = diffs.get(0);
        assertEquals("path","$[1].dos",diff.getPath());
        assertEquals("value count",2,diff.keys().size());
        assertEquals("diff[1]","two",diff.value("1"));
        assertEquals("diff[2]","false",diff.value("2"));
    }
}
