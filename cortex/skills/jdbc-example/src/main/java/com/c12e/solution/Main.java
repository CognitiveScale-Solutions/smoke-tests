/*
 * Copyright 2022 Cognitive Scale, Inc. All Rights Reserved
 */
package com.c12e.solution;

import com.c12e.fabric.ConfigurationProvider;
import com.c12e.fabric.CortexClient;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class Main {

    private static boolean allValuesPresent(String... params) {
        return Arrays.stream(params).filter(p -> Objects.isNull(p) || p.trim().isEmpty()).toArray().length > 0;
    }

    public static void main(String[] args) throws SQLException, IOException {
        BigQueryConnector connector = new BigQueryConnector();

        try {
            // cortex payload parsing
            Map<String, Object> requestBody = ConfigurationProvider.mapper.readValue(args[0], Map.class);
            String endpoint = (String) requestBody.get("apiEndpoint");
            String token = (String) requestBody.get("token");
            String project = (String) requestBody.get("projectId");
            Map<String, Object> payload = (Map<String, Object>) requestBody.get("payload");
            Map<String, Object> properties = (Map<String, Object>) requestBody.get("properties");
            String connectionName = (String) properties.get("connection-name");
            String folderPath = (String) properties.get("folder-path");
            int batchSize = 10000;
            if (properties.get("batch-size") != null) {
                batchSize = Integer.valueOf(properties.get("batch-size").toString());
            }
            String prefix = (String) properties.get("prefix");
            String sql = (String) payload.get("query");

            // validate params
            if (allValuesPresent(connectionName, folderPath, prefix, sql)) {
                throw new RuntimeException("'connection_name', 'folder-path', 'prefix' skill properties and 'query' in payload must be provided");
            }
            ConfigurationProvider configClient = new CortexClient(endpoint, token, project);
            ConfigurationProvider.JdbcConnectionParams jdbcParams = configClient.getConnectionParams(connectionName);
            connector.setup(jdbcParams);
            connector.execute(sql, folderPath, batchSize, token, endpoint, prefix, project);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
