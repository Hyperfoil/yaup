package io.hyperfoil.tools.yaup.time;

import io.hyperfoil.tools.yaup.json.Json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
        public SystemTimer start(String name, Json json, boolean parallel){return this;}
        @Override
        public boolean isActive(){return false;}
    };

    private final boolean isParallel;
    private boolean hasParallel; //helper to track if there is potentially a running parallel child
    private long nanoStart;
    private long milliStart;
    private long nanoStop=-1;
    private long milliStop=-1;
    private String name;
    private Json data;

    private SystemTimer parent;
    private List<SystemTimer> children;


    public SystemTimer(String name){
        this(name,0,0, false);
    }
    public SystemTimer(String name, long milli, long nano, boolean isParallel){
        this.name = name;
        this.children = new ArrayList<>(); // this risks NPE when concurrently modified :(
        this.milliStart = milli;
        this.nanoStart = nano;
        this.isParallel = isParallel;
        this.hasParallel = false;
        this.parent = null;
        this.data = new Json(false);
    }

    /**
     * Starts the current timer if it was not already started
     * @return true if the timer was started, or false if already active
     */
    public boolean start(){
        return start((Json) null);
    }

    public boolean hasParent(){return parent !=null && parent != NULL;}
    public SystemTimer getParent(){return parent;}

    /**
     * Starts the current timer if it was not already started
     * @param data additional data to store with the timer
     * @return
     */
    public boolean start(Json data){
        if(isStopped() || isActive()){
            return false;
        }
        if(data!=null){
            this.data.merge(data);
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
        return start(name,null,false);
    }
    /**
     * Starts a new child timer and ends the current child timer if !parallel.
     * @param name the name of the new timer. Names are not unique
     * @param parallel if true, leave current child timer running, otherwise stop child timers
     * @return new SystemTime
     */
    public SystemTimer start(String name, boolean parallel){
        return start(name,null,parallel);
    }
    /**
     * Starts a new child timer and ends the current child timer if !parallel.
     * @param name the name of the new timer. Names are not unique
     * @param data to associate with the new SystemTimer
     * @param parallel if true, leave current child timer running, otherwise stop child timers
     * @return new SystemTime
     */
    public SystemTimer start(String name, Json data, boolean parallel){
//TODO throwing breaks async ScriptCmd
//        if(isStopped()){
//            throw new IllegalStateException("cannot start("+name.trim()+") after stop("+this.name+")");
//        }

        hasParallel = hasParallel || parallel;
        long milli = System.currentTimeMillis();
        long nano = System.nanoTime();
        if(isActive()) {
            if(!parallel) {
                stopChildren(milli,nano,null,false);
            }
        }else{
            //this isn't started so it needs to start too
            this.milliStart = milli;
            this.nanoStart = nano;

        }
        //creates a gap but more accurately reflects time being measured by ignoring time taken to stop
        milli = System.currentTimeMillis();
        nano = System.nanoTime();
        SystemTimer newTimer =new SystemTimer(name,milli,nano,parallel);
        newTimer.parent = this;
        newTimer.data.merge(data);
        children.add(newTimer);
        return newTimer;
    }

    public Json getData(){return data;}

    /**
     * Stop this timer and all descendant timers
     */
    public void stop(){
        stop((Json)null);
    }

    public void stop(Json data){
        if(isActive()) {
            long milli = System.currentTimeMillis();
            long nano = System.nanoTime();
            this.stop(milli, nano, data,true);
        }
    }
    public boolean hasChildren(){return !children.isEmpty();}
    public List<SystemTimer> getChildren(){
        return Collections.unmodifiableList(children);
    }

    private SystemTimer current(){
        if(!children.isEmpty()){
            return children.get(children.size()-1);
        }else{
            return NULL;//TODO return this instead of NULL?
        }
    }
    private void stop(long milliStop,long nanoStop,boolean stopParallel){
        stop(milliStop,nanoStop,null,stopParallel);
    }
    private void stop(long milliStop,long nanoStop,Json data,boolean stopParallel){
        if(isActive() && ( stopParallel || !isParallel)) {
            this.milliStop = milliStop;
            this.nanoStop = nanoStop;
            stopChildren(milliStop,nanoStop,data,stopParallel);
        }
    }

    /**
     * stop running children. If !stopParallel then search from back until find a not-running child.
     * If stopParallel then full scan for any parallel children if there is at least one parallel child
     * @param milliStop
     * @param nanoStop
     * @param stopParallel
     */
    private void stopChildren(long milliStop,long nanoStop,Json data, boolean stopParallel){
        int counter = children.size()-1;
        while(counter>=0 && ( (stopParallel && hasParallel) || children.get(counter).isActive())){
            children.get(counter).stop(milliStop, nanoStop, data,stopParallel);
            counter--;
        }
        if(stopParallel){
            hasParallel = false;
        }

    }

    public Json getJson(){
        Json rtrn = new Json();
        rtrn.set("name",getName());
        rtrn.set("start",getStart());
        rtrn.set("stop",getStop());
        rtrn.set("nanos",nanoTime());
        rtrn.set("millis",milliTime());
        if(!data.isEmpty()) {
            rtrn.set("data", data.clone());
        }

        if(hasChildren()){
            Json childrenJson = new Json();
            for(int i=0; i<children.size(); i++){ //was getting ConcurrentModificationException, want snapshot read?
                childrenJson.add(children.get(i).getJson());
            }
            rtrn.set("timers",childrenJson);
        }
        return rtrn;
    }

}
