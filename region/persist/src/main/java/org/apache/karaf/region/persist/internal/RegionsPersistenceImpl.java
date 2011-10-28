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

import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.karaf.region.persist.RegionsPersistence;
import org.apache.karaf.region.persist.internal.util.ManifestHeaderProcessor;
import org.apache.karaf.region.persist.model.FilterAttributeType;
import org.apache.karaf.region.persist.model.FilterBundleType;
import org.apache.karaf.region.persist.model.FilterNamespaceType;
import org.apache.karaf.region.persist.model.FilterPackageType;
import org.apache.karaf.region.persist.model.FilterType;
import org.apache.karaf.region.persist.model.RegionBundleType;
import org.apache.karaf.region.persist.model.RegionType;
import org.apache.karaf.region.persist.model.RegionsType;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @version $Rev:$ $Date:$
 */
public class RegionsPersistenceImpl implements RegionsPersistence {

    private JAXBContext jaxbContext;
    private BundleContext frameworkContext;
    private RegionDigraph regionDigraph;

    public RegionsPersistenceImpl(RegionDigraph regionDigraph) throws JAXBException {
        this.regionDigraph = regionDigraph;
        jaxbContext = JAXBContext.newInstance(RegionsType.class);
    }

    void save(RegionsType regionsType, Writer out) throws JAXBException {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(regionsType, out);
    }

    void  load(RegionDigraph regionDigraph, Reader in) throws JAXBException, BundleException, InvalidSyntaxException {
        RegionsType regionsType = load(in);
        load(regionsType, regionDigraph);
    }

    RegionsType load(Reader in) throws JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (RegionsType) unmarshaller.unmarshal(in);
    }

    void load(RegionsType regionsType, RegionDigraph regionDigraph) throws BundleException, InvalidSyntaxException {
        for (RegionType regionType: regionsType.getRegion()) {
            String name = regionType.getName();
            Region region = regionDigraph.createRegion(name);
            for (RegionBundleType bundleType: regionType.getBundle()) {
                if (bundleType.getId() != null) {
                    region.addBundle(bundleType.getId());
                } else {
                    Bundle b = frameworkContext.getBundle(bundleType.getLocation());
                    region.addBundle(b);
                }
            }
        }
        for (FilterType filterType: regionsType.getFilter()) {
            Region from = regionDigraph.getRegion(filterType.getFrom());
            Region to = regionDigraph.getRegion(filterType.getTo());
            RegionFilterBuilder builder = regionDigraph.createRegionFilterBuilder();
            for (FilterBundleType bundleType: filterType.getBundle()) {
                String symbolicName = bundleType.getSymbolicName();
                String version = bundleType.getVersion();
                if (bundleType.getId() != null) {
                    Bundle b = frameworkContext.getBundle(bundleType.getId());
                    symbolicName = b.getSymbolicName();
                    version = b.getVersion().toString();
                }
                String namespace = "osgi.wiring.bundle";
                List<FilterAttributeType> attributeTypes = bundleType.getAttribute();
                buildFilter(symbolicName, version, namespace, attributeTypes, builder);
            }
            for (FilterPackageType packageType: filterType.getPackage()) {
                String packageName = packageType.getName();
                String version = packageType.getVersion();
                String namespace = "osgi.wiring.package";
                List<FilterAttributeType> attributeTypes = packageType.getAttribute();
                buildFilter(packageName, version, namespace, attributeTypes, builder);
            }
            //TODO explicit services?
            for (FilterNamespaceType namespaceType: filterType.getNamespace()) {
                String namespace = namespaceType.getName();
                HashMap<String, String> attributes = new HashMap<String, String>();
                for (FilterAttributeType attributeType: namespaceType.getAttribute()) {
                    attributes.put(attributeType.getName(), attributeType.getValue());
                }
                String filter = ManifestHeaderProcessor.generateFilter(attributes);
                builder.allow(namespace, filter);
            }
            regionDigraph.connect(from, builder.build(), to);
        }
    }

    private void buildFilter(String packageName, String version, String namespace, List<FilterAttributeType> attributeTypes, RegionFilterBuilder builder) throws InvalidSyntaxException {
        HashMap<String, String> attributes = new HashMap<String, String>();
        if (namespace != null) {
            attributes.put(namespace, packageName);
        }
        if (version != null) {
            attributes.put("version", version);
        }
        for (FilterAttributeType attributeType: attributeTypes) {
            attributes.put(attributeType.getName(), attributeType.getValue());
        }
        String filter = ManifestHeaderProcessor.generateFilter(attributes);
        builder.allow(namespace, filter);
    }

}
