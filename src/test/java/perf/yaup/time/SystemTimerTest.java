package perf.yaup.time;

import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.time.SystemTimer;
import org.junit.Ignore;
import org.junit.Test;

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

   @Test @Ignore
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

   }

   @Test(timeout = 4000)  @Ignore
   public void start_notParallel_under_one_second(){
      int count = 1_000_000;
      SystemTimer timer = new SystemTimer("one");
      long start = System.currentTimeMillis();
      for(int i=0; i<count; i++){
         timer.start("next",null,false);
      }
      long stop = System.currentTimeMillis();

      assertTrue("create "+count+" children timers in "+StringUtil.durationToString(stop-start)+" <1s ",(stop-start)<1100);
   }


   @Test
   public void parallel_children_not_stop_parent(){
      SystemTimer timer = new SystemTimer("top");
      SystemTimer parallelTimer = timer.start("parallel",null,true);
      sleep(100);
      parallelTimer.start("parallel2",null,false);
      SystemTimer serialTimer = timer.start("serial");
      sleep(100);
      parallelTimer.start("parallel3",null,false);
      sleep(100);
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
      //TODO check serial and parallel
   }

   @Test
   public void serial_not_stop_parallel(){
      SystemTimer timer = new SystemTimer("top");
      timer.start("parallel",null,true);
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
