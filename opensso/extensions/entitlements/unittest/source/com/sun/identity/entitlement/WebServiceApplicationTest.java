/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: WebServiceApplicationTest.java,v 1.2 2009/08/11 23:11:37 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class WebServiceApplicationTest {
    private static final String WSDL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!-- Published by JAX-WS RI at http://jax-ws.dev.java.net. RI's version is JAX-WS RI 2.1.3.1-hudson-581-SNAPSHOT. --><!-- Generated by JAX-WS RI at http://jax-ws.dev.java.net. RI's version is JAX-WS RI 2.1.3.1-hudson-581-SNAPSHOT. --><definitions xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:tns=\"http://services.geotracking.us/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://services.geotracking.us/\" name=\"UsaGeoTrackingService\"> <types> <xsd:schema> <xsd:import namespace=\"http://services.geotracking.us/\" schemaLocation=\"http://192.18.47.152:8080/UsaGeoTrackingService/UsaGeoTracking?xsd=1\"></xsd:import> </xsd:schema> </types> <message name=\"findAll\"> <part name=\"parameters\" element=\"tns:findAll\"></part> </message> <message name=\"findAllResponse\"> <part name=\"parameters\" element=\"tns:findAllResponse\"></part> </message> <message name=\"getTrackingById\"> <part name=\"parameters\" element=\"tns:getTrackingById\"></part> </message> <message name=\"getTrackingByIdResponse\"> <part name=\"parameters\" element=\"tns:getTrackingByIdResponse\"></part> </message> <portType name=\"UsaGeoTracking\"> <operation name=\"findAll\"> <input message=\"tns:findAll\"></input> <output message=\"tns:findAllResponse\"></output> </operation> <operation name=\"getTrackingById\"> <input message=\"tns:getTrackingById\"></input> <output message=\"tns:getTrackingByIdResponse\"></output> </operation> </portType> <binding name=\"UsaGeoTrackingPortBinding\" type=\"tns:UsaGeoTracking\"> <soap:binding transport=\"http://schemas.xmlsoap.org/soap/http\" style=\"document\"></soap:binding> <operation name=\"findAll\"> <soap:operation soapAction=\"\"></soap:operation> <input> <soap:body use=\"literal\"></soap:body> </input> <output> <soap:body use=\"literal\"></soap:body> </output> </operation> <operation name=\"getTrackingById\"> <soap:operation soapAction=\"\"></soap:operation> <input> <soap:body use=\"literal\"></soap:body> </input> <output> <soap:body use=\"literal\"></soap:body> </output> </operation> </binding> <service name=\"UsaGeoTrackingService\"> <port name=\"UsaGeoTrackingPort\" binding=\"tns:UsaGeoTrackingPortBinding\"> <soap:address location=\"http://192.18.47.152:8080/UsaGeoTrackingService/UsaGeoTracking\"></soap:address> </port> </service> </definitions>";
    private static final String APPL_NAME = "WebServiceApplicationTestApp";
    private static final String POLICY_NAME =
        "WebServiceApplicationTestPolicy";
    private static final String URL = "http://192.18.47.152:8080/UsaGeoTrackingService/UsaGeoTracking/";
    private Subject adminSubject;

    @BeforeClass
    public void setup() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        adminSubject = SubjectUtils.createSubject(adminToken);
        Application appl = ApplicationManager.newApplication("/", APPL_NAME,
            ApplicationTypeManager.getAppplicationType(adminSubject,
            ApplicationTypeManager.WEB_SERVICE_APPLICATION_TYPE_NAME));
        WebServiceApplication wsAppl = (WebServiceApplication)appl;

        InputStream is = null;
        try {
            is = new ByteArrayInputStream(WSDL.getBytes("UTF-8"));
            wsAppl.initialize(is);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                //ignore
            }
        }
        
        wsAppl.setEntitlementCombiner(DenyOverride.class);
        ApplicationManager.saveApplication(adminSubject, "/", appl);

        PrivilegeManager pm = PrivilegeManager.getInstance("/", adminSubject);
        Privilege privilege = Privilege.getNewInstance();
        privilege.setName(POLICY_NAME);

        Map<String, Boolean> actions = new HashMap<String, Boolean>();
        actions.put("findAll", true);
        Entitlement entitlement = new Entitlement(APPL_NAME, URL + "index.html",
            actions);
        privilege.setEntitlement(entitlement);
        privilege.setSubject(new AuthenticatedESubject());
        pm.addPrivilege(privilege);

        Privilege p = pm.getPrivilege(POLICY_NAME);
    }

    @AfterClass
    public void cleanup() throws EntitlementException {
        ApplicationManager.deleteApplication(adminSubject, "/", APPL_NAME);
        PrivilegeManager pm = PrivilegeManager.getInstance("/", adminSubject);
        pm.removePrivilege(POLICY_NAME);
    }

    @Test
    public void testEvaluation()
        throws Exception {
        Set actions = new HashSet();
        actions.add("findAll");
        Evaluator evaluator = new Evaluator(adminSubject, APPL_NAME);
        boolean allow = evaluator.hasEntitlement("/", adminSubject,
            new Entitlement(URL + "index.html", actions),
            Collections.EMPTY_MAP);
        if (!allow) {
            throw new Exception(
                "WebServiceApplicationTest.testEvaluation: incorrect policy decision.");
        }
    }

    @Test
    public void testApplication() throws Exception {
        Application appl = ApplicationManager.getApplication(
            adminSubject, "/", APPL_NAME);
        if (!appl.getClass().equals(WebServiceApplication.class)) {
            throw new Exception(
                "WebServiceApplicationTest.testApplication: incorrect class");
        }
        Map<String, Boolean> actions = appl.getActions();
        if (actions.size() != 2) {
            throw new Exception(
                "WebServiceApplicationTest.testApplication: incorrect actions");
        }

        Boolean bfindAll = actions.get("findAll");
        if ((bfindAll == null) || !bfindAll) {
            throw new Exception(
                "WebServiceApplicationTest.testApplication: expect findAll action");
        }
        Boolean bgetTrackingById = actions.get("getTrackingById");
        if ((bgetTrackingById == null) || !bgetTrackingById) {
            throw new Exception(
                "WebServiceApplicationTest.testApplication: expect getTrackingById action");
        }

        Set<String> resources = appl.getResources();
        if ((resources == null) || (resources.size() != 1)) {
            throw new Exception(
                "WebServiceApplicationTest.testApplication: incorrect resources");
        }

        String res = resources.iterator().next();
        if (!res.equals(URL + "*")) {
            throw new Exception(
                "WebServiceApplicationTest.testApplication: incorrect resources");
        }
    }
}
