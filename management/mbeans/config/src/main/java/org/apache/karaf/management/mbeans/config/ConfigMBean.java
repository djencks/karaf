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
package org.apache.karaf.management.mbeans.config;

import java.util.List;
import java.util.Map;

/**
 * MBean to manipulate the Config layer.
 */
public interface ConfigMBean {

    /**
     * Get the list of all configuration PIDs.
     *
     * @return the list of all configuration PIDs.
     * @throws Exception
     */
    List<String> list() throws Exception;

    /**
     * Create a new configuration for the given PID.
     *
     * @param pid the configuration PID.
     * @throws Exception
     */
    void create(String pid) throws Exception;

    /**
     * Delete a configuration identified by the given PID.
     *
     * @param pid the configuration PID to delete.
     * @throws Exception
     */
    void delete(String pid) throws Exception;

    /**
     * Get the list of properties for a configuration PID.
     *
     * @param pid the configuration PID.
     * @return the list of properties.
     * @throws Exception
     */
    Map<String, String> proplist(String pid) throws Exception;

    /**
     * Remove the configuration property identified by the given key.
     *
     * @param pid the configuration PID.
     * @param key the property key.
     * @param bypassStorage doesn't flush the change on the storage config file.
     * @throws Exception
     */
    void propdel(String pid, String key, boolean bypassStorage) throws Exception;

    /**
     * Append (or add) a value for the given configuration key.
     *
     * @param pid the configuration PID.
     * @param key the property key.
     * @param value the value to append to the current property value.
     * @param bypassStorage doesn't flush the change on the storage config file.
     * @throws Exception
     */
    void propappend(String pid, String key, String value, boolean bypassStorage) throws Exception;

    /**
     * Set a configuration property.
     *
     * @param pid the configuration PID.
     * @param key the property key.
     * @param value the property value.
     * @param bypassStorage doesn't flush the change on the storage config file.
     * @throws Exception
     */
    void propset(String pid, String key, String value, boolean bypassStorage) throws Exception;

}
