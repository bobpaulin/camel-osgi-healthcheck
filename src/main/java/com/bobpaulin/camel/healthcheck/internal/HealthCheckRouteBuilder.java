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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.etcd.EtcdConstants;
import org.apache.camel.model.dataformat.JsonLibrary;

import org.osgi.framework.BundleContext;

import com.bobpaulin.camel.healthcheck.HealthcheckConfig;
import com.bobpaulin.camel.healthcheck.HealthcheckService;

public class HealthCheckRouteBuilder extends RouteBuilder {
    
    private final BundleContext bundleContext;
    
    private final String updateInterval;
    
    private final String updateTTL;
    
    public HealthCheckRouteBuilder(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        HealthcheckConfig config = bundleContext.getService(bundleContext.getServiceReference(HealthcheckConfig.class));
        this.updateInterval = config.getUpdateInterval();
        this.updateTTL = config.getUpdateTTL();
    }

    @Override
    public void configure() throws Exception {
        
        from("timer://healthtimer?fixedRate=true&period="+ updateInterval)
        .setHeader(EtcdConstants.ETCD_ACTION, constant(EtcdConstants.ETCD_KEYS_ACTION_SET))
        .process(new Processor() {
            
            public void process(Exchange exchange) throws Exception {
                
                HealthcheckConfig healthConfig = 
                        exchange.getContext().getRegistry().lookupByNameAndType("healthConfig", HealthcheckConfig.class);
                
                HealthcheckService healthcheckService = 
                        exchange.getContext().getRegistry().lookupByNameAndType(healthConfig.getHealthcheckServiceName(), HealthcheckService.class);
                
                
                StringBuilder etcdPath = new StringBuilder("/app/health/");
                etcdPath.append(healthConfig.getAppName());
                etcdPath.append("/");
                etcdPath.append(healthConfig.getHostname());
                etcdPath.append("_");
                etcdPath.append(healthConfig.getPort());
                exchange.getIn().setHeader(EtcdConstants.ETCD_PATH, etcdPath.toString());
                
                exchange.getIn().setBody(healthcheckService.getCurrentStatus());
                
            }
        }).marshal().json(JsonLibrary.Jackson)
        .to("etcd:keys?timeToLive=" + this.updateTTL);

    }

}
