package com.c12e.fabric;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

/**
 * Cortex implementation of Cortex JDBC/CDATA Connections ConfigurationProvider
 *
 * This class helps parsing Cortex Connections and setting up CDATA license validation
 */
public class CortexClient implements ConfigurationProvider {

    private static final String  SHARED_PROJECT = "shared";
    private static final String  SHARED_OEM_KEY = "cdata-oem-key";
    private static final String  PRODUCT_CHECKSUM = "cdata-prdct-checksum";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder().readTimeout(180, TimeUnit.SECONDS).build(); // increase timeout for downloading managed content jar

    private final String endpoint, token, project, libDir, sharedProject, accountsEndpoint, connectionsEndpoint;

    public CortexClient(String endpoint, String token, String project, String libDir, String sharedProject) {
        this.endpoint = endpoint;
        this.token = token;
        this.project = project;
        this.libDir = libDir;
        this.sharedProject = sharedProject;
        loadEnv();

        this.connectionsEndpoint = String.format("%s:%s", this.endpoint.replace("cortex-internal", "cortex-connections"), env.getProperty("CORTEX_CONNECTIONS_SERVICE_PORT"));
        this.accountsEndpoint = String.format("%s:%s", this.endpoint.replace("cortex-internal", "cortex-accounts"), env.getProperty("CORTEX_ACCOUNTS_SERVICE_PORT"));
    }

    public CortexClient(String endpoint, String token, String project) {
        this(endpoint, token, project, "lib", SHARED_PROJECT);
    }

    private Response cortexRequest(String url) throws IOException {
        Request request = new Request.Builder().url(url)
                .addHeader("Authorization", "Bearer " + this.token)
                .build();
        Response res = HTTP_CLIENT.newCall(request).execute();
        if (!res.isSuccessful()) {
            throw new RuntimeException("Request failed: " + url);
        }
        return res;
    }

    private String getSecretValue(String secretKey) throws IOException {
        // To read Secret value from shared project, connect to `cortex-accounts` directly to invoke internal APIs
        String url = String.format("%s/internal/projects/%s/secrets/%s", accountsEndpoint, sharedProject, secretKey);
        Response res = cortexRequest(url);
        if (!res.isSuccessful() || !res.headers("content-type").contains("application/json")) {
            throw new RuntimeException(String.format("Unable to get Secret %s: [%d] %s. Response type: %s ", url, res.code(), res.message(), res.headers("content-type")));
        }
        String body = res.body().string();
        return (String) mapper.readValue(body, Map.class).get("value");
    }

    @Override
    public String getLicenseKey() throws IOException {
        System.setProperty("product_checksum", getSecretValue(PRODUCT_CHECKSUM));
        return getSecretValue(SHARED_OEM_KEY);
    }

    public Path getSharedManagedContentFile(String location) throws IOException {
        Response res = cortexRequest(String.format("%s/fabric/v4/projects/%s/content/%s", this.endpoint, sharedProject, location));
        Path path = Paths.get(libDir, location).normalize();
        Files.createDirectories(path.getParent());
        if (Files.notExists(path)) {
            Files.copy(res.body().byteStream(), path);
        }
        JvmClasspathAgent.appendJarFile(new JarFile(path.toFile()));
        return path;
    }

    @Override
    public JdbcConnectionParams getConnectionParams(String connectionName) throws IOException {
        // get Cortex connection with Connection Secrets resolved. Hence, directly connect to internal APIs
        String url = String.format("%s/internal/projects/%s/connections/%s", connectionsEndpoint, project, connectionName);
        Response res = cortexRequest(url);
        if (!res.isSuccessful() || !res.headers("content-type").contains("application/json")) {
            throw new RuntimeException(String.format("Unable to get Connection %s: [%d] %s. Response type: %s ", url, res.code(), res.message(), res.headers("content-type")));
        }
        // parse cortex connection params
        JdbcConnectionParams jdbcConfig = parseCortexConnectionDefinition(res.body().string());
        // download driver Jar, if required
        try {
            Class.forName(jdbcConfig.getDriverClassname());
        } catch (ClassNotFoundException e) {
            String managedContentKey = (String) jdbcConfig.getRawParams().get(MANAGED_CONTENT_KEY);
            if (Objects.isNull(managedContentKey) || managedContentKey.trim().isEmpty()) {
                throw new RuntimeException(String.format("Driver class %s required for JDBC connection is not on classpath. ", jdbcConfig.getDriverClassname()));
            }
            getSharedManagedContentFile((String) jdbcConfig.getRawParams().get(MANAGED_CONTENT_KEY));
        }
        return jdbcConfig;
    }
}