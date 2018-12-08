package perf.yaup;

import perf.yaup.xml.pojo.XmlComparison;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JarMain {

    public static void main(String[] vargs) {
        List<String> args = new ArrayList<>(Arrays.asList(vargs));
        if(!args.isEmpty()){
            String tool = args.remove(0);
            switch (tool){
                case "xml-diff":
                    XmlComparison.main(args.toArray(new String[]{}));
            }
        }
    }
}
