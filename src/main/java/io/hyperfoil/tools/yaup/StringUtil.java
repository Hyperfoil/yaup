package io.hyperfoil.tools.yaup;

import com.oracle.truffle.regex.nashorn.regexp.joni.exception.SyntaxException;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.NashornMapContext;
import io.hyperfoil.tools.yaup.json.ValueConverter;
import io.hyperfoil.tools.yaup.json.graaljs.JsonProxy;
import io.hyperfoil.tools.yaup.json.graaljs.JsonProxyObject;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by wreicher
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class StringUtil {

    public static final String MINUTES = "m";
    public static final String SECONDS = "s";
    public static final String MILLISECONDS = "ms";
    public static final String HOURS = "h";

    private static final Pattern timeUnitPattern = Pattern.compile("(?<amount>\\d+)(?<unit>"+MILLISECONDS+"|"+MINUTES+"|"+SECONDS+"|"+HOURS+")?");


    public static final String PATTERN_PREFIX = "${{";
    public static final String PATTERN_JAVASCRIPT_PREFIX = "=";
    public static final String PATTERN_SUFFIX = "}}";
    public static final String PATTERN_DEFAULT_SEPARATOR = ":";

    private static final Pattern NAMED_CAPTURE = java.util.regex.Pattern.compile("\\(\\?<([^>]+)>");
    private static final String VALID_REGEX_NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static List<String> getCaptureNames(String pattern) {
        Matcher fieldMatcher = NAMED_CAPTURE.matcher(pattern);
        List<String> names = new LinkedList<>();
        while (fieldMatcher.find()) {
            names.add(fieldMatcher.group(1));
        }
        return names;
    }
    /*

     */
    public static String populatePattern(String pattern, Map<Object,Object> map) throws PopulatePatternException{
        return populatePattern(pattern,map,PATTERN_PREFIX,PATTERN_DEFAULT_SEPARATOR,PATTERN_SUFFIX, PATTERN_JAVASCRIPT_PREFIX);
    }
    public static Object jsEval(String js,Object...args){
        return jsEval(js, Collections.EMPTY_LIST,args);
    }
    public static Object jsEval(String js, Collection<String> evals,Object...args){
        try(Context context = Context.newBuilder("js")
        .allowAllAccess(true)
       .allowExperimentalOptions(true)
       .option("js.experimental-foreign-object-prototype", "true")
        .build()){
            context.enter();
            try{
                evals.forEach(s->{
                    try {
                        context.eval("js", s);
                    } catch(PolyglotException pge) {
                        throw new RuntimeException("failed to evaluate "+s+" preparing for "+js,pge);
                    }
                });
                Value matcher = null;
                try{ //evaluate the js to see if it directly returns a value
                    matcher = context.eval("js",js);
                }catch(PolyglotException pge){

                    //throw new RuntimeException("failed to evaluate "+js,pge);
                    try{
                        matcher = context.eval("js","(() => "+js+")()");
                    }catch (PolyglotException pge2){

                    }
                }
                if(matcher==null ){
                    //TODO raise issue that the return from graaljs is missing
                }else{
                    if( !matcher.canExecute() ){
                        //the result of evaluating the javascript is an object
                    }else {
                        if (args != null && args.length > 0) {
                            for (int i = 0; i < args.length; i++) {
                                if (args[i] != null && args[i] instanceof Json) {
                                    args[i] = JsonProxy.create((Json) args[i]);
                                }
                            }
                            Value result = matcher.execute(args);
                            if (result != null) {
                                matcher = result;
                            }
                        }
                    }
                    Object converted = ValueConverter.convert(matcher);
                    if (converted instanceof JsonProxyObject) {
                        return ((JsonProxyObject) converted).getJson();
                    } else if (converted instanceof Json) {
                        return (Json) converted;
                    } else {
                        return converted;
                    }
                }

            }catch(Exception e){
                throw new RuntimeException("jsEval exception for:"+js,e);
            }finally{
                context.leave();
            }
        }
        return null;
    }
    public static String populatePattern(String pattern, Map<Object,Object> map,String prefix, String separator, String suffix, String javascriptPrefix) throws PopulatePatternException {
        Set<String> seen = new HashSet();
        return populatePattern(pattern,map,prefix,separator,suffix,javascriptPrefix,seen);
    }
    private static String populatePattern(String pattern, Map<Object,Object> map, String prefix, String separator, String suffix, String javascriptPrefix, Set<String> seen) throws PopulatePatternException {
        boolean replaceMissing = false;
        if(map == null){
            map = new HashMap<>();
        }
        String rtrn = pattern;
        boolean replaced;
        int skip=0;
        int seenIndex=-1; //index where seen becomes invalid
        do {
            replaced = false;
            int nameStart=-1;
            int nameEnd=-1;
            int defaultStart=-1;
            int defaultEnd=-1;
            int count=0;
            char quoteChar='"';
            boolean inQuote=false;
            for(int i=skip; i<rtrn.length(); i++){
                char targetChar = rtrn.charAt(i);
                if(inQuote){
                   if(rtrn.charAt(i) == quoteChar){
                      inQuote = false;
                   }
                }else{
                    // added ` for string template patterns in javascript, do we need to make sure it's js to accept `?
                    // only care about count > 0 quotes so we ignore quotes outside of ${{...}}
                   if(count > 0 && (rtrn.charAt(i) == '"' || rtrn.charAt(i) == '\'' || rtrn.charAt(i) == '`')){ //TODO do we ignore escaped ' and "?
                      quoteChar=rtrn.charAt(i);
                      inQuote=true;
                   }
                }
                if(rtrn.startsWith(prefix,i)){
                    if(count==0){
                        nameStart=i;
                    }
                    count++;
                    i+=prefix.length()-1;
                }else if (rtrn.startsWith(separator,i) && !inQuote){ // '${{FOO:foo}}' doesn't work if checking for inQuote
                    if(count==1){
                        nameEnd=i;
                        defaultStart=i;
                    }

                }else if (rtrn.startsWith(suffix,i) && count > 0){ //added count > 0 for when jq has }} outside ${{ }}
                    count--;
                    if(count==0){
                        if(nameEnd>-1 && defaultStart>-1){
                            defaultEnd=i;
                        }else{
                            nameEnd=i;//do we need -1 for the case where } in the end
                        }
                        i=rtrn.length();//end the loop
                    }
                    i+=suffix.length()-1;//skip the rest of suffix
                }
            }
            if(nameStart>-1 && count == 0){
                replaced = true;
                if(nameStart > seenIndex && seenIndex >= 0){
                   seen.clear();
                }
                String namePattern = rtrn.substring(nameStart + prefix.length(),nameEnd);
                String name = populatePattern(namePattern,map,prefix,separator,suffix,javascriptPrefix,seen);
                String defaultValue = defaultStart>-1?rtrn.substring(defaultStart+separator.length(),defaultEnd): null;
                //String replacement = map.containsKey(name) ? map.get(name).toString() : defaultValue;

                boolean isJavascript = false;
                if(name.startsWith(javascriptPrefix)){
                    isJavascript = true;
                    name = name.substring(javascriptPrefix.length());
                }

                String replacement = null;
                if(!isJavascript && map.containsKey(name) && !map.get(name).toString().isEmpty()){
                    if (seen.contains(name)) {
                        throw new PopulatePatternException("Circular pattern reference "+name+"\n  pattern="+pattern+"\n  nameStart="+nameStart+"\n  seenIndex="+seenIndex+"\n  seen="+seen+"\n  rtrn="+rtrn);
                    }
                    replacement = map.get(name).toString();
                    seen.add(name); //only add to seen if used to replace?
                }else if(isJavascript || StringUtil.findAny(name,"()/*^+-") > -1 || name.matches(".*?\\.\\.\\.\\s*[{\\[].*")){
                    String value = null;
                    try(Context context = Context.newBuilder("js")
                       .allowHostAccess(true)
                        .allowIO(true)
                        //.fileSystem()TODO custom fileSystem for loading modules?
                        .build()
                       ){
                        //TODO this is probably not how Graal expects to access static methods
                        context.eval("js", "function milliseconds(v){ return Packages.io.hyperfoil.tools.yaup.StringUtil.parseToMs(v)}");
                        context.eval("js", "function seconds(v){ return Packages.io.hyperfoil.tools.yaup.StringUtil.parseToMs(v)/1000}");

                        Value evaled = null;
                        try { //try the javascript as an expression that returns a value
                            evaled = context.eval("js", name);
                        }catch(SyntaxException|PolyglotException e){
                            //try returning the javascript as the value
                            Value factory = context.eval("js","new Function('return '+"+StringUtil.quote(name)+")"); //this method didn't work with multi-line string literals
                            evaled = factory.execute();
                        }
                        value = ValueConverter.convert(evaled).toString();
                        replacement = value;

                    }catch (SyntaxException | PolyglotException e){
//                        System.err.println("SyntaxException::"+e.getMessage()+"\n"+pattern+"\n"+Arrays.asList(e.getStackTrace())
//                           .stream()
//                           .map(ste->ste.getClassName()+"."+ste.getMethodName()+"():"+ste.getLineNumber())
//                           .collect(Collectors.joining("\n"))
//                        );
                    }
                }
                if((replacement == null || "".equals(replacement))){
                    replacement = defaultValue != null ? defaultValue : (map.containsKey(name) ? map.get(name).toString() : null);
                }
                int end = Math.max(nameEnd,defaultEnd)+PATTERN_SUFFIX.length();
                if(replacement == null){
                    skip = end;
                    throw new PopulatePatternException("Unable to resolve replacement for: " + name + " Either state variable has not been set, or JS expression is invalid"); //TODO: replace with logging framework
                }else {
                    rtrn = rtrn.substring(0, nameStart) + replacement + rtrn.substring(end);
                    seenIndex = nameStart + replacement.length()-1;
                }
            }
        }while(replaced);
        return rtrn;
    }

    public static HashedLists<String,String> groupCommonPrefixes(List<String> inputs){
        return groupCommonPrefixes(inputs,StringUtil::commonPrefixLength);
    }
    public static HashedLists<String,String> groupCommonPrefixes(List<String> inputs, BiFunction<String,String,Integer> prefixMeasurer){
        inputs.sort(String.CASE_INSENSITIVE_ORDER);
        HashedLists rtrn = new HashedLists();

        for(int i=0; i<inputs.size(); i++){
            String current = inputs.get(i);
            String previous = i>0 ? inputs.get(i-1) : "";
            String next = i < inputs.size()-1 ? inputs.get(i+1) : "";


            int commonPreviousLength = prefixMeasurer.apply(current,previous);
            int commonNextLength = prefixMeasurer.apply(current,next);

            if( commonPreviousLength == 0 && commonNextLength == 0){
                rtrn.put(current,current);
            }else{
                if( commonPreviousLength > commonNextLength){
                    rtrn.put(current.substring(0,commonPreviousLength),current);
                }else{
                    rtrn.put(current.substring(0,commonNextLength),current);
                }
            }
        }
        return rtrn;
    }

    public static long parseToMs(String amount){
        amount = amount.replaceAll("_","");
        long rtrn = 0;
        Matcher m = timeUnitPattern.matcher(amount);
        while(m.find()){
            long toAdd = Long.parseLong(m.group("amount"));
            String unit = m.group("unit") == null ? "" : m.group("unit"); //in case there isn't a unit
            TimeUnit timeUnit;
            switch (unit){
                case HOURS:
                    timeUnit = TimeUnit.HOURS;
                    break;
                case MINUTES:
                    timeUnit = TimeUnit.MINUTES;
                    break;
                case SECONDS:
                    timeUnit = TimeUnit.SECONDS;
                    break;
                case MILLISECONDS:
                default:
                    timeUnit = TimeUnit.MILLISECONDS;
            }
            long increment = timeUnit.toMillis(toAdd);
            rtrn += increment;
        }
        return rtrn;
    }

    public static long parseKMG(String kmg){
        Matcher m = java.util.regex.Pattern.compile("(?<number>\\d+\\.?\\d*)\\s?(?<kmg>[kmgtpezyKMGTPEZY]*)(?<bB>[bB]*)").matcher(kmg);
        if(m.matches()){

            double mult = 1;

            switch(m.group("kmg").toUpperCase()){
                case "Y": mult*=1024;//8
                case "Z": mult*=1024;//7
                case "E": mult*=1024;//6
                case "P": mult*=1024;//5
                case "T": mult*=1024;//4
                case "G": mult*=1024;//3
                case "M": mult*=1024;//2
                case "K": mult*=1024;//1
                case "B": mult*=1; // included for completeness
            }
            double bytes = m.group("bB").equals("b") ? 1.0/8 : 1;
            double v =Double.parseDouble(m.group("number"))*mult*bytes;
            return (long)v;
        }else{
            if(kmg.equals("-")){//trap for when dstat has a - value for a field (no idea why that happens but it does
                return 0;
            } else {
                throw new IllegalArgumentException(kmg + " does not match expected pattern for KMG");
            }
        }
    }


    public static <T extends Enum<?>> T getEnum(String input,Class<T> clazz){
        return getEnum(input,clazz,null);
    }
    /**
     * Finds the nearest matching enum ignoring case and removing - or _'s.
     * @param input the name of an enum instance
     * @param clazz the enum class
     * @param defaultValue return value if a match is not found. null is acceptable
     * @param <T> the enum class
     * @return enum instance matching input string or defaultValue
     */
    public static <T extends Enum<?>> T getEnum(String input,Class<T> clazz,T defaultValue){
        if(input==null || input.isEmpty()){
            return defaultValue;
        }
        input = input.replaceAll("[\\-_]","");
        for(T t : clazz.getEnumConstants()){
            if(input.equalsIgnoreCase(t.name())){
                return t;
            }
        }
        return defaultValue;
    }

    private static char randNameChar(){
        return VALID_REGEX_NAME_CHARS.charAt(ThreadLocalRandom.current().nextInt(0,  VALID_REGEX_NAME_CHARS.length()));
    }

    public static String greatestCommonSubstring(String first, String second){
        int firstLength = first.length();
        int secondLength = second.length();
        int maxIndex = -1;
        int max = 0;

        int[][] dp = new int[firstLength][secondLength];

        for(int f=0; f<firstLength; f++){
            for(int s=0; s<secondLength; s++){
                if(first.charAt(f) == second.charAt(s)){
                    if(f==0 || s==0){
                        dp[f][s]=1;
                    }else{
                        dp[f][s] = dp[f-1][s-1]+1;
                    }
                    if(max < dp[f][s]) {
                        max = dp[f][s];
                        maxIndex = f;
                    }
                }

            }
        }
        if(max>0) {
            return first.substring(maxIndex - max+1, maxIndex+1);
        }else{
            return "";
        }
    }

    public static int commonPrefixLength(String a,String b){
        int rtrn = 0;
        while(a.length()>rtrn && b.length()>rtrn && a.charAt(rtrn) == b.charAt(rtrn)){
            rtrn++;
        }
        return rtrn;
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
        int index = 0-toFind.length();
        while( (index=target.indexOf(toFind,index+toFind.length()))>-1){
            count++;
        }
        return count;
    }
    public static String toHms(long duration){
        String rtrn ;
        long ms =  duration % 1000;
        duration = duration / 1000;
        if(ms>0){
            return ""+duration;
        }else{
            rtrn = (duration%60)+"s";
            duration = duration/60;
            if(duration==0){return rtrn;}

            rtrn = (duration%60)+"m"+rtrn;
            duration = duration/60;
            if(duration==0){return rtrn;}

            rtrn = (duration%24)+"h"+rtrn;
            duration = duration/24;
            if(duration==0){return rtrn;}

            rtrn = duration+"d"+rtrn;
        }
        return rtrn;
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

    public static int findAny(String input,String toFind){
        int index=-1;
        HashSet<Character> chars = new HashSet<>();
        boolean stop=false;
        if(toFind!=null){
            char[] charArray = toFind.toCharArray();
            //noinspection ForLoopReplaceableByForEach
            for(int i=0; i<charArray.length; i++){
                chars.add(charArray[i]);
            }
        }
        while(index<input.length() && !stop){
            index++;
            if(index>=input.length()){
                stop=true;
            }else if(chars.contains(input.charAt(index))){
                stop=true;
            }
        }
        if(index >= input.length()){
            index = -1;
        }
        return index;
    }

    /**
     * Find the first occurrence of a character in toFind that is not wrapped in ' or "
     * @param input subject of search
     * @param toFind set of characters to find in input
     * @return the substring up to the first matched char or all of input
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
        char[] charArray = toFind.toCharArray();
        //noinspection ForLoopReplaceableByForEach
        for(int i=0; i<charArray.length; i++){
            chars.add(charArray[i]);
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
                        if(! ( index > 0 && '/' == input.charAt(index-1) )){//it's escaped, not end
                            quoted=false;
                        }
                    }
                }
            }
        }
        return input.substring(0,index);
    }
    public static String trimIndent(String input){
        return input.replaceAll("^\\s*","");
    }
    public static String trimTrailing(String input){
        return input.replaceAll("\\s*$","");
    }
    public static String removeSpace(String input){
        return input.replaceAll("\\s+","");
    }

    public static boolean isQuoted(String value){
        if(value == null){
            return false;
        }
        return (value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("\'") && value.endsWith("\'"));
    }
    public static String quote(String value){
        return quote(value,"\"");
    }
    public static String quote(String value,String quoteMark){
        if(value ==null){
            value = "";
        }
        if(isQuoted(value)){
            return value;
        }
        return quoteMark+value.replaceAll(""+quoteMark+"(?<!\\\\"+quoteMark+")","\\\\"+quoteMark+"")+quoteMark;
    }
    public static String removeQuotes(String value){
        if(value==null){
            return null;
        }
        String rtrn = value;
        if( isQuoted(value)) {
            rtrn =value.substring(1,value.length()-1);
        }
        return rtrn;
    }
    public static int indexNotMatching(String input,String toFind,int start) {
        int index = start;
        boolean stop = false;
        HashSet<Character> chars = new HashSet<>();
        if (toFind != null) {
            char[] charArray = toFind.toCharArray();
            //noinspection ForLoopReplaceableByForEach
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
