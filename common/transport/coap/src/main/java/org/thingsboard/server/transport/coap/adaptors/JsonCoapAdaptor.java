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
package org.thingsboard.server.transport.coap.adaptors;

import java.util.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.msg.kv.AttributesKVMsg;
import org.thingsboard.server.common.msg.session.SessionContext;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.springframework.stereotype.Component;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.CoapTransportResource;

@Component("JsonCoapAdaptor")
@Slf4j
public class JsonCoapAdaptor implements CoapTransportAdaptor {

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, Request inbound) throws AdaptorException {
        String payload = validatePayload(sessionId, inbound);
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, Request inbound) throws AdaptorException {
        String payload = validatePayload(sessionId, inbound);
        try {
            return JsonConverter.convertToAttributesProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, Request inbound) throws AdaptorException {
        List<String> queryElements = inbound.getOptions().getUriQuery();
        TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
        if (queryElements != null && queryElements.size() > 0) {
            Set<String> clientKeys = toKeys(queryElements, "clientKeys");
            Set<String> sharedKeys = toKeys(queryElements, "sharedKeys");
            if (clientKeys != null) {
                result.addAllClientAttributeNames(clientKeys);
            }
            if (sharedKeys != null) {
                result.addAllSharedAttributeNames(sharedKeys);
            }
        }
        return result.build();
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, Request inbound) throws AdaptorException {
        Optional<Integer> requestId = CoapTransportResource.getRequestId(inbound);
        String payload = validatePayload(sessionId, inbound);
        JsonObject response = new JsonParser().parse(payload).getAsJsonObject();
        return TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId.orElseThrow(() -> new AdaptorException("Request id is missing!")))
                .setPayload(response.toString()).build();
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, Request inbound) throws AdaptorException {
        String payload = validatePayload(sessionId, inbound);
        return JsonConverter.convertToServerRpcRequest(new JsonParser().parse(payload), 0);
    }

    @Override
    public Response convertToPublish(CoapTransportResource.CoapSessionListener session, TransportProtos.AttributeUpdateNotificationMsg msg) throws AdaptorException {
        return getObserveNotification(session.getNextSeqNumber(), JsonConverter.toJson(msg));
    }

    @Override
    public Response convertToPublish(CoapTransportResource.CoapSessionListener session, TransportProtos.ToDeviceRpcRequestMsg msg) throws AdaptorException {
        return getObserveNotification(session.getNextSeqNumber(), JsonConverter.toJson(msg, true));
    }

    @Override
    public Response convertToPublish(CoapTransportResource.CoapSessionListener coapSessionListener, TransportProtos.ToServerRpcResponseMsg msg) throws AdaptorException {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        JsonElement result = JsonConverter.toJson(msg);
        response.setPayload(result.toString());
        return response;
    }

    @Override
    public Response convertToPublish(CoapTransportResource.CoapSessionListener session, TransportProtos.GetAttributeResponseMsg msg) throws AdaptorException {
        if (msg.getClientAttributeListCount() == 0 && msg.getSharedAttributeListCount() == 0 && msg.getDeletedAttributeKeysCount() == 0) {
            return new Response(CoAP.ResponseCode.NOT_FOUND);
        } else {
            Response response = new Response(CoAP.ResponseCode.CONTENT);
            JsonObject result = JsonConverter.toJson(msg);
            response.setPayload(result.toString());
            return response;
        }
    }

    private Response getObserveNotification(int seqNumber, JsonElement json) {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        response.getOptions().setObserve(seqNumber);
        response.setPayload(json.toString());
        return response;
    }

    private String validatePayload(UUID sessionId, Request inbound) throws AdaptorException {
        String payload = inbound.getPayloadString();
        if (payload == null) {
            log.warn("[{}] Payload is empty!", sessionId);
            throw new AdaptorException(new IllegalArgumentException("Payload is empty!"));
        }
        return payload;
    }

    private Set<String> toKeys(List<String> queryElements, String attributeName) throws AdaptorException {
        String keys = null;
        for (String queryElement : queryElements) {
            String[] queryItem = queryElement.split("=");
            if (queryItem.length == 2 && queryItem[0].equals(attributeName)) {
                keys = queryItem[1];
            }
        }
        if (keys != null && !StringUtils.isEmpty(keys)) {
            return new HashSet<>(Arrays.asList(keys.split(",")));
        } else {
            return null;
        }
    }

}
