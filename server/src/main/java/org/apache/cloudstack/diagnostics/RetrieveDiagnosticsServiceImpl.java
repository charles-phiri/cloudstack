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

package org.apache.cloudstack.diagnostics;

import com.cloud.agent.AgentManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterDetailsDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.domain.dao.DomainDetailsDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.StorageManager;
import com.cloud.storage.secondary.SecondaryStorageListener;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VirtualMachineManager;
import org.apache.cloudstack.api.command.admin.diagnostics.RetrieveDiagnosticsCmd;
import org.apache.cloudstack.api.response.RetrieveDiagnosticsResponse;
import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.diagnostics.dao.RetrieveDiagnosticsDao;
import org.apache.cloudstack.diagnostics.impl.RetrieveDiagnosticsVO;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.log4j.Logger;
import org.codehaus.stax2.ri.typed.NumberUtil;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.*;

public class RetrieveDiagnosticsServiceImpl extends ManagerBase implements RetrieveDiagnosticsService, Configurable {

    private static final Logger s_logger = Logger.getLogger(RetrieveDiagnosticsServiceImpl.class);

    private String _instance;
    private int _mgmtPort = 8250;
    private boolean editConfiguration = false;

    public Map<String, Object> get_configParams() {
        return _configParams;
    }

    public void set_configParams(Map<String, Object> _configParams) {
        this._configParams = _configParams;
    }

    protected Map<String, Object> _configParams = new HashMap<String, Object>();

    private Long _timeOut;
    private Boolean _enabledGC;
    private String _filePath;
    private Float _disableThreshold;
    private Long _fileAge;
    private Long _intervalGC;

    @Inject
    SecondaryStorageVmManager _ssVmMgr;
    private SecondaryStorageListener _listener;

    @Inject
    private AgentManager _agentMgr;

    @Inject
    public AccountManager _accountMgr;

    @Inject
    protected ConfigurationDao _configDao;
    HostDao hostDao;

    @Inject
    private VirtualMachineManager _itMgr;

    @Inject
    protected ResourceManager _resourceMgr;

    @Inject
    DataCenterDao _zoneDao;

    @Inject
    DataCenterDetailsDao _dcDetailsDao;

    @Inject
    RetrieveDiagnosticsDao _retrieveDiagnosticsDao;

    @Inject
    ClusterDetailsDao _clusterDetailsDao;

    @Inject
    ClusterDao _clusterDao;

    @Inject
    PrimaryDataStoreDao _storagePoolDao;

    @Inject
    private ConfigDepot _configDepot;

    @Inject
    StoragePoolDetailsDao _storagePoolDetailsDao;

    @Inject
    AccountDao _accountDao;

    @Inject
    AccountDetailsDao _accountDetailsDao;

    @Inject
    ImageStoreDao _imageStoreDao;

    @Inject
    ImageStoreDetailsDao _imageStoreDetailsDao;

    @Inject
    DomainDetailsDao _domainDetailsDao;

    @Inject
    DomainDao _domainDao;

    @Inject
    StorageManager _storageManager;

    @Inject
    ConfigDepot configDepot;
    @Inject
    MessageBus messageBus;

