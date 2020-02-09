package Events;

import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;
import java.util.HashMap;

public class MapperMessage implements KompicsEvent {
    public String src;
    public String dst;
    public ArrayList<String> dictionary;
    public int begin;
    public int end;
    public MapperMessage(String src, String dst, ArrayList<String> dictionary, int begin, int end) {
        this.src = src;
        this.dst = dst;
        this.dictionary = dictionary;
        this.begin = begin;
        this.end = end;
    }
}
