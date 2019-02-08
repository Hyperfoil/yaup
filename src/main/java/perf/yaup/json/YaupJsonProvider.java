package perf.yaup.json;

import com.jayway.jsonpath.*;
import com.jayway.jsonpath.spi.json.JsonProvider;
import perf.yaup.StringUtil;
import perf.yaup.file.FileUtility;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class YaupJsonProvider implements JsonProvider {

    static {
        System.setProperty("com.jayway.jsonpath.internal.path.CompiledPath.level", "ERROR");
    }

    @Override
    public Object parse(String s) throws InvalidJsonException {
        return Json.fromString(s);
    }

    @Override
    public Object parse(InputStream inputStream, String s) throws InvalidJsonException {
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        String result = scanner.hasNext() ? scanner.next() : "";
        return Json.fromString(result);
    }

    @Override
    public String toJson(Object o) {
        return o instanceof Json ? ((Json)o).toString() : "";
    }

    @Override
    public Object createArray() {
        return new Json(true);
    }

    @Override
    public Object createMap() {
        return new Json(false);
    }

    @Override
    public boolean isArray(Object o) {
        return o != null && o instanceof Json && ((Json)o).isArray();
    }

    @Override
    public int length(Object o) {
        if(o instanceof Json){
            return ((Json)o).size();
        }else if (o instanceof String){
            return ((String)o).length();
        }else{
            throw new JsonPathException("cannot get length of "+o);
        }

    }

    @Override
    public Iterable<?> toIterable(Object o) {
        if(o instanceof Json  && ((Json)o).isArray()){
            return ((Json)o).values();
        } else {
            throw new JsonPathException("cannot iterate over " + o);
        }
    }

    @Override
    public Collection<String> getPropertyKeys(Object o) {
        if (isMap(o)) {
            return ((Json)o).keys().stream().map(Object::toString).collect(Collectors.toList());
        }else{
            throw new JsonPathException("cannot get keys of "+o);
        }

    }

    @Override
    public Object getArrayIndex(Object o, int i) {
        return o instanceof Json ? ((Json)o).get(i) : JsonProvider.UNDEFINED;
    }

    @Override
    public Object getArrayIndex(Object o, int i, boolean b) {
        return getArrayIndex(o,i);
    }

    @Override
    public void setArrayIndex(Object o, int i, Object o1) {
        if(o instanceof Json){
            Json json = (Json)o;
            if(json.isArray()){
                while(json.size()<=i){
                    json.add(new Json(false));
                }
                json.set(i,o1);
            }
        }
    }

    @Override
    public Object getMapValue(Object o, String s) {
        return o instanceof Json  && ((Json)o).has(s) ? ((Json)o).get(s) : JsonProvider.UNDEFINED;
    }

    @Override
    public void setProperty(Object o, Object o1, Object o2) {
        if(o instanceof Json){
            ((Json)o).set(o1,o2);
        }
    }

    @Override
    public void removeProperty(Object o, Object o1) {
        if(o instanceof Json){
            ((Json)o).remove(o1);
        }
    }

    @Override
    public boolean isMap(Object o) {
        return o != null && o instanceof Json && !((Json)o).isArray();
    }

    @Override
    public Object unwrap(Object o) {
        return o;
    }
}
