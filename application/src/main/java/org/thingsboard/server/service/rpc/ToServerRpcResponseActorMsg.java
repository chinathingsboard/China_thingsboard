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
package org.thingsboard.server.service.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.thingsboard.rule.engine.api.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.core.ToServerRpcResponseMsg;

/**
 * Created by ashvayka on 16.04.18.
 */
@ToString
@RequiredArgsConstructor
public class ToServerRpcResponseActorMsg implements ToDeviceActorNotificationMsg {

    @Getter
    private final TenantId tenantId;

    @Getter
    private final DeviceId deviceId;

    @Getter
    private final ToServerRpcResponseMsg msg;

    @Override
    public MsgType getMsgType() {
        return MsgType.SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG;
    }
}
