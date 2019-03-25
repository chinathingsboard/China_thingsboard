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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DeviceController extends BaseController {

    public static final String DEVICE_ID = "deviceId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public Device getDeviceById(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            return checkDeviceId(deviceId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDevice(@RequestBody Device device) throws ThingsboardException {
        try {
            device.setTenantId(getCurrentUser().getTenantId());

            Operation operation = device.getId() == null ? Operation.CREATE : Operation.WRITE;

            accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, operation,
                    device.getId(), device);

            Device savedDevice = checkNotNull(deviceService.saveDevice(device));

            actorService
                    .onDeviceNameOrTypeUpdate(
                            savedDevice.getTenantId(),
                            savedDevice.getId(),
                            savedDevice.getName(),
                            savedDevice.getType());

            logEntityAction(savedDevice.getId(), savedDevice,
                    savedDevice.getCustomerId(),
                    device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            if (device.getId() == null) {
                deviceStateService.onDeviceAdded(savedDevice);
            } else {
                deviceStateService.onDeviceUpdated(savedDevice);
            }
            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), device,
                    null, device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDevice(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.DELETE);
            deviceService.deleteDevice(getCurrentUser().getTenantId(), deviceId);

            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.DELETED, null, strDeviceId);

            deviceStateService.onDeviceDeleted(device);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE),
                    null,
                    null,
                    ActionType.DELETED, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToCustomer(@PathVariable("customerId") String strCustomerId,
                                         @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId, Operation.READ);

            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            checkDeviceId(deviceId, Operation.ASSIGN_TO_CUSTOMER);

            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(getCurrentUser().getTenantId(), deviceId, customerId));

            logEntityAction(deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strDeviceId, strCustomerId, customer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strDeviceId, strCustomerId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Device unassignDeviceFromCustomer(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.UNASSIGN_FROM_CUSTOMER);
            if (device.getCustomerId() == null || device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Device isn't assigned to any customer!");
            }
            Customer customer = checkCustomerId(device.getCustomerId(), Operation.READ);

            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromCustomer(getCurrentUser().getTenantId(), deviceId));

            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, strDeviceId, customer.getId().toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToPublicCustomer(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.ASSIGN_TO_CUSTOMER);
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(device.getTenantId());
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(getCurrentUser().getTenantId(), deviceId, publicCustomer.getId()));

            logEntityAction(deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strDeviceId, publicCustomer.getId().toString(), publicCustomer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}/credentials", method = RequestMethod.GET)
    @ResponseBody
    public DeviceCredentials getDeviceCredentialsByDeviceId(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.READ_CREDENTIALS);
            DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(getCurrentUser().getTenantId(), deviceId));
            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_READ, null, strDeviceId);
            return deviceCredentials;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_READ, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device/credentials", method = RequestMethod.POST)
    @ResponseBody
    public DeviceCredentials saveDeviceCredentials(@RequestBody DeviceCredentials deviceCredentials) throws ThingsboardException {
        checkNotNull(deviceCredentials);
        try {
            Device device = checkDeviceId(deviceCredentials.getDeviceId(), Operation.WRITE_CREDENTIALS);
            DeviceCredentials result = checkNotNull(deviceCredentialsService.updateDeviceCredentials(getCurrentUser().getTenantId(), deviceCredentials));
            actorService.onCredentialsUpdate(getCurrentUser().getTenantId(), deviceCredentials.getDeviceId());
            logEntityAction(device.getId(), device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_UPDATED, null, deviceCredentials);
            return result;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_UPDATED, e, deviceCredentials);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Device> getTenantDevices(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDevicesByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(deviceService.findDevicesByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"deviceName"}, method = RequestMethod.GET)
    @ResponseBody
    public Device getTenantDevice(
            @RequestParam String deviceName) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/devices", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Device> getCustomerDevices(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", params = {"deviceIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Device> getDevicesByIds(
            @RequestParam("deviceIds") String[] strDeviceIds) throws ThingsboardException {
        checkArrayParameter("deviceIds", strDeviceIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = user.getCustomerId();
            List<DeviceId> deviceIds = new ArrayList<>();
            for (String strDeviceId : strDeviceIds) {
                deviceIds.add(new DeviceId(toUUID(strDeviceId)));
            }
            ListenableFuture<List<Device>> devices;
            if (customerId == null || customerId.isNullUid()) {
                devices = deviceService.findDevicesByTenantIdAndIdsAsync(tenantId, deviceIds);
            } else {
                devices = deviceService.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, deviceIds);
            }
            return checkNotNull(devices.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", method = RequestMethod.POST)
    @ResponseBody
    public List<Device> findByQuery(@RequestBody DeviceSearchQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getDeviceTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            List<Device> devices = checkNotNull(deviceService.findDevicesByQuery(getCurrentUser().getTenantId(), query).get());
            devices = devices.stream().filter(device -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, Operation.READ, device.getId(), device);
                    return true;
                } catch (ThingsboardException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return devices;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getDeviceTypes() throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> deviceTypes = deviceService.findDeviceTypesByTenantId(tenantId);
            return checkNotNull(deviceTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
