package com.c12e.fabric;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Types definition and utilities for parsing Cortex connections, CDATA license validation etc
 *
 * Known Implementation @see {@link CortexClient}
 */
public interface ConfigurationProvider {

    ObjectMapper mapper = new ObjectMapper();
    Properties env = new Properties(System.getProperties());

    String CONNECTION_NAME = "name";
    String CONNECTION_TYPE = "connectionType";
    String MANAGED_CONTENT_KEY = "managed_content_key";
    String DRIVE_CLASS_NAME = "classname";
    String PLUGIN_PROPERTIES = "plugin_properties";
    String JDBC_URI = "uri";
    String JDBC_USERNAME = "username";
    String JDBC_PASSWORD = "password";

    class JdbcConnectionParams {
        private String connectionProtocol;
        private JdbcConnectionType connectionType;
        private String connectionName;
        private Properties connectionProperties;
        private Map<String, Object> rawParams;
        private String driverClassname;
        private String lastUpdated;

        public JdbcConnectionParams(String connectionName, JdbcConnectionType connectionType, String connectionProtocol, Properties connectionProperties, String driverClassname, Map<String, Object> rawParams, String createdAt) {
            this.connectionName = connectionName;
            this.connectionType = connectionType;
            this.connectionProtocol = connectionProtocol;
            this.connectionProperties = connectionProperties;
            this.driverClassname = driverClassname;
            this.rawParams = rawParams;
            this.lastUpdated = createdAt;
        }

        public String getConnectionProtocol() {
            return connectionProtocol;
        }

        public JdbcConnectionType getConnectionType() {
            return connectionType;
        }

        public String getConnectionName() {
            return connectionName;
        }

        public Properties getConnectionProperties() {
            return connectionProperties;
        }

        public Map<String, Object> getRawParams() {
            return rawParams;
        }

        public String getDriverClassname() {
            return driverClassname;
        }

        public String getLicenseKey() {
            return connectionProperties.getProperty("OEMKey");
        }

        public String getLastUpdated() {
            return lastUpdated;
        }
    }

    enum JdbcConnectionType {
        JDBC_GENERIC, JDBC_CDATA;
    }

    default void loadEnv() {
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        try {
            env.load(ConfigurationProvider.class.getResourceAsStream("/cortex-fabric-default.properties"));
            InputStream propsOverride = ConfigurationProvider.class.getResourceAsStream("/cortex-fabric.properties");
            if (propsOverride != null) env.load(propsOverride);
        } catch (IOException e) {
            e.printStackTrace();
        }
        env.putAll(System.getenv());
    }

    default JdbcConnectionParams parseCortexConnectionDefinition(String connectionDef) throws IOException {
        // parse params
        Map<String, Object> connection = mapper.readValue(connectionDef, Map.class);
        String connectionName = (String) connection.get(CONNECTION_NAME);
        JdbcConnectionType connectionType = JdbcConnectionType.valueOf(connection.get(CONNECTION_TYPE).toString().toUpperCase());
        Map<String, Object> params = ((List<Map<String, Object>>) connection.get("params"))
                .stream().collect(Collectors.toMap(kv -> kv.get("name").toString(), kv -> kv.get("value")));

        if (params.get(PLUGIN_PROPERTIES) instanceof String) {
            String pluginProperties = (String) params.get(PLUGIN_PROPERTIES);

            if (!(Objects.isNull(pluginProperties) || pluginProperties.trim().isEmpty())) {
                try {
                    params.put(PLUGIN_PROPERTIES, mapper.readValue(pluginProperties, Map.class));
                } catch (JsonParseException | JsonMappingException e) {
                    throw new RuntimeException("CDATA plugin_properties must be JSON formatted string", e);
                }
            } else if (connectionType == JdbcConnectionType.JDBC_CDATA) {
                throw new RuntimeException("plugin_properties is required for CDATA connections");
            }
        }

        String driverClassName = (String) params.get(DRIVE_CLASS_NAME);

        // build connection params
        String protocol = (String) params.get(JDBC_URI);
        Properties connectionProperties = new Properties();
        if (connectionType == JdbcConnectionType.JDBC_CDATA) {
            String[] classParts = driverClassName.split("\\.");
            protocol = classParts[1] + ":" + classParts[2] + ":";
            connectionProperties.putAll((Map<String, String>) params.get(PLUGIN_PROPERTIES));
            connectionProperties.setProperty("OEMKey", getLicenseKey());
        } else {
            connectionProperties.putAll(params);
        }

        return new JdbcConnectionParams(connectionName, connectionType, protocol, connectionProperties, driverClassName, params, (String) connection.get("createdAt"));
    }

    JdbcConnectionParams getConnectionParams(String connectionName) throws IOException;

    String getLicenseKey() throws IOException;
}