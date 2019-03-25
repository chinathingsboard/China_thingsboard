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

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.CoapTransportResource;

import java.util.UUID;
import java.util.Optional;

public interface CoapTransportAdaptor {

    TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, Request inbound) throws AdaptorException;

    TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, Request inbound) throws AdaptorException;

    TransportProtos.GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, Request inbound) throws AdaptorException;

    TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, Request inbound) throws AdaptorException;

    TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, Request inbound) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener coapSessionListener, TransportProtos.ToServerRpcResponseMsg msg) throws AdaptorException;

}
