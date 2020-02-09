package Events;

import se.sics.kompics.KompicsEvent;

import java.util.HashMap;

public class ReducerMessage implements KompicsEvent {
    public String src;
    public String dst;
    public HashMap<String,Integer> words = new HashMap<>();
    public ReducerMessage(String src, String dst, HashMap<String,Integer> words) {
        this.src = src;
        this.dst = dst;
        this.words = words;
    }
}
