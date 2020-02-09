package Ports;

import Events.*;
import se.sics.kompics.PortType;

public class EdgePort extends PortType {{
    positive(EchoMessage.class);
    negative(EchoMessage.class);
    positive(WaveMessage.class);
    negative(WaveMessage.class);
    positive(LeafNumCounterMessage.class);
    negative(LeafNumCounterMessage.class);
    positive(MapperMessage.class);
    negative(MapperMessage.class);
    positive(ReducerMessage.class);
    negative(ReducerMessage.class);
}}
