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
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.bobpaulin.camel.healthcheck.HealthStatus;
import com.bobpaulin.camel.healthcheck.HealthcheckService;

public class BundleStatusServiceImpl implements HealthcheckService {
    
    private final BundleContext context;
    
    public BundleStatusServiceImpl(BundleContext context)
    {
        this.context = context;
    }

    public HealthStatus getCurrentStatus() {
        HealthStatus result = new HealthStatus();
        
        boolean allBundlesActive = true;
        
        Bundle[] bundles = this.context.getBundles();
        
        for(Bundle currentBundle : bundles)
        {
            int bundleState = currentBundle.getState();
            
            Dictionary<String, ?> bundleHeaders = currentBundle.getHeaders();
            
            if(bundleState != Bundle.ACTIVE && bundleHeaders.get("Fragment-Host") == null)
            {
                List<String> inactiveBundlesList = 
                        (List<String>) result.getMetadata().get("inactiveBundles");
                
                if(inactiveBundlesList == null)
                {
                    inactiveBundlesList = new ArrayList<String>();
                    result.getMetadata().put("inactiveBundles", inactiveBundlesList);
                }
                
                inactiveBundlesList.add(currentBundle.getSymbolicName());
                allBundlesActive = false;
            }
        }
        
        result.setActive(allBundlesActive);
        
        return result;
    }

}
