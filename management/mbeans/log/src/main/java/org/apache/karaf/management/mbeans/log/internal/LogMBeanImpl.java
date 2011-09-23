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
package org.apache.karaf.management.mbeans.log.internal;

import org.apache.karaf.management.mbeans.log.LogMBean;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.io.IOException;
import java.util.*;

/**
 * Implementation of the Log MBean.
 */
public class LogMBeanImpl extends StandardMBean implements LogMBean {

    static final String CONFIGURATION_PID = "org.ops4j.pax.logging";
    static final String ROOT_LOGGER_PREFIX = "log4j.rootLogger";
    static final String LOGGER_PREFIX = "log4j.logger.";
    static final String ROOT_LOGGER = "ROOT";

    private LruList events;
    private BundleContext bundleContext;

    public LogMBeanImpl() throws NotCompliantMBeanException {
        super(LogMBean.class);
    }

    public List<String> display() throws Exception {
        return display(null);
    }

    public List<String> display(String logger) throws Exception {
        Iterable<PaxLoggingEvent> le = events.getElements(Integer.MAX_VALUE);
        List<String> out = new ArrayList<String>();
        for (PaxLoggingEvent event : le) {
            if ((logger != null) && (event != null)
                    && (checkIfFromRequestedLog(event, logger))) {
                out.add(render(event));
            } else if ((event != null) && (logger == null)) {
                out.add(render(event));
            }
        }
        return out;
    }

    public void set(String level) throws Exception {
        set(level, null);
    }

    public void set(String level, String logger) throws Exception {
        if (ROOT_LOGGER.equalsIgnoreCase(logger)) {
            logger = null;
        }

        // make sure both uppercase and lowercase levels are supported
        level = level.toUpperCase();

        if (!level.equals("TRACE") && !level.equals("DEBUG") && !level.equals("INFO")
                && !level.equals("WARN") && !level.equals("ERROR") && !level.equals("DEFAULT")) {
            throw new IllegalArgumentException("level must be set to TRACE, DEBUG, INFO, WARN or ERROR (or DEFAULT to unset it)");
        }

        Configuration cfg = getConfiguration();
        Dictionary props = cfg.getProperties();

        String val;
        String prop;
        if (logger == null) {
            prop = ROOT_LOGGER_PREFIX;
        } else {
            prop = LOGGER_PREFIX + logger;
        }
        val = (String) props.get(prop);
        if (level.equals("DEFAULT")) {
            if (val != null) {
                val = val.trim();
                int idx = val.indexOf(",");
                if (idx < 0) {
                    val = null;
                } else {
                    val = val.substring(idx);
                }
            }
        } else {
            if (val == null) {
                val = level;
            } else {
                val = val.trim();
                int idx = val.indexOf(",");
                if (idx < 0) {
                    val = level;
                } else {
                    val = level + val.substring(idx);
                }
            }
        }
        if (val == null) {
            props.remove(prop);
        } else {
            props.put(prop, val);
        }
        cfg.update(props);
    }

    public String get() throws Exception {
        return get(null);
    }

    public String get(String logger) throws Exception {
        ConfigurationAdmin cfgAdmin = getConfigAdmin();
        Configuration cfg = cfgAdmin.getConfiguration(CONFIGURATION_PID, null);
        Dictionary props = cfg.getProperties();

        if (ROOT_LOGGER.equalsIgnoreCase(logger)) {
            logger = null;
        }

        String val;
        for (; ; ) {
            String prop;
            if (logger == null) {
                prop = ROOT_LOGGER_PREFIX;
            } else {
                prop = LOGGER_PREFIX + logger;
            }
            val = (String) props.get(prop);
            val = getLevel(val);
            if (val != null || logger == null) {
                break;
            }
            int idx = logger.lastIndexOf('.');
            if (idx < 0) {
                logger = null;
            } else {
                logger = logger.substring(0, idx);
            }
        }
        String st = "Level: " + val;
        return st;
    }

    private boolean checkIfFromRequestedLog(PaxLoggingEvent event, String logger) {
        return (event.getLoggerName().lastIndexOf(logger) >= 0) ? true : false;
    }

    private String render(PaxLoggingEvent event) {
        StringBuffer sb = new StringBuffer();
        sb.setLength(0);
        if (event.getThrowableStrRep() != null) {
            for (String r : event.getThrowableStrRep()) {
                sb.append(r).append('\n');
            }
        }
        return sb.toString();
    }

    private String getLevel(String prop) {
        if (prop == null) {
            return null;
        } else {
            String val = prop.trim();
            int idx = val.indexOf(",");
            if (idx == 0) {
                val = null;
            } else if (idx > 0) {
                val = val.substring(0, idx);
            }
            return val;
        }
    }

    public LruList getEvents() {
        return this.events;
    }

    public void setEvents(LruList events) {
        this.events = events;
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    protected Configuration getConfiguration() throws IOException {
        Configuration cfg = getConfigAdmin().getConfiguration(CONFIGURATION_PID, null);
        return cfg;
    }

    protected ConfigurationAdmin getConfigAdmin() {
        ServiceReference ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        if (ref != null) {
            return (ConfigurationAdmin) bundleContext.getService(ref);
        }
        return null;
    }

}
