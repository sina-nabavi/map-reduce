package Events;

import se.sics.kompics.KompicsEvent;

public class EchoMessage implements KompicsEvent {
    public String src;
    public String dst;
    public int dist;
    public String echoId;
    public EchoMessage(String src, String dst, int dist, String echoId) {
        this.src = src;
        this.dst = dst;
        this.dist = dist;
        this.echoId = echoId;
    }
}
