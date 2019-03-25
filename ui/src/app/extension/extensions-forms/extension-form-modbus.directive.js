/*
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
import 'brace/ext/language_tools';
import 'brace/mode/json';
import 'brace/theme/github';

import './extension-form.scss';

/* eslint-disable angular/log */

import extensionFormModbusTemplate from './extension-form-modbus.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ExtensionFormModbusDirective($compile, $templateCache, $translate, types) {


    var linker = function(scope, element) {

        function TcpTransport() {
            this.type = "tcp",
            this.host = "localhost",
            this.port = 502,
            this.timeout = 5000,
            this.reconnect = true,
            this.rtuOverTcp = false
        }

        function UdpTransport() {
            this.type = "udp",
            this.host = "localhost",
            this.port = 502,
            this.timeout = 5000
        }

        function RtuTransport() {
            this.type = "rtu",
            this.portName = "COM1",
            this.encoding = "ascii",
            this.timeout = 5000,
            this.baudRate = 115200,
            this.dataBits = 7,
            this.stopBits = 1,
            this.parity ="even"
        }

        function Server() {
            this.transport = new TcpTransport();
            this.devices = []
        }
		
        function Device() {
            this.unitId = 1;
            this.deviceName = "";
            this.attributesPollPeriod = 1000;
            this.timeseriesPollPeriod = 1000;
            this.attributes = [];
            this.timeseries = [];
        }

        function Tag(globalPollPeriod) {
            this.tag = "";
            this.type = "long";
            this.pollPeriod = globalPollPeriod;
            this.functionCode = 3;
            this.address = 0;
            this.registerCount = 1;
            this.bit = 0;
            this.byteOrder = "BIG";
        }


        var template = $templateCache.get(extensionFormModbusTemplate);
        element.html(template);

        scope.types = types;
        scope.theForm = scope.$parent.theForm;


        if (!scope.configuration.servers.length) {
            scope.configuration.servers.push(new Server());
        }

        scope.addServer = function(serversList) {
            serversList.push(new Server());
            scope.theForm.$setDirty();
        };

        scope.addDevice = function(deviceList) {
            deviceList.push(new Device());
            scope.theForm.$setDirty();
        };

        scope.addNewAttribute = function(device) {
            device.attributes.push(new Tag(device.attributesPollPeriod));
            scope.theForm.$setDirty();
        };

        scope.addNewTimeseries = function(device) {
            device.timeseries.push(new Tag(device.timeseriesPollPeriod));
            scope.theForm.$setDirty();
        };

        scope.removeItem = (item, itemList) => {
            var index = itemList.indexOf(item);
            if (index > -1) {
                itemList.splice(index, 1);
            }
            scope.theForm.$setDirty();
        };

        scope.onTransportChanged = function(server) {
            var type = server.transport.type;

            if (type === "tcp") {
                server.transport = new TcpTransport();
            } else if (type === "udp") {
                server.transport = new UdpTransport();
            } else if (type === "rtu") {
                server.transport = new RtuTransport();
            }

            scope.theForm.$setDirty();
        };
        
        $compile(element.contents())(scope);


        scope.collapseValidation = function(index, id) {
            var invalidState = angular.element('#'+id+':has(.ng-invalid)');
            if(invalidState.length) {
                invalidState.addClass('inner-invalid');
            }
        };

        scope.expandValidation = function (index, id) {
            var invalidState = angular.element('#'+id);
            invalidState.removeClass('inner-invalid');
        };

    };

    return {
        restrict: "A",
        link: linker,
        scope: {
            configuration: "=",
            isAdd: "="
        }
    }
}