/**
 * Copyright © 2016-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.mqtt.session;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class DeviceSessionCtx extends MqttDeviceAwareSessionContext {

    @Getter
    private ChannelHandlerContext channel;
    private AtomicInteger msgIdSeq = new AtomicInteger(0);

    public DeviceSessionCtx(UUID sessionId, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap) {
        super(sessionId, mqttQoSMap);
    }

    public void setChannel(ChannelHandlerContext channel) {
        this.channel = channel;
    }

    public int nextMsgId() {
        return msgIdSeq.incrementAndGet();
    }
}
