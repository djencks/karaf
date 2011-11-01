/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.karaf.region.persist.internal;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.bind.JAXBException;
import org.apache.karaf.region.persist.RegionsPersistence;
import org.apache.karaf.region.persist.internal.util.SingleServiceTracker;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev:$ $Date:$
 */
public class Activator implements BundleActivator {

    private static final Logger log = LoggerFactory.getLogger(Activator.class);

    private SingleServiceTracker<RegionDigraph> tracker;
    private final AtomicReference<ServiceReference<RegionDigraph>> ref = new AtomicReference<ServiceReference<RegionDigraph>>();
    private final AtomicReference<RegionsPersistenceImpl> persistence = new AtomicReference<RegionsPersistenceImpl>();

    @Override
    public void start(BundleContext bundleContext) throws Exception {
         tracker = new SingleServiceTracker<RegionDigraph>(bundleContext, RegionDigraph.class, new SingleServiceTracker.SingleServiceListener() {
          public void serviceFound() {
            log.debug("Found RegionDigraph service, initializing");
              RegionDigraph regionDigraph = tracker.getService();
              RegionsPersistenceImpl persistence = null;
              try {
                  persistence = new RegionsPersistenceImpl(regionDigraph);
              } catch (Exception e) {
                  log.info("Could not create RegionsPersistenceImpl", e);
              }
              Activator.this.persistence.set(persistence);
          }
          public void serviceLost() {
              Activator.this.persistence.set(null);
          }
          public void serviceReplaced() {
              //??
          }
        });
        tracker.open();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        tracker.close();
        persistence.set(null);
    }


}
