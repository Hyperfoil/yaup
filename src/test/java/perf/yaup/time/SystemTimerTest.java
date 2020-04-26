package perf.yaup.time;

import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.time.SystemTimer;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SystemTimerTest {

   private void sleep(long ms){
      try {
         Thread.sleep(ms);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

   }

   @Test
   public void stops(){
      SystemTimer timer = new SystemTimer("one");
      timer.start();
      sleep(100);
      timer.start("two");
      sleep(100);
      timer.start("three");
      sleep(100);
      timer.stop();

      Json json = timer.getJson();
      System.out.println(json.toString(2));
   }

   @Test(timeout = 1000)
   public void start_notParallel_under_one_second(){
      int count = 1_000_000;
      SystemTimer timer = new SystemTimer("one");
      long start = System.currentTimeMillis();
      for(int i=0; i<count; i++){
         timer.start("next",false);
      }
      long stop = System.currentTimeMillis();

      assertTrue("create "+count+" children timers in "+StringUtil.durationToString(stop-start)+" <1s ",(stop-start)<1000);
   }


   @Test
   public void parallel_children_not_stop_parent(){
      SystemTimer timer = new SystemTimer("top");
      SystemTimer parallelTimer = timer.start("parallel",true);
      try {
         Thread.sleep(100);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      parallelTimer.start("parallel2",false);
      SystemTimer serialTimer = timer.start("serial");
      try {
         Thread.sleep(100);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      parallelTimer.start("parallel3",false);
      try {
         Thread.sleep(100);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      timer.stop();

      Json json = timer.getJson();
      System.out.println(json.toString(2));
      assertTrue("expect timer timers",json.has("timers") && json.get("timers") instanceof Json);
      Json children = json.getJson("timers");
      assertEquals("expect 2 timers",2,children.size());
      Json serial = children.getJson(0);
      Json parallel = children.getJson(1);
      if(serial.getString("name").equals("parallel")){
         Json tmp = serial;
         serial = parallel;
         parallel = tmp;
      }

   }

   @Test
   public void serial_not_stop_parallel(){
      SystemTimer timer = new SystemTimer("top");
      timer.start("parallel",true);
      try {
         Thread.sleep(100);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      SystemTimer serialTimer = timer.start("serial");
      try {
         Thread.sleep(100);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      serialTimer.stop();
      try {
         Thread.sleep(100);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      timer.stop();

      Json json = timer.getJson();
      assertTrue("expect timer timers",json.has("timers") && json.get("timers") instanceof Json);
      Json children = json.getJson("timers");
      assertEquals("expect 2 timers",2,children.size());
      Json serial = children.getJson(0);
      Json parallel = children.getJson(1);
      if(serial.getString("name").equals("parallel")){
         Json tmp = serial;
         serial = parallel;
         parallel = tmp;
      }

      assertTrue("parallel should stop after serial parallel="+parallel.getLong("stop")+" serial="+serial.getLong("stop"),parallel.getLong("stop") > serial.getLong("stop"));
      assertEquals("parallel timer should stop when parent timer stops",parallel.getLong("stop"),json.getLong("stop"));

   }
}
