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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.EntitiesRelatedDeviceIdAsyncLoader;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "device attributes",
        configClazz = TbGetDeviceAttrNodeConfiguration.class,
        nodeDescription = "Add Originators Related Device Attributes and Latest Telemetry value into Message Metadata",
        nodeDetails = "If Attributes enrichment configured, <b>CLIENT/SHARED/SERVER</b> attributes are added into Message metadata " +
                "with specific prefix: <i>cs/shared/ss</i>. Latest telemetry value added into metadata without prefix. " +
                "To access those attributes in other nodes this template can be used " +
                "<code>metadata.cs_temperature</code> or <code>metadata.shared_limit</code> ",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeDeviceAttributesConfig")
public class TbGetDeviceAttrNode extends TbAbstractGetAttributesNode<TbGetDeviceAttrNodeConfiguration, DeviceId> {

    @Override
    protected TbGetDeviceAttrNodeConfiguration loadGetAttributesNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbGetDeviceAttrNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<DeviceId> findEntityIdAsync(TbContext ctx, EntityId originator) {
        return EntitiesRelatedDeviceIdAsyncLoader.findDeviceAsync(ctx, originator, config.getDeviceRelationsQuery());
    }
}
