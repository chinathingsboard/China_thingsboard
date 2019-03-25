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
package org.thingsboard.server.common.msg.cluster;

import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;

@Data
public class SendToClusterMsg implements TbActorMsg {

    private TbActorMsg msg;
    private EntityId entityId;

    public SendToClusterMsg(EntityId entityId, TbActorMsg msg) {
        this.entityId = entityId;
        this.msg = msg;
    }


    @Override
    public MsgType getMsgType() {
        return MsgType.SEND_TO_CLUSTER_MSG;
    }
}