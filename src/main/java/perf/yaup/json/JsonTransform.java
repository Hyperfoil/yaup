package perf.yaup.json;


import perf.yaup.AsciiArt;
import perf.yaup.StringUtil;
import perf.yaup.file.FileUtility;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Takes a list of json and creates a new list
 */
public class JsonTransform implements Function<Json,Json>{

    private final Map<String, Function<Json,Object>> ops = new LinkedHashMap<>();

    public JsonTransform(){}

    public JsonTransform set(String name, String jsonPath){
        set(name,(json)->{
            Object result = Json.find(json,jsonPath);
            if(result instanceof Json){
                Json resultJson = (Json)result;
                if(resultJson.isArray() && resultJson.size()==1){
                    return resultJson.get(0);
                }else if (resultJson.size()==0) {
                    return "";
                }else{
                    return resultJson;
                }
            }else{
                return result;
            }

        });
        return this;
    }
    public JsonTransform set(String name, Function<Json,Object> op){
        ops.put(name,op);
        return this;
    }

    @Override
    public Json apply(Json json) {
        Json rtrn = new Json();
        ops.forEach((name,op)->{
            Object result = op.apply(json);
            rtrn.set(name, result);
        });
        return rtrn;
    }
}
