package io.hyperfoil.tools.yaup.time;

import io.hyperfoil.tools.yaup.json.Json;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A SystemTimer based on System.currentTimeMillis and System.nanoTime that supports sequential or parallel children timers.
 * Using System.currentTimeMillis is not suitable for short lived tasks
 * * This is not for performance testing
 * * This is for timing tasks in a long process
 * A parallel child timer will only stop if either the parent stops or timer.stop() is called
 * A serial child timer stops whenever the next serial sibling timer starts
 */
public class SystemTimer {

    // A black hole to avoid returning null for SystemTimer.current()
    private static final SystemTimer NULL = new SystemTimer("NULL"){
        @Override
        public SystemTimer start(String name, boolean parallel){return this;}
        @Override
        public boolean isActive(){return false;}
    };

    private final boolean isParallel;
    private long nanoStart;
    private long milliStart;
    private long nanoStop=-1;
    private long milliStop=-1;
    private String name;

    private LinkedList<SystemTimer> children;


    public SystemTimer(String name){
        this(name,0,0, false);
    }
    public SystemTimer(String name, long milli, long nano, boolean isParallel){
        this.name = name;
        this.children = new LinkedList<>(); // this risks NPE when concurrently modified :(
        this.milliStart = milli;
        this.nanoStart = nano;
        this.isParallel = isParallel;
    }

    /**
     * Starts the current timer if it was not already started
     * @return true if the timer was started, or false if already active
     */
    public boolean start(){
        if(isActive()){
            return false;
        }
        this.milliStart = System.currentTimeMillis();
        this.nanoStart = System.nanoTime();
        return true;
    }
    public boolean isStopped(){return isStarted() && milliStop > 0;}
    public boolean isStarted(){return milliStart > 0;}
    public boolean isActive(){return isStarted() && !isStopped();}
    public long getStart() {return milliStart;}
    public long getStop() {return milliStop;}
    public String getName() {return name;}

    public long nanoTime(){return nanoStop-nanoStart;}
    public long milliTime(){return milliStop-milliStart;}


    public SystemTimer start(String name){
        return start(name,false);
    }
    /**
     * Starts a new child timer and ends the current child timer if !parallel.
     * @param name the name of the new timer. Names are not unique
     * @param parallel if true, leave current child timer running, otherwise stop child timers
     * @return new SystemTime
     */
    public SystemTimer start(String name, boolean parallel){
        long milli = System.currentTimeMillis();
        long nano = System.nanoTime();
        if(isActive()) {
            if(!parallel) {
                stopChildren(milli,nano,false);
            }
        }else{
            this.milliStart = milli;
            this.nanoStart = nano;

        }
        //creates a gap but more accurately reflects time being measured by ignoring time taken to stop
        milli = System.currentTimeMillis();
        nano = System.nanoTime();
        SystemTimer newTimer =new SystemTimer(name,milli,nano,parallel);
        children.push(newTimer);
        return newTimer;
    }

    /**
     * Stop this timer and all descendant timers
     */
    public void stop(){
        if(isActive()) {
            long milli = System.currentTimeMillis();
            long nano = System.nanoTime();
            this.stop(milli, nano, true);
        }
    }
    public boolean hasChildren(){return !children.isEmpty();}
    public List<SystemTimer> getChildren(){
        return Collections.unmodifiableList(children);
    }

    private SystemTimer current(){
        if(!children.isEmpty()){
            return children.getLast();
        }else{
            return NULL;//TODO return this instead of NULL?
        }
    }
    private void stop(long milliStop,long nanoStop,boolean stopParallel){
        if(isActive()) {
            this.milliStop = milliStop;
            this.nanoStop = nanoStop;
            stopChildren(milliStop,nanoStop,stopParallel);
        }
    }
    private void stopChildren(long milliStop,long nanoStop,boolean stopParallel){
        int count = children.size();
        for(int i=0; i<count; i++){
            SystemTimer timer = children.get(i);
            if(stopParallel || !timer.isParallel) {
                children.get(i).stop(milliStop, nanoStop, stopParallel);
            }
        }
    }

    public Json getJson(){
        Json rtrn = new Json();
        rtrn.set("name",getName());
        rtrn.set("start",getStart());
        rtrn.set("stop",getStop());
        rtrn.set("nanos",nanoTime());
        rtrn.set("millis",milliTime());
        if(hasChildren()){
            Json childrenJson = new Json();
            for(SystemTimer child : children){
                childrenJson.add(child.getJson());
            }
            rtrn.set("timers",childrenJson);
        }
        return rtrn;
    }

}
