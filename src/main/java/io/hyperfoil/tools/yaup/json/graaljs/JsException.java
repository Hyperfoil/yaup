package io.hyperfoil.tools.yaup.json.graaljs;

public class JsException extends RuntimeException{

    private String js;

    public JsException(String message){
        this(message,"");
    }
    public JsException(String message,String js){
        super(message);
        this.js = js;
    }
    public JsException(String message,String js,Throwable throwable){
        super(message,throwable);
        this.js = js;
    }

    public String getJs(){return js;}

    @Override
    public String toString(){
        return "JsException: "+getMessage()+" "+getJs()+" "+getCause();
    }
}
