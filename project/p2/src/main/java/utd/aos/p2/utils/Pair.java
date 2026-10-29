package utd.aos.p2.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// models a pair of values.
public class Pair<K, V> implements Serializable {
    private K k;
    private V v;

    public static <A,B> List<Pair<A, B>> zip(List<A> a, List<B> b) {
        List<Pair<A,B>> result = new ArrayList<>();
        for(int i = 0; i < a.size() && i < b.size(); i++) {
            result.add(new Pair<>(a.get(i), b.get(i)));
        }
        return result;
    }

    public Pair(K k, V v) {
        this.k = k;
        this.v = v;
    }

    public K getKey() {
        return k;
    }

    public V getValue() {
        return v;
    }

    public String toString() {
        return "{\"key\": " + k.toString() + ", \"value\": " + v.toString() + "}";
    }
}
