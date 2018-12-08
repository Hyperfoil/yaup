package perf.yaup;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class HashedSetsTest {

    public ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),Runtime.getRuntime().availableProcessors(),10,TimeUnit.MINUTES,new LinkedBlockingQueue<>());


    @Test
    public void put_parallel_same_key(){
        final HashedSets<String,String> set = new HashedSets<>();

        AtomicBoolean failed = new AtomicBoolean(false);

        List<Callable<Boolean>> toDo = new ArrayList<>();

        StringBuffer sb = new StringBuffer();

        int count = Runtime.getRuntime().availableProcessors();
        for(int i=0; i<count; i++){
            toDo.add(()->{
                boolean rtrn = true;
                try {
                    set.put("KEY", "" + System.currentTimeMillis());
                }catch(Exception e){
                    rtrn = false;
                    sb.append(e.getMessage()+"\n");
                }
                return rtrn;
            });
        }
        try {
            boolean ok = executor.invokeAll(toDo).stream().map(f-> {
                boolean rtrn = false;
                try {
                    rtrn = f.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                return rtrn;
            }).collect(Collectors.reducing(Boolean::logicalAnd)).get();

            assertTrue("Expect all ok but something failed:\n"+sb.toString(),ok);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
