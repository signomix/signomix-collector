package com.signomix.collector.adapters.driven;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

public class MqttAdapter {

    @Channel("data")
    Emitter<byte[]> dataEmitter;
    
}
