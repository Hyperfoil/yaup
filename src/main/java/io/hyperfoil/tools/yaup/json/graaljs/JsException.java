package io.hyperfoil.tools.yaup.json.graaljs;

import io.hyperfoil.tools.yaup.json.Json;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.SourceSection;

public class JsException extends RuntimeException{

    private String js;
    private int lineStart;
    private int lineEnd;
    private int columnStart;
    private int columnEnd;
    private Json context;

    public JsException(String message){
        this(message,"",null,-1,-1,-1,-1);
    }
    public JsException(String message,String js,int lineStart,int lineEnd,int columnStart,int columnEnd){
        this(message,js,null,lineStart,lineEnd,columnStart,columnEnd);
        this.context=new Json(false);
    }
    public JsException(String message,String js){
        this(message,js,null);
        this.context=new Json(false);
    }

    public Json getContext(){return context;}
    public JsException(String message,PolyglotException pe){
        super(message,pe);
        this.context=new Json(false);
        SourceSection sourceSection = pe.getSourceLocation();
        if(sourceSection!=null){
            Source source = sourceSection.getSource();
            if(source != null){
                this.js = source.hasCharacters() ? source.getCharacters().toString() : new String(source.getBytes().toByteArray());
            }else{
                this.js = "unknown";
            }
            this.lineStart = sourceSection.getStartLine();
            this.lineEnd = sourceSection.getEndLine();
            this.columnStart = sourceSection.getStartColumn();
            this.columnEnd = sourceSection.getEndColumn();
        }else{
            this.js = "unknown";
            this.lineStart=-1;
            this.lineEnd=-1;
            this.columnStart=-1;
            this.columnEnd=-1;
        }

    }
    public JsException(String message,String js,Throwable throwable){
        super(message,throwable);
        this.js=js;
        this.context=new Json(false);
        if (throwable != null && throwable instanceof PolyglotException){
            PolyglotException pe = (PolyglotException) throwable;
            SourceSection sourceSection = pe.getSourceLocation();
            if(sourceSection != null) {
                Source source = sourceSection.getSource();
                this.lineStart = sourceSection.getStartLine();
                this.lineEnd = sourceSection.getEndLine();
                this.columnStart = sourceSection.getStartColumn();
                this.columnEnd = sourceSection.getEndColumn();
            }
        }else{
            this.lineStart=-1;
            this.lineEnd=-1;
            this.columnStart=-1;
            this.columnEnd=-1;
        }
    }
    public JsException(String message,String js,Throwable throwable,int lineStart,int lineEnd,int columnStart,int columnEnd){
        super(message,throwable);
        this.js = js;
        this.context=new Json(false);
        this.lineStart=lineStart;
        this.lineEnd=lineEnd;
        this.columnStart=columnStart;
        this.columnEnd=columnEnd;
    }

    public boolean hasSource(){return js!=null;}
    public boolean hasErrorLocation(){return columnStart>-1 && columnEnd>-1 && lineStart>-1 && lineEnd>-1;}
    public int getLineStart(){return lineStart;}
    public int getLineEnd(){return lineEnd;}
    public int getColumnStart(){return columnStart;}
    public int getColumnEnd(){return columnEnd;}
    public String getJs(){return js;}

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());
        String split[] = js.split(System.lineSeparator());
        for(int i=0; i<split.length; i++){
            if(i>0) {
                sb.append(System.lineSeparator());
            }
            sb.append(split[i]);
            if((i+1)==lineStart){//one based
                sb.append(System.lineSeparator());
                sb.append(String.format("%"+columnStart+"s","^")+String.format("%"+(columnEnd-columnStart)+"s","^").replaceAll(" ","-"));
            }
        }
        if(!getContext().isEmpty()){
            sb.append(System.lineSeparator());
            sb.append(getContext().toString());
        }
        return sb.toString();
    }
}
