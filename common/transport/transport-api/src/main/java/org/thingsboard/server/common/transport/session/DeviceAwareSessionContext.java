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
package org.thingsboard.server.common.transport.session;

import lombok.Data;
import lombok.Getter;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.session.SessionContext;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceInfoProto;

import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
public abstract class DeviceAwareSessionContext implements SessionContext {

    @Getter
    protected final UUID sessionId;
    @Getter
    private volatile DeviceId deviceId;
    @Getter
    private volatile DeviceInfoProto deviceInfo;

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public void setDeviceInfo(DeviceInfoProto deviceInfo) {
        this.deviceInfo = deviceInfo;
        this.deviceId = new DeviceId(new UUID(deviceInfo.getDeviceIdMSB(), deviceInfo.getDeviceIdLSB()));
    }

    public boolean isConnected() {
        return deviceInfo != null;
    }
}
