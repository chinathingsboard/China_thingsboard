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
package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class RuleChain extends SearchTextBasedWithAdditionalInfo<RuleChainId> implements HasName, HasTenantId {

    private static final long serialVersionUID = -5656679015121935465L;

    private TenantId tenantId;
    private String name;
    private RuleNodeId firstRuleNodeId;
    private boolean root;
    private boolean debugMode;
    private transient JsonNode configuration;
    @JsonIgnore
    private byte[] configurationBytes;

    public RuleChain() {
        super();
    }

    public RuleChain(RuleChainId id) {
        super(id);
    }

    public RuleChain(RuleChain ruleChain) {
        super(ruleChain);
        this.tenantId = ruleChain.getTenantId();
        this.name = ruleChain.getName();
        this.firstRuleNodeId = ruleChain.getFirstRuleNodeId();
        this.root = ruleChain.isRoot();
        this.setConfiguration(ruleChain.getConfiguration());
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }

    public JsonNode getConfiguration() {
        return SearchTextBasedWithAdditionalInfo.getJson(() -> configuration, () -> configurationBytes);
    }

    public void setConfiguration(JsonNode data) {
        setJson(data, json -> this.configuration = json, bytes -> this.configurationBytes = bytes);
    }

}