    DiagnosticsKey<Long> LOGFILES = new ConfigKey<Long>("Advanced", Long.class, "retrieveDiagnostics.retrieval.timeout", "3600",
            "The timeout setting in seconds for the overall API call", true, ConfigKey.Scope.Global);
    DiagnosticsKey<Boolean> PROPERTYFILES = new ConfigKey<>("Advanced", Boolean.class, "retrieveDiagnostics.gc.enabled",
            "true", "Garbage collection on/off switch (true|false", true, ConfigKey.Scope.Global);
    DiagnosticsKey<String> CONFIGURATIONFILES = new ConfigKey<String>("Advanced", String.class, "retrieveDiagnostics.filepath",
            "/tmp", "File path to use on the management server for all temporary data. This allows CloudStack administrators to determine where best to place the files.", true, ConfigKey.Scope.Global);
    DiagnosticsKey<Float> DHCPFILES = new ConfigKey<Float>("Advanced", Float.class, "retrieveDiagnostics.disablethreshold", "0.95",
            "The percentage disk space cut-off before API call will fail", true, ConfigKey.Scope.Global);
    DiagnosticsKey<Long> USERDATA = new ConfigKey<Long>("Advanced", Long.class, "retrieveDiagnostics.max.fileage", "86400",
            "The diagnostics file age in seconds before considered for garbage collection", true, ConfigKey.Scope.Global);
    DiagnosticsKey<Long> LB = new ConfigKey<Long>("Advanced", Long.class, "retrieveDiagnostics.gc.interval", "86400",
            "The interval between garbage collection executions in seconds", true, ConfigKey.Scope.Global);
    DiagnosticsKey<Long> DNS = new ConfigKey<Long>("Advanced", Long.class, "retrieveDiagnostics.gc.interval", "86400",
            "The interval between garbage collection executions in seconds", true, ConfigKey.Scope.Global);
, VPN, IPTABLESretrieve, IPTABLESremove
    DiagnosticsKey<Long> VPN = new ConfigKey<Long>("Advanced", Long.class, "retrieveDiagnostics.gc.interval", "86400",
            "The interval between garbage collection executions in seconds", true, ConfigKey.Scope.Global);
    DiagnosticsKey<Long> IPTABLESretrieve = new ConfigKey<Long>("Advanced", Long.class, "retrieveDiagnostics.gc.interval", "86400",
            "The interval between garbage collection executions in seconds", true, ConfigKey.Scope.Global);
    DiagnosticsKey<Long> IPTABLESremove = new ConfigKey<Long>("Advanced", Long.class, "retrieveDiagnostics.gc.interval", "86400",
            "The interval between garbage collection executions in seconds", true, ConfigKey.Scope.Global);

    public RetrieveDiagnosticsServiceImpl() {
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Initialising configuring values for retrieve diagnostics api : " + name);
        }
        _timeOut = RetrieveDiagnosticsTimeOut.value();
        _fileAge = RetrieveDiagnosticsFileAge.value();
        _enabledGC = enabledGCollector.value();
        _filePath = RetrieveDiagnosticsFilePath.value();
        _disableThreshold = RetrieveDiagnosticsDisableThreshold.value();
        _intervalGC = RetrieveDiagnosticsInterval.value();

