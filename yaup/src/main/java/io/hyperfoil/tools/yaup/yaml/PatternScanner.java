package io.hyperfoil.tools.yaup.yaml;

import io.hyperfoil.tools.yaup.StringUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.scanner.Scanner;
import org.yaml.snakeyaml.scanner.ScannerImpl;
import org.yaml.snakeyaml.tokens.FlowMappingEndToken;
import org.yaml.snakeyaml.tokens.FlowMappingStartToken;
import org.yaml.snakeyaml.tokens.ScalarToken;
import org.yaml.snakeyaml.tokens.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an implementation of org.yaml.snakeyaml.scanner.Scanner that uses org.yaml.snakeyaml.scanner.ScannerImpl
 * but intercepts the tokens to recombine $,{,{,...,},} into a pattern expressions. This allows patterns to exist as implicit
 * values inside yaml flow maps and flow sequences but does break yaml syntax.
 * For example, `{foo: ${{bar}} }` is not valid yaml because `$` is seen as a scalar and the following `{` is seen as the start
 * of another map. Using a PatternScanner will preserve `${{bar}}` as a scalar value without forcing the value to be treated as a String.
 * This has the advantage of avoiding tpe erasure (if `bar` is a number it will remain a number rather than turn into a string).
 * A more valid way to express this in yaml would be to not use flow sytnax
 * foo: ${{bar}}
 * removing the surrounding `{}` avoids flow syntax and still results in a map where the key is foo
 * but the flow syntax limitation was too bothersome to ignore.
 */
public class PatternScanner implements Scanner {
    private StreamReader reader;
    private ScannerImpl scanner;
    private List<Token> stolenTokens = new ArrayList<>();
    public PatternScanner(StreamReader reader, LoaderOptions options) {
        this.reader = reader;
        this.scanner = new ScannerImpl(reader,options);
    }

    public String combineTokens(List<Token> tokens,int start,int end){
        StringBuilder stringBuilder = new StringBuilder();
        boolean deferQuote=false;
        char quoteChar=' ';
        for(int i=start; i<end; i++){
            Token t = tokens.get(i);
            switch(t.getTokenId()){
                case Scalar:
                    ScalarToken sc = (ScalarToken)t;
                    if("$".equals(sc.getValue()) && i==start){
                        //first entry
                        if(DumperOptions.ScalarStyle.DOUBLE_QUOTED.equals(sc.getStyle()) || DumperOptions.ScalarStyle.SINGLE_QUOTED.equals(sc.getStyle())){
                            deferQuote=true;
                            quoteChar = sc.getStyle().getChar();
                            stringBuilder.append(quoteChar);
                        }
                        stringBuilder.append("$");//TODO quoting
                    }else{
                        if(DumperOptions.ScalarStyle.DOUBLE_QUOTED.equals(sc.getStyle())){
                            stringBuilder.append(StringUtil.quote(sc.getValue(),"\""));
                        } else if (DumperOptions.ScalarStyle.SINGLE_QUOTED.equals(sc.getStyle())) {
                            stringBuilder.append(StringUtil.quote(sc.getValue(),"'"));
                        }else{
                            stringBuilder.append(sc.getValue());
                        }
                    }
                    break;
                case FlowMappingStart:
                    stringBuilder.append("{");
                    break;
                case FlowMappingEnd:
                    stringBuilder.append("}");
                    break;
                case FlowSequenceStart:
                    stringBuilder.append("[");
                    break;
                case FlowSequenceEnd:
                    stringBuilder.append("]");
                    break;
                case FlowEntry:
                    stringBuilder.append(",");
                    break;
                case Value:
                    stringBuilder.append(":");
                    break;
                case Key://We don't need to add these to the encoding because they don't have associated characters
                    break;
                default:
                    System.err.println("PatternScanner missing conversion for "+t.getTokenId().name()+" "+t);
            }
        }
        //should this first ensure we are closing a pattern reference?
        if(deferQuote){
            stringBuilder.append(quoteChar);
        }
        return stringBuilder.toString();
    }

    public void stealTokens(){
        scanner.checkToken(Token.ID.Comment);
        boolean stolen = false;
        try{
            while(true){
                Token t = scanner.getToken();
                stolen=true;
                stolenTokens.add(t);
            }
        }catch(IndexOutOfBoundsException e){}//only way to know scanner is empty
        //print out the tokens for now
        if(stolen) {
            int patternStartDepth=-1;
            int patternStart=-1;
            int patternDepth=0;
            for (int i = 0; i < stolenTokens.size(); i++) {
                Token t = stolenTokens.get(i);
                Token.ID id = t.getTokenId();
                if (t instanceof ScalarToken && ((ScalarToken) t).getValue().endsWith("$") && stolenTokens.size() > i + 2 && stolenTokens.get(i + 1) instanceof FlowMappingStartToken && stolenTokens.get(i + 2) instanceof FlowMappingStartToken) {
                    //we are in a pattern
                    if(patternStart < 0) {
                        //start a new pattern
                        patternStart = i;
                    }
                    patternDepth++;
                    i+=2;//advanced past the 2 FlowMappingStartToken
                    continue;
                }
                //if we are an end pattern
                if( t instanceof FlowMappingEndToken && stolenTokens.size() > i+1 && stolenTokens.get(i+1) instanceof FlowMappingEndToken){
                    patternDepth--;
                    i+=1;//advance to end of the matched pattern
                    if( patternDepth==0 ){
                        String combined = combineTokens(stolenTokens,patternStart,i+1);
                        Token firstDropped = stolenTokens.get(patternStart);
                        Token lastDropped = stolenTokens.get(i+1);
                        for (int k=0; k<= i-patternStart; k++){//remove the tokens
                            stolenTokens.remove(patternStart);
                        }
                        i=patternStart;
                        Token newToken = new ScalarToken(
                                combined,
                                true/*"'\"".contains(combined.substring(0,1))*/,
                                firstDropped.getStartMark(),
                                lastDropped.getEndMark(),
                                combined.startsWith("\"") ? DumperOptions.ScalarStyle.DOUBLE_QUOTED
                                        : combined.startsWith("'") ? DumperOptions.ScalarStyle.SINGLE_QUOTED
                                        : DumperOptions.ScalarStyle.PLAIN
                        );
                        stolenTokens.add(
                                patternStart,
                                newToken
                        );
                        patternStart=-1;
                    }
                }
            }
        }

        //look for $,{,{ and the associated ,},} and replace with single scalarToken
    }

    @Override
    public boolean checkToken(Token.ID... ids) {
        stealTokens();
        if(!stolenTokens.isEmpty()){
            if(ids.length == 0){
                return true;
            }
            Token.ID t = stolenTokens.get(0).getTokenId();
            for(int i=0; i<ids.length; i++){
                if (t == ids[i]){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Token peekToken() {
        stealTokens();
        //throws the same IOOBException as ScannerImpl
        return stolenTokens.get(0);
    }

    @Override
    public Token getToken() {
        stealTokens();
        Token rtrn = stolenTokens.remove(0);
        return rtrn;
    }

    @Override
    public void resetDocumentIndex() {
        scanner.resetDocumentIndex();
    }
}
