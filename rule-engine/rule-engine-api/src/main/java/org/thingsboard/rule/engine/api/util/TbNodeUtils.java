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
package org.thingsboard.rule.engine.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;

/**
 * Created by ashvayka on 19.01.18.
 */
public class TbNodeUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String VARIABLE_TEMPLATE = "${%s}";


    public static <T> T convert(TbNodeConfiguration configuration, Class<T> clazz) throws TbNodeException {
        try {
            return mapper.treeToValue(configuration.getData(), clazz);
        } catch (JsonProcessingException e) {
            throw new TbNodeException(e);
        }
    }

    public static String processPattern(String pattern, TbMsgMetaData metaData) {
        String result = new String(pattern);
        for (Map.Entry<String,String> keyVal  : metaData.values().entrySet()) {
            result = processVar(result, keyVal.getKey(), keyVal.getValue());
        }
        return result;
    }

    private static String processVar(String pattern, String key, String val) {
        String varPattern = String.format(VARIABLE_TEMPLATE, key);
        return pattern.replace(varPattern, val);
    }

}