        return true;
    }

    public boolean loadDiagnosticsConfiguration(final RetrieveDiagnosticsVO retrieveDiagnosticsVO) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Retrieving configuring values for retrieve diagnostics api : " + getConfigComponentName());
        }
        Map<String, String> configDetails = _retrieveDiagnosticsDao.getDiagnosticsDetails();
        if (configDetails != null) {

        }
        return false;
    }

   public Pair<List<? extends Configuration>, Integer> searchForDiagnosticsConfigurations(final RetrieveDiagnosticsCmd cmd) {
       final Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getId());
       Boolean _enabledGCollector = false;
       final Long _timeOut = NumbersUtil.parseLong(cmd.getTimeOut(), 3600);
       final Float _disableThreshold = NumbersUtil.parseFloat(cmd.getDisableThreshold(), 0.95f);
       String _enabledGC = cmd.getEnabledGC();
       if ("true".equalsIgnoreCase(_enabledGC)) {
           _enabledGCollector = true;
       }
       final Long _intervalGC = NumbersUtil.parseLong(cmd.getIntervalGC(), 86400);
       final Long _fileAge = NumbersUtil.parseLong(cmd.getFileAge(), 86400);
       final String _filePath = cmd.getFilePath();

       final Object id = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getId());

       final Filter searchFilter = new Filter(ConfigurationVO.class, "id", true, 0L, 0L);

       final SearchBuilder<ConfigurationVO> sb = _configDao.createSearchBuilder();
       sb.and("timeout", sb.entity().getValue(), SearchCriteria.Op.EQ);
       sb.and("disablethreshold", sb.entity().getValue(), SearchCriteria.Op.EQ);
       sb.and("enabledGC", sb.entity().getValue(), SearchCriteria.Op.EQ);
       sb.and("intervalGC", sb.entity().getValue(), SearchCriteria.Op.EQ);
       sb.and("fileage", sb.entity().getValue(), SearchCriteria.Op.EQ);
       sb.and("filepath", sb.entity().getValue(), SearchCriteria.Op.EQ);

       final SearchCriteria<ConfigurationVO> sc = sb.create();
       if (_timeOut != null) {
           sc.setParameters("timeout", _timeOut);
       }

       if (_disableThreshold != null) {
           sc.setParameters("disablethreshold", _disableThreshold);
       }

       if (_enabledGCollector != null) {
           sc.setParameters("enabledGC", _enabledGCollector);
       }

       if (_intervalGC != null) {
           sc.setParameters("intervalGC", _intervalGC);
       }

       if (_fileAge != null) {
           sc.setParameters("fileage", _fileAge);
       }

       if (_filePath != null) {
           sc.setParameters("filepath", _filePath);
       }

       final Pair<List<ConfigurationVO>, Integer> result = _configDao.searchAndCount(sc, searchFilter);

       final List<ConfigurationVO> configVOList = new ArrayList<ConfigurationVO>();
       for (final ConfigurationVO param : result.first()) {
           final ConfigurationVO configVo = _configDao.findByName(param.getName());
           if (configVo != null) {
               final ConfigKey<?> key = _configDepot.get(param.getName());
               if (key != null) {
                   configVo.setValue(key.valueIn((Long) id) == null ? null : key.valueIn((Long) id).toString());
                   configVOList.add(configVo);
               } else {
                   s_logger.warn("ConfigDepot could not find parameter " + param.getName());
               }
           } else {
               s_logger.warn("Configuration item  " + param.getName() + " not found.");
           }
       }

        return new Pair<List<? extends Configuration>, Integer>(configVOList, configVOList.size());
       //}

       //return new Pair<List<? extends Configuration>, Integer>(result.first(), result.second());
   }


    @Override
    public RetrieveDiagnosticsResponse getDiagnosticsFiles(RetrieveDiagnosticsCmd cmd) throws AgentUnavailableException, ConfigurationException {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Initialising configuring values for retrieve diagnostics api : " + getConfigComponentName());
        }
        boolean enableGCollector = false;
        if (_configParams == null) {
            _configParams = new HashMap<>();
        }
        if (configure(getConfigComponentName(), _configParams)) {
            if (cmd != null) {
                String _timeOut = cmd.getTimeOut();
                String _disableThreshold = cmd.getDisableThreshold();
                String _enableGC = cmd.getEnabledGC();
                String _intervalGC = cmd.getIntervalGC();
                String _fileAge = cmd.getFileAge();
                final String _filePath = cmd.getFilePath();
                if (!_disableThreshold.isEmpty() || !_timeOut.isEmpty() || !_enableGC.isEmpty()
                            || !_intervalGC.isEmpty() || !_fileAge.isEmpty() || !_filePath.isEmpty()) {
                    final Long _ttimeOut = NumbersUtil.parseLong(_timeOut, 3600);
                    final Float _ddisableThreshold = NumbersUtil.parseFloat(_disableThreshold, 0.95f);
                    final Long _ffileAge = NumbersUtil.parseLong(_fileAge, 86400);
                    final Long _iintervalGC = NumbersUtil.parseLong(_intervalGC, 86400);

                    if ("true".equalsIgnoreCase(_enableGC)) {
                        enableGCollector = true;
                    }

                }
            }

        }
        Long instanceId = cmd.getId();
        List<String> diagnosticsfilesToRetrieve = cmd.getListOfDiagnosticsFiles();
        if (diagnosticsfilesToRetrieve != null) {
            for (String file : diagnosticsfilesToRetrieve) {

            }
        } else {
            //get list of default files from the database table for this diagnostics type


        }



        return null;
    }


    @Override
    public String getConfigComponentName() {
        return RetrieveDiagnosticsServiceImpl.class.getSimpleName();
    }


    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { RetrieveDiagnosticsTimeOut, enabledGCollector, RetrieveDiagnosticsFilePath, RetrieveDiagnosticsDisableThreshold,
                RetrieveDiagnosticsFileAge, RetrieveDiagnosticsInterval };
    }

    public DiagnosticsKey<?>[] getDiagnosticsDataKeys() {
        return new DiagnosticsKey<?>[] { LOGFILES, PROPERTYFILES,
                CONFIGURATIONFILES, DHCPFILES, USERDATA, LB, DNS, VPN, IPTABLESretrieve, IPTABLESremove };
    }


    @Override
    public List<Class<?>> getCommands(){
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(RetrieveDiagnosticsCmd.class);
        return cmdList;
    }

/*    public SecondaryStorageVmVO startSecondaryStorageVm(long secStorageVmId) {
        return _ssVmMgr.startSecStorageVm(secStorageVmId);
    }

    public boolean generateSecStorageSetupCommand(Long ssHostId) {
        return _ssVmMgr.generateSetupCommand(ssHostId);
    }

    public boolean generateVMSetupCommand(Long ssAHostId) {
        return _ssVmMgr.generateVMSetupCommand(ssAHostId);
    }

    public void onAgentConnect(Long dcId, StartupCommand cmd) {
        onAgentConnect(dcId, cmd);
    }
*/


}
