package io.hyperfoil.tools.yaup;

public class PopulatePatternException extends Exception {

    private String result;
    private Boolean jsFailure;

    public PopulatePatternException(String s,String result) {
        this(s, result, false);
    }
    public PopulatePatternException(String s,String result, Boolean jsFailure) {
        super(s);
        this.result = result;
        this.jsFailure = jsFailure;
    }
    public String getResult(){return result;}

    public Boolean isJsFailure(){ return jsFailure; }

    void setResult(String result){
        this.result = result;
    }
}
