/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bobpaulin.camel.healthcheck.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.camel.CamelContext;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

import com.bobpaulin.camel.healthcheck.HealthcheckConfig;
import com.bobpaulin.camel.healthcheck.HealthcheckService;

public class Activator implements BundleActivator {
    
    private ServiceRegistration<CamelContext> camelContextReg;
    
    private ServiceRegistration healthConfigReg;
    
    private ServiceRegistration<HealthcheckService> defaultHealthcheckServiceReg;
    
    private CamelContext camelContext;

    public void start(BundleContext context) throws Exception {
        
        String[] healthConfigServiceClasses = {HealthcheckConfig.class.getName(), ManagedService.class.getName()};
        Dictionary<String, Object> healthConfigServiceProps = new Hashtable<String, Object>();
        healthConfigServiceProps.put(Constants.SERVICE_PID, HealthcheckConfig.class.getName());
        healthConfigServiceProps.put("name", "healthConfig");
        this.healthConfigReg = context.registerService(healthConfigServiceClasses, new HealthcheckConfigImpl(), healthConfigServiceProps);
        
        Dictionary<String, Object> healthcheckServiceConfigProps = new Hashtable<String, Object>();
        healthcheckServiceConfigProps.put(Constants.SERVICE_PID, HealthcheckService.class.getName());
        healthcheckServiceConfigProps.put("name", "defaultHealthCheck");
        this.defaultHealthcheckServiceReg = 
                context.registerService(HealthcheckService.class, new BundleStatusServiceImpl(context), healthcheckServiceConfigProps);
        
        this.camelContext = new OsgiDefaultCamelContext(context);
        
        camelContext.addRoutes(new HealthCheckRouteBuilder(context));
        
        camelContext.start();

        camelContextReg = context.registerService(CamelContext.class, camelContext, null);

    }

    public void stop(BundleContext context) throws Exception {
        camelContext.stop();
        camelContext.removeRouteDefinitions(new ArrayList<RouteDefinition>(this.camelContext.getRouteDefinitions()));

        context.ungetService(camelContextReg.getReference());
        context.ungetService(this.healthConfigReg.getReference());
        context.ungetService(this.defaultHealthcheckServiceReg.getReference());

    }

}
