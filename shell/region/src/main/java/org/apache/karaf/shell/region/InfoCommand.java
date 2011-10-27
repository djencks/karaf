/*
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
package org.apache.karaf.shell.region;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionDigraphPersistence;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@Command(scope = "region", name = "info", description = "Prints information about region digraph.")
public class InfoCommand extends RegionCommandSupport {

    protected void doExecute(RegionDigraph regionDigraph) throws Exception {
        System.out.println("Regions");
        for (Region region: regionDigraph.getRegions()) {
            System.out.println(region.getName());
            for (Long id: region.getBundleIds()) {
                Bundle b = getBundleContext().getBundle(id);
                System.out.println("  " + id + "  " + getStateString(b) + b);
            }
            for (RegionDigraph.FilteredRegion f: region.getEdges()) {
                System.out.println("  filter to " + f.getRegion().getName());
                RegionFilter rf = f.getFilter();
                for (Map.Entry<String, Collection<String>> policy: rf.getSharingPolicy().entrySet()) {
                    String namespace = policy.getKey();
                    System.out.println("  namespace: " + namespace);
                    for (String e: policy.getValue()) {
                        System.out.println("    " + e);
                    }
                }
            }
        }
    }

    public String getStateString(Bundle bundle)
    {
        int state = bundle.getState();
        if (state == Bundle.ACTIVE) {
            return "Active     ";
        } else if (state == Bundle.INSTALLED) {
            return "Installed  ";
        } else if (state == Bundle.RESOLVED) {
            return "Resolved   ";
        } else if (state == Bundle.STARTING) {
            return "Starting   ";
        } else if (state == Bundle.STOPPING) {
            return "Stopping   ";
        } else {
            return "Unknown    ";
        }
    }

}
