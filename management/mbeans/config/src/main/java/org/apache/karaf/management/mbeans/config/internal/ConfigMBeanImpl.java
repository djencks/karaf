/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.management.mbeans.config.internal;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.management.mbeans.config.ConfigMBean;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.io.File;
import java.util.*;

/**
 * Implementation of the ConfigMBean.
 */
public class ConfigMBeanImpl extends StandardMBean implements ConfigMBean {

    private final String FELIX_FILEINSTALL_FILENAME = "felix.fileinstall.filename";

    private ConfigurationAdmin configurationAdmin;
    private File storage;

    public ConfigurationAdmin getConfigurationAdmin() {
        return this.configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public File getStorage() {
        return this.storage;
    }

    public void setStorage(File storage) {
        this.storage = storage;
    }

    public ConfigMBeanImpl() throws NotCompliantMBeanException {
        super(ConfigMBean.class);
    }

    public List<String> list() throws Exception {
        Configuration[] configurations = configurationAdmin.listConfigurations(null);
        List<String> pids = new ArrayList<String>();
        for (int i = 0; i < configurations.length; i++) {
            pids.add(configurations[i].getPid());
        }
        return pids;
    }

    public void create(String pid) throws Exception {
        store(pid, new Hashtable(), false);
    }

    public void delete(String pid) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration(pid);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        configuration.delete();
        if (storage != null) {
            File cfgFile = new File(storage, pid + ".cfg");
            cfgFile.delete();
        }
    }

    public Map<String, String> proplist(String pid) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration(pid);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        Dictionary dictionary = configuration.getProperties();
        Map<String, String> propertiesMap = new HashMap<String, String>();
        for (Enumeration e = dictionary.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            Object value = dictionary.get(key);
            propertiesMap.put(key.toString(), value.toString());
        }
        return propertiesMap;
    }

    public void propdel(String pid, String key) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration(pid);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        Dictionary dictionary = configuration.getProperties();
        dictionary.remove(key);
        store(pid, dictionary, false);
    }

    public void propappend(String pid, String key, String value) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration(pid);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        Dictionary dictionary = configuration.getProperties();
        Object currentValue = dictionary.get(key);
        if (currentValue == null) {
            dictionary.put(key, value);
        } else if (currentValue instanceof String) {
            dictionary.put(key, currentValue + value);
        } else {
            throw new IllegalStateException("Current value is not a String");
        }
        store(pid, dictionary, false);
    }

    public void propset(String pid, String key, String value) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration(pid);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        Dictionary dictionary = configuration.getProperties();
        dictionary.put(key, value);
        store(pid, dictionary, false);
    }

    /**
     * Store/flush a configuration PID into the configuration file.
     *
     * @param pid        the configuration PID.
     * @param properties the configuration properties.
     * @throws Exception
     */
    private void store(String pid, Dictionary properties, boolean bypassStorage) throws Exception {
        if (!bypassStorage && storage != null) {
            File storageFile = new File(storage, pid + ".cfg");
            Configuration configuration = configurationAdmin.getConfiguration(pid, null);
            if (configuration != null && configuration.getProperties() != null) {
                Object val = configuration.getProperties().get(FELIX_FILEINSTALL_FILENAME);
                if (val instanceof String) {
                    if (((String) val).startsWith("file:")) {
                        val = ((String) val).substring("file:".length());
                    }
                    storageFile = new File((String) val);
                }
            }
            Properties p = new Properties(storageFile);
            p.clear();
            for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
                Object key = keys.nextElement();
                if (!Constants.SERVICE_PID.equals(key)
                        && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
                        && !FELIX_FILEINSTALL_FILENAME.equals(key)) {
                    p.put((String) key, (String) properties.get(key));
                }
            }
            storage.mkdirs();
            p.save();
        } else {
            Configuration cfg = configurationAdmin.getConfiguration(pid, null);
            if (cfg.getProperties() == null) {
                String[] pids = parsePid(pid);
                if (pids[1] != null) {
                    cfg = configurationAdmin.createFactoryConfiguration(pids[0], null);
                }
            }
            if (cfg.getBundleLocation() != null) {
                cfg.setBundleLocation(null);
            }
            cfg.update(properties);
        }
    }

    private String[] parsePid(String pid) {
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[] { pid, factoryPid };
        } else {
            return new String[] { pid, null };
        }
    }

}
