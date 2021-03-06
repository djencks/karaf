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
package org.apache.karaf.features.internal;

import static org.easymock.EasyMock.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.felix.utils.manifest.Clause;
import org.apache.karaf.features.Feature;
import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkListener;

/**
 * Test cases for {@link FeaturesServiceImpl}
 */
public class FeaturesServiceImplTest extends TestCase {
    
    File dataFile;

    protected void setUp() throws IOException {
        dataFile = File.createTempFile("features", null, null);
    }

    public void testGetFeature() throws Exception {
        final Map<String, Map<String, Feature>> features = new HashMap<String, Map<String,Feature>>();
        Map<String, Feature> versions = new HashMap<String, Feature>();
        FeatureImpl feature = new FeatureImpl("transaction");
        versions.put("1.0.0", feature);
        features.put("transaction", versions);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl() {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };
        };
        assertNotNull(impl.getFeature("transaction", FeatureImpl.DEFAULT_VERSION));
        assertSame(feature, impl.getFeature("transaction", FeatureImpl.DEFAULT_VERSION));
    }
    
    public void testGetFeatureStripVersion() throws Exception {
        final Map<String, Map<String, Feature>> features = new HashMap<String, Map<String,Feature>>();
        Map<String, Feature> versions = new HashMap<String, Feature>();
        FeatureImpl feature = new FeatureImpl("transaction");
        versions.put("1.0.0", feature);
        features.put("transaction", versions);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl() {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };
        };
        assertNotNull(impl.getFeature("transaction", "  1.0.0  "));
        assertSame(feature, impl.getFeature("transaction", "  1.0.0   "));
    }
    
    public void testGetFeatureNotAvailable() throws Exception {
        final Map<String, Map<String, Feature>> features = new HashMap<String, Map<String,Feature>>();
        Map<String, Feature> versions = new HashMap<String, Feature>();
        versions.put("1.0.0", new FeatureImpl("transaction"));
        features.put("transaction", versions);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl() {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };
        };
        assertNull(impl.getFeature("activemq", FeatureImpl.DEFAULT_VERSION));
    }
    
    public void testGetFeatureHighestAvailable() throws Exception {
        final Map<String, Map<String, Feature>> features = new HashMap<String, Map<String,Feature>>();
        Map<String, Feature> versions = new HashMap<String, Feature>();
        versions.put("1.0.0", new FeatureImpl("transaction", "1.0.0"));
        versions.put("2.0.0", new FeatureImpl("transaction", "2.0.0"));
        features.put("transaction", versions);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl() {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };
        };
        assertNotNull(impl.getFeature("transaction", FeatureImpl.DEFAULT_VERSION));
        assertSame("2.0.0", impl.getFeature("transaction", FeatureImpl.DEFAULT_VERSION).getVersion());
    }

    public void testStartDoesNotFailWithOneInvalidUri()  {
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        bundleContext.addFrameworkListener(EasyMock.<FrameworkListener>anyObject());
        bundleContext.removeFrameworkListener(EasyMock.<FrameworkListener>anyObject());
        replay(bundleContext);
        FeaturesServiceImpl service = new FeaturesServiceImpl();
        service.setBundleContext(bundleContext);
        try {
            service.setUrls("mvn:inexistent/features/1.0/xml/features");
            service.start();
        } catch (Exception e) {
            fail(String.format("Service should not throw start-up exception but log the error instead: %s", e));
        }
    }

    /**
     * This test checks KARAF-388 which allows you to specify version of boot feature.
     */
    public void testStartDoesNotFailWithNonExistentVersion()  {
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);

        final Map<String, Map<String, Feature>> features = new HashMap<String, Map<String,Feature>>();
        Map<String, Feature> versions = new HashMap<String, Feature>();
        versions.put("1.0.0", new FeatureImpl("transaction", "1.0.0"));
        versions.put("2.0.0", new FeatureImpl("transaction", "2.0.0"));
        features.put("transaction", versions);

        Map<String, Feature> versions2 = new HashMap<String, Feature>();
        versions2.put("1.0.0", new FeatureImpl("ssh", "1.0.0"));
        features.put("ssh", versions2);

        final FeaturesServiceImpl impl = new FeaturesServiceImpl() {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };

            // override methods which refers to bundle context to avoid mocking everything
            @Override
            protected boolean loadState() {
                return true;
            }
            @Override
            protected void saveState() {
            }
        };
        impl.setBundleContext(bundleContext);

        try {
            Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[0]));
            impl.setBoot("transaction;version=1.2,ssh;version=1.0.0");
            impl.start();

            assertFalse("Feature transaction 1.0.0 should not be installed", impl.isInstalled(impl.getFeature("transaction", "1.0.0")));
            assertFalse("Feature transaction 2.0.0 should not be installed", impl.isInstalled(impl.getFeature("transaction", "2.0.0")));
            assertFalse("Feature ssh should be installed", impl.isInstalled(impl.getFeature("ssh", "1.0.0")));
        } catch (Exception e) {
            fail(String.format("Service should not throw start-up exception but log the error instead: %s", e));
        }
    }

    public void testGetOptionalImportsOnly() {
        FeaturesServiceImpl service = new FeaturesServiceImpl();

        List<Clause> result = service.getOptionalImports("org.apache.karaf,org.apache.karaf.optional;resolution:=optional");
        assertEquals("One optional import expected", 1, result.size());
        assertEquals("org.apache.karaf.optional", result.get(0).getName());

        result = service.getOptionalImports(null);
        assertNotNull(result);
        assertEquals("No optional imports expected", 0, result.size());
    }
}
