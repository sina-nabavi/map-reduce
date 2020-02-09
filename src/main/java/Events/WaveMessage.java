package Events;

import se.sics.kompics.KompicsEvent;

public class WaveMessage implements KompicsEvent {
    public String src;
    public String dst;
    public int dist;
    public String waveId;
    public WaveMessage(String src, String dst, int dist, String waveId) {
        this.src = src;
        this.dst = dst;
        this.dist = dist;
        this.waveId = waveId;
    }
}
