// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.api.command.admin.diagnostics;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.RetrieveDiagnosticsResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.diagnostics.RetrieveDiagnosticsService;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

@APICommand(name = RetrieveDiagnosticsCmd.APINAME,
        description = "Retrieves diagnostics files from host VMs",
        responseObject = RetrieveDiagnosticsResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.12.0",
        authorized = {RoleType.Admin})
public class RetrieveDiagnosticsCmd extends BaseAsyncCmd {

    private static final Logger LOGGER = Logger.getLogger(RetrieveDiagnosticsCmd.class);

    public static final String APINAME = "retrieveDiagnostics";

    @Inject
    private RetrieveDiagnosticsService retrieveDiagnosticsService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = DomainRouterResponse.class,
            required = true,
            description = "The host VM type that the diagnostics files requested are to be retrieved from")
    private Long id;


    @Parameter(name = ApiConstants.TYPE,
            type = CommandType.STRING,
            required = true,
            description = "The type of diagnostics files requested, if DIAGNOSTICS_TYPE is not provided then the default files specified in the database will be retrieved")
    private String type;

    @Parameter(name = ApiConstants.DETAIL,
            type = CommandType.STRING,
            description = "Optional comma separated list of diagnostics files or items, can be specified as filenames only or full file path. These come in addition to the defaults set in diagnosticstype")
    private String optionalListOfFiles;

    // Configuration parameters //////

    @Parameter(name = ApiConstants.TIMEOUT,
            type = CommandType.STRING,
            description = "Time out setting in seconds for the overall API call.")
    private String timeOut;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOptionalListOfFiles() {
        return optionalListOfFiles;
    }

    public void setOptionalListOfFiles(String optionalListOfFiles) {
        this.optionalListOfFiles = optionalListOfFiles;
    }

    public String getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(String timeOut) {
        this.timeOut = timeOut;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getEventType() {
        VirtualMachine.Type type = _mgr.findSystemVMTypeById(getId());
        return type.toString();
    }
    public String getType() {
        return type;
    }


    @Override
    public void execute() {
        CallContext.current().setEventDetails("Vm Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getId()));

        try {
            RetrieveDiagnosticsResponse retrieveDiagnosticsResponse = retrieveDiagnosticsService.getDiagnosticsFiles(this);
            retrieveDiagnosticsResponse.setObjectName("retrieved information");
            retrieveDiagnosticsResponse.setResponseName(getCommandName());
            this.setResponseObject(retrieveDiagnosticsResponse);
        } catch (InvalidParameterValueException ipve) {
            LOGGER.error("Failed to retrieve diagnostics files from ", ipve);
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, ipve.getMessage());
        } catch (ConfigurationException cre) {
            LOGGER.error("Failed to retrieve diagnostics files from ", cre);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, cre.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseAsyncCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventDescription() {
        return "Retrieved diagnostics files from host =" + id;
    }
}
