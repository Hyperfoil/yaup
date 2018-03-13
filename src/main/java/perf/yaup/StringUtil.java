package perf.yaup;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by wreicher
 */
public class StringUtil {

    private static final String VALID_REGEX_NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static char randNameChar(){
        return VALID_REGEX_NAME_CHARS.charAt(ThreadLocalRandom.current().nextInt(0,  VALID_REGEX_NAME_CHARS.length()));
    }

    public static String generateRegexNameSubstitute(String input){
        if(input==null){return "";}

        StringBuilder rtrn = new StringBuilder();
        while(input.contains(rtrn.toString())){
            rtrn.append(randNameChar());
        }
        return rtrn.toString();
    }
    public static String escapeRegex(String input){
        if(input==null){ return "";}

        String rtrn = input;
        rtrn = rtrn.replaceAll("\\.(?<!\\\\\\.)","\\\\.");
        rtrn = rtrn.replaceAll("\\*(?<!\\\\\\.)","\\\\*");
        rtrn = rtrn.replaceAll("\\+(?<!\\\\\\+)","\\\\+");
        rtrn = rtrn.replaceAll("\\?(?<!\\\\\\?)","\\\\?");
        rtrn = rtrn.replaceAll("\\^(?<!\\\\\\^)","\\\\^");
        rtrn = rtrn.replaceAll("\\$(?<!\\\\\\$)","\\\\\\$");
        return rtrn;
    }

    public static int countOccurances(String target,String toFind){

        int count = 0;
        int index = -1;
        while( (index=target.indexOf(toFind,index+1))>-1){
            count++;
        }
        return count;
    }
    public static String durationToString(long duration){

        long _ms = duration % 1000;
        duration = duration / 1000;
        String rtrn = String.format("%02d.%03d",duration%60,_ms);
        duration = duration/60;
        if(duration==0) { return rtrn; }
        rtrn = String.format("%02d:%s", duration % 60, rtrn);
        duration = duration/60;
        if(duration==0) { return rtrn; }
        rtrn = String.format("%02d:%s", duration % 24, rtrn);
        duration = duration/24;
        if(duration==0) { return rtrn; }
        rtrn = String.format("%dd %s", duration % 7, rtrn);
        duration = duration/7;
        if(duration==0) { return rtrn; }
        rtrn = String.format("%dw %s", duration % 52, rtrn);
        if(duration==0) { return rtrn; }
        return rtrn;
    }

    //Levenshtein
    public static int editDistance(String s1, String s2) {
        if(s1==null || s2==null){
            return -1;
        }

        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    /**
     * Find the first occurance of a character in toFind that is not wrapped in ' or "
     * @param input
     * @param toFind
     * @return
     */
    public static String findNotQuoted(String input,String toFind){
        //validity traps
        if(input==null){ return ""; }
        if(toFind==null){ return input; }

        int index=-1;
        boolean stop=false;
        boolean quoted=false;
        char quoteChar='"';
        HashSet<Character> chars = new HashSet<>();
        if(toFind!=null){
            char charArray[] = toFind.toCharArray();
            for(int i=0; i<charArray.length; i++){
                chars.add(charArray[i]);
            }
        }
        while(index<input.length() && !stop){
            index++;
            if(index>=input.length()){
                stop=true;
            }else if(!quoted && chars.contains(input.charAt(index))){
                stop=true;
            }else if ('"' == input.charAt(index) || '\'' == input.charAt(index)){
                if(!quoted){
                    quoteChar = input.charAt(index);
                    quoted=true;
                }else{//already in a quote
                    if(quoteChar == input.charAt(index)){ // this could be the end of the quote
                        if(index > 0 && '/' == input.charAt(index-1)){//it's escaped, not end

                        }else{
                            quoted=false;
                        }
                    }
                }
            }
        }
        return input.substring(0,index);
    }
    public static String quote(String value){
        return quote(value,"\"");
    }
    public static String quote(String value,String quoteMark){
        return quoteMark+value.replaceAll(""+quoteMark+"(?<!\\\\"+quoteMark+")","\\\\"+quoteMark+"")+quoteMark;
    }
    public static String removeQuotes(String value){
        String rtrn = value;
        if( (value.startsWith("\"")&& value.endsWith("\"")) || (value.startsWith("\'") && value.endsWith("\'"))) {
            rtrn =value.substring(1,value.length()-1);
        }
        return rtrn;
    }
    public static int indexNotMatching(String input,String toFind,int start) {
        int index = start;
        boolean stop = false;
        HashSet<Character> chars = new HashSet<>();
        if (toFind != null) {
            char charArray[] = toFind.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                chars.add(charArray[i]);
            }
        }
        while (!stop && index < input.length() && chars.contains(input.charAt(index))) {
            index++;
        }
        return index;
    }
}
