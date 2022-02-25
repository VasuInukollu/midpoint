/*
 * Copyright (c) 2016-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest.authentication;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.testing.rest.RestServiceInitializer;

import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.AfterMethod;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;

public abstract class TestAbstractAuthentication extends RestServiceInitializer {

    protected static final File BASE_AUTHENTICATION_DIR = new File("src/test/resources/authentication/");
    protected static final File BASE_REPO_DIR = new File(BASE_AUTHENTICATION_DIR,"repo/");

    @Autowired
    protected MidpointJsonProvider jsonProvider;

    @Autowired
    private SystemObjectCache systemObjectCache;

    @Override
    protected String getAcceptHeader() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    protected String getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    protected MidpointAbstractProvider getProvider() {
        return jsonProvider;
    }

    @AfterMethod
    public void clearCache(){
        systemObjectCache.invalidateCaches();
    }

    protected void replaceSecurityPolicy(File securityPolicy) throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObject<SecurityPolicyType> secPolicy = parseObject(securityPolicy);
        addObject(secPolicy, executeOptions().overwrite(), task, result);
        getDummyAuditService().clear();
    }
}