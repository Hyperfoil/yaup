package io.hyperfoil.tools.yaup.json.graaljs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.hyperfoil.tools.yaup.json.Json;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

/**
 * This is a shim for the Response object returned by fetch()
 */
public class JsResponse {
    private String content;

    //member variables to provide the expected properties in javascript
    @HostAccess.Export
    public final int status;
    @HostAccess.Export
    public final String statusText;
    @HostAccess.Export
    public final ObjectNode headers;
    @HostAccess.Export
    public final boolean ok;
    @HostAccess.Export
    public final boolean redirected;
    @HostAccess.Export
    public final String type;
    @HostAccess.Export
    public final String url;


    public JsResponse(String content, int status, String statusText, ObjectNode headers,boolean redirected,String type, String url){
        this.content = content;
        this.status = status;
        this.statusText = statusText;
        this.headers = headers;
        this.ok = status < 300 && status >= 200;
        this.redirected = redirected;
        this.type = type;
        this.url = url;
    }

    @HostAccess.Export
    public Object text(){
        return content;
    }

    @HostAccess.Export
    public Object json(){

        Object wrapped = JsonProxy.create(Json.fromString(content));
        return (JsThenable) (onResolve, onReject) -> {
            try{
                if(onResolve.hasMember("then")){
                    onResolve.invokeMember("then",wrapped);
                }else{
                    if(onResolve.canExecute()){
                        onResolve.execute(wrapped);
                    }
                }
            }catch(Exception e){
                if(onReject.hasMember("then")) {
                    onReject.invokeMember("then", e.getMessage());
                }else{
                    if(onReject.canExecute()){
                        onReject.execute(e.getMessage());
                    }
                }
            }
        };
    }

    @Override
    public String toString(){return "Response";}

}
