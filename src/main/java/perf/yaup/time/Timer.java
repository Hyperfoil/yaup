package perf.yaup.time;

import perf.yaup.json.Json;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Timer {

    // A black hole to avoid returning null in current
    private static final Timer NULL = new Timer("NULL"){
        @Override
        public Timer start(String name){return this;}
        @Override
        public boolean isActive(){return false;}
    };

    private long nanoStart;
    private long milliStart;
    private long nanoStop;
    private long milliStop;
    private String name;

    private LinkedList<Timer> children;


    public Timer(String name){
        this(name,0,0);
    }
    public Timer(String name, long milli, long nano){
        name = name;
        children = new LinkedList<>();
        this.milliStart = milli;
        this.nanoStart = nano;
    }

    /**
     * Starts the current timer if it was not already started
     * @return
     */
    public boolean start(){
        if(isActive()){
            return false;
        }
        this.milliStart = System.currentTimeMillis();
        this.nanoStart = System.nanoTime();
        return true;
    }
    public boolean isActive(){return milliStop<=0;}
    public long getStart() {return milliStart;}
    public long getStop() {return milliStop;}
    public String getName() {return name;}

    public long nanoTime(){return nanoStop-nanoStart;}
    public long milliTime(){return milliStop-milliStart;}

    /**
     * Starts a new child timer and ends the current child timer.
     * @param name the name of the new timer. Names are not unique
     * @return
     */
    public Timer start(String name){
        long milli = System.currentTimeMillis();
        long nano = System.nanoTime();
        if(isActive()) {
            current().stop(milli, nano);
        }else{
            this.milliStart = milli;
            this.nanoStart = nano;
        }
        //creates a gap but more accurately reflects time being measured by ignoring time taken to stop
        milli = System.currentTimeMillis();
        nano = System.nanoTime();
        Timer newTimer =new Timer(name,milli,nano);
        children.push(newTimer);
        return newTimer;
    }

    /**
     * Stop this timer and all descendant timers
     */
    public void stop(){
        long milli = System.currentTimeMillis();
        long nano = System.nanoTime();
        this.stop(milli,nano);
    }
    public boolean hasChildren(){return !children.isEmpty();}
    public List<Timer> getChildren(){
        return Collections.unmodifiableList(children);
    }

    private Timer current(){
        if(!children.isEmpty()){
            return children.getLast();
        }else{
            return NULL;
        }
    }
    private void stop(long milliStop,long nanoStop){
        if(isActive()) {
            this.milliStop = milliStop;
            this.nanoStop = nanoStop;
            current().stop(milliStop,nanoStop);
        }
    }

    public Json getJson(){
        Json rtrn = new Json();

        rtrn.set(name,getName());
        rtrn.set("start",getStart());
        rtrn.set("stop",getStop());
        rtrn.set("nanos",nanoTime());
        if(hasChildren()){
            Json childrenJson = new Json();
            for(Timer child : children){
                childrenJson.add(child.getJson());
            }
            rtrn.set("timers",childrenJson);
        }


        return rtrn;
    }

}
