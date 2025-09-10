package io.hyperfoil.tools.yaup.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by wreicher
 */
public class WrappedInputStream extends InputStream {

    public static interface Watcher{
        public void advanced(long amount);
    }

    private final InputStream stream;


    private List<Watcher> watchers;
    private long cumulative;
    public WrappedInputStream(InputStream stream){
        this.stream = stream;
        this.cumulative = 0;
        this.watchers = new LinkedList<>();
    }

    private void notifyAll(long amount){
        if(!watchers.isEmpty()){
            for(Watcher w : watchers){
                w.advanced(amount);
            }
        }
    }
    public void addWatcher(Watcher w){
        watchers.add(w);
    }
    public long getCumulative(){return cumulative;}

    @Override
    public int read() throws IOException {
        int amnt = stream.read();
        cumulative+=amnt;
        notifyAll(amnt);
        return amnt;
    }
    @Override
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int amnt = stream.read(b,off,len);
        cumulative+=amnt;
        notifyAll(amnt);
        return amnt;
    }
    @Override
    public synchronized void reset() throws IOException {
        stream.reset();
        cumulative=0;
    }
    @Override
    public long skip(long n) throws IOException {
        long amnt = stream.skip(n);
        cumulative+=amnt;
        notifyAll(amnt);
        return amnt;
    }

}
