package perf.yaup.time;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.time.SystemTimer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SystemTimerTest {


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
