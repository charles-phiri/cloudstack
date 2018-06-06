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
package org.apache.cloudstack.framework.config.impl;

import org.apache.cloudstack.framework.config.DiagnosticsConfigDepot;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;

public class DiagnosticsConfigDepotImpl implements DiagnosticsConfigDepot {

    private final static Logger s_logger = Logger.getLogger(DiagnosticsConfigDepotImpl.class);
    @Inject
    RetrieveDiagnosticsDao _diagnosticsDao;

    @Override
    public void setDiagnosticsKeyHashMap(HashMap<String, DiagnosticsKey> diagnosticsKeyHashMap) {
        this.diagnosticsKeyHashMap = diagnosticsKeyHashMap;
    }

    HashMap<String, DiagnosticsKey> diagnosticsKeyHashMap = null;

    public DiagnosticsConfigDepotImpl() {
    }

    @Override
    public void populateDiagnostics(DiagnosticsKey clazz) {
        boolean diagnosticsTypeExists = false;
        DiagnosticsKey diagnosticsKey = diagnosticsKeyHashMap.get(clazz.getDiagnosticsClassType());
        if (diagnosticsKey != null) {
            if (diagnosticsKey.getRole().equalsIgnoreCase(clazz.getRole()) && diagnosticsKey.getDiagnosticsClassType().equalsIgnoreCase(clazz.getDiagnosticsClassType())) {
                if (!diagnosticsKey.getDetail().equalsIgnoreCase(clazz.getDetail())) {
                    diagnosticsKey.setDetail(clazz.getDetail());
                    diagnosticsTypeExists = true;
                }
            }
        }
        if (!diagnosticsTypeExists) {
            DiagnosticsKey newDiagnosticsType = new DiagnosticsKey(clazz.getRole(), clazz.getDiagnosticsClassType(), clazz.getDetail(), clazz.description());
            createOrUpdateDiagnosticObject(newDiagnosticsType );
        }
    }

    @Override
    public void createOrUpdateDiagnosticObject(DiagnosticsKey diagnosticsType) {
        List<RetrieveDiagnosticsVO> voList = _diagnosticsDao.findByEntity(diagnosticsType.getDiagnosticsClassType(), diagnosticsType.getRole());
        for (RetrieveDiagnosticsVO vo : voList) {
            if (vo == null) {
                vo = new RetrieveDiagnosticsVO(diagnosticsType.getRole(), diagnosticsType.getDiagnosticsClassType(), diagnosticsType.getDetail());
                vo.setType(diagnosticsType.getRole());
                vo.setRole(diagnosticsType.getDiagnosticsClassType());//to be given SystemVM type
                vo.setDefaultValue(diagnosticsType.getDetail());//to be populated
                _diagnosticsDao.persist(vo);

            } else {
                if (vo.getDefaultValue() != diagnosticsType.getDiagnosticsClassType() || !ObjectUtils.equals(vo.getRole(), diagnosticsType.getRole()) || !ObjectUtils.equals(vo.getDefaultValue(),
                        diagnosticsType.getDetail())) {
                    vo.setRole(diagnosticsType.value()); //to be changed
                    vo.setType(diagnosticsType.key());
                    vo.setDefaultValue(diagnosticsType.getDetail()); //to be changed
                    _diagnosticsDao.persist(vo);
                }
            }
        }
    }

    @Override
    public HashMap<String, DiagnosticsKey> getDiagnosticsTypeLevelsMap() {
        return diagnosticsKeyHashMap;
    }

    public void setDiagnosticsTypeLevelsMap(HashMap<String, DiagnosticsKey> diagnosticsTypeLevelsMap) {
        this.diagnosticsKeyHashMap = diagnosticsTypeLevelsMap;
    }

    public RetrieveDiagnosticsDao global() {
        return _diagnosticsDao;
    }

    @Override
    public void set(DiagnosticsKey key, String value) {

    }

}
