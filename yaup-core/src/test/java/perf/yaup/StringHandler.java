package perf.yaup;

import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.OutputStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class StringHandler extends OutputStreamHandler {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public StringHandler(){
        this(null);
    }
    public StringHandler(String format){
        setOutputStream(baos);
        this.setFormatter(new PatternFormatter(format == null || format.isBlank() ? "%m" : format));
        this.setLevel(Level.ALL);
        this.setAutoFlush(true);
        //LogContext.getLogContext().getLogger("").addHandler(this);
    }

    public String getLog(){return baos.toString();}

    @Override
    public void close() throws SecurityException {
        //LogContext.getLogContext().getLogger("").removeHandler(this);
        super.close();
        try {
            baos.close();
        } catch (IOException e) { /* meh */}
    }

    public static StringHandler rootStringHandler(){
        return rootStringHandler(null);
    }
    public static StringHandler rootStringHandler(String format){
        StringHandler rtrn = new StringHandler(format);
        LogContext.getLogContext().getLogger("").addHandler(rtrn);
        rtrn.setFormatter(new PatternFormatter(format == null || format.isBlank() ? "%m" : format));
        rtrn.setLevel(Level.ALL);
        rtrn.setAutoFlush(true);

        return rtrn;
    }
}
