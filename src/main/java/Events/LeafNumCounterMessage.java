package Events;

import se.sics.kompics.KompicsEvent;

public class LeafNumCounterMessage implements KompicsEvent {
    public String src;
    public String dst;
    public int token;
    public LeafNumCounterMessage(String src, String dst, int token) {
        this.src = src;
        this.dst = dst;
        this.token = token;
    }
}
