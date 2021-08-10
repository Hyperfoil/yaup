package io.hyperfoil.tools.yaup.json.graaljs;

public class JsException extends Exception{

    private String js;

    public JsException(String message){
        this(message,"");
    }
    public JsException(String message,String js){
        super(message);
        this.js = js;
    }

    public String getJs(){return js;}
}
