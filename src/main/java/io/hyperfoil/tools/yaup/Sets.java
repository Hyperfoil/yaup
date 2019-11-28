package io.hyperfoil.tools.yaup;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 */
@SuppressWarnings("unused")
public class Sets {

    public static <T> Set<T> getOverlap(Set<T> a, Set<T> b){
        Set<T> rtrn = new HashSet<>(a);
        rtrn.retainAll(b);
        return rtrn;
    }
    @SafeVarargs
    public static <T> Set<T> of(T...t){
        LinkedHashSet<T> rtrn = new LinkedHashSet<>();
        if(t!=null && t.length > 0){
            rtrn.addAll(Arrays.asList(t));
        }
        return rtrn;
    }
    public static <T> Set<T> unique(Set<T> a, Set<T> b){
        if(b.containsAll(a)){
            return Collections.emptySet();
        }
        Set<T> rtrn = new HashSet<>(a);
        rtrn.removeAll(b);
        return rtrn;
    }
    public static <T> Set<T> join(Set<T> a, Set<T> b){
        Set<T> rtrn = new LinkedHashSet<>();
        rtrn.addAll(a);
        rtrn.addAll(b);
        return rtrn;
    }

}
