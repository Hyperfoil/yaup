package io.hyperfoil.tools.yaup;

public class PopulatePatternException extends Exception {

    private String result;
    public PopulatePatternException(String s,String result) {
        super(s);
        this.result = result;
    }
    public String getResult(){return result;}

    void setResult(String result){
        this.result = result;
    }
}
