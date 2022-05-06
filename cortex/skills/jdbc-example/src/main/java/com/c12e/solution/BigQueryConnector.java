/*
 * Copyright 2022 Cognitive Scale, Inc. All Rights Reserved
 */
package com.c12e.solution;

import com.c12e.fabric.ConfigurationProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.sql.*;
import java.util.List;
import java.util.Objects;

public class BigQueryConnector {
    private HikariDataSource connectionPool;

    private static char DELIMITER = ',';

    public void setup(ConfigurationProvider.JdbcConnectionParams connectionParams) {
        if (!Objects.isNull(connectionPool)) {
            connectionPool.close();
        }
        connectionPool = createHikariDataSource(connectionParams);
    }

    public ResponseEntity execute(String sql, String folderPath, int batchSize, String cortexToken,
                                  String cortexUrl, String prefix, String project) throws SQLException {
        ResponseEntity result = new ResponseEntity();
        result.setStatusCode(HttpURLConnection.HTTP_OK);
        if (Objects.isNull(connectionPool)) {
            throw new SQLException("Connection pool is not initialized. Please call setup to initialize the pool");
        }
        try (Connection connection = connectionPool.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql);
        ) {
            System.out.println("resultset received");
            extractData(resultSet, folderPath, batchSize, cortexToken, cortexUrl, prefix, project);
            result.setData("Data imported successfully");
        } catch (final IOException | SQLException e) {
            result.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    private void extractData(final ResultSet rs, String folderPath, int batchSize, String cortexToken,
                             String cortexUrl, String prefix, String project) throws SQLException, IOException {
        try {
            int fileCount = 0;
            int rowCount = 0;
            final ResultSetMetaData rsmd = rs.getMetaData();
            final int columnCount = rsmd.getColumnCount();
            String filePath = folderPath + "/" + fileCount + ".csv";
            FileOutputStream fileOs = createNewFile(filePath);
            PrintWriter pw = new PrintWriter(fileOs, true);
            writeHeader(rsmd, columnCount, pw);
            while (rs.next()) {
                if (rowCount > batchSize) {
                    pw.close();
                    fileOs.close();
                    uploadFile(filePath, fileCount + ".csv", cortexToken, cortexUrl, prefix, project);
                    fileCount++;
                    filePath = folderPath + "/" + fileCount + ".csv";
                    fileOs = createNewFile(filePath);
                    pw = new PrintWriter(fileOs, true);
                    writeHeader(rsmd, columnCount, pw);
                }
                for (int i = 1; i <= columnCount; i++) {
                    final Object value = rs.getObject(i);
                    pw.write(value == null ? "" : value.toString());
                    if (i != columnCount) {
                        pw.append(DELIMITER);
                    }
                }
                pw.println();
                rowCount++;
            }
            pw.close();
            fileOs.close();
            uploadFile(filePath, fileCount + ".csv", cortexToken, cortexUrl, prefix, project);
            pw.flush();
        } catch (final IOException | SQLException e) {
            System.out.println("extractData Failed..." + e.getMessage());
            throw e;
        }
    }

    private void writeDataToManagedContent(String filePath, String fileName, String cortexToken,
                                           String cortexUrl, String prefix, String project)
            throws IOException {
        try {
            System.out.println("writeDataToManagedContent ... " + filePath);
            File uploadFile = new File(filePath);
            String location = prefix + "/" + fileName;
            String requestURL = String.format("%s/fabric/v4/projects/%s/content/%s", cortexUrl, project, location);
            String charset = "UTF-8";
            FileWriterUtil multipart = new FileWriterUtil(requestURL, charset, cortexToken);
            multipart.addFilePart("fileUpload", uploadFile);
            List<String> response = multipart.finish();
        } catch (IOException ex) {
            System.out.println("writeDataToManagedContent Failed..." + ex.getMessage());
            throw ex;
        }
    }

    private FileOutputStream createNewFile(String filePath) throws IOException {
        System.out.println("createNewFile ... " + filePath);
        File file = new File(filePath);
        file.getParentFile().mkdirs(); // Will create parent directories if not exists
        file.createNewFile();
        return new FileOutputStream(file, false);
    }

    private void uploadFile(String filePath, String fileName, String cortexToken, String cortexUrl, String prefix,
                            String project) throws IOException {
        writeDataToManagedContent(filePath, fileName, cortexToken, cortexUrl, prefix, project);
        System.out.println("File uploaded... " + fileName);
        File file = new File(filePath);
        file.delete();

    }

    private static void writeHeader(final ResultSetMetaData rsmd,
                                    final int columnCount, final PrintWriter pw) throws SQLException {
        for (int i = 1; i <= columnCount; i++) {
            pw.write(rsmd.getColumnName(i));
            if (i != columnCount) {
                pw.append(DELIMITER);
            }
        }
        pw.println();
    }

    private HikariDataSource createHikariDataSource(ConfigurationProvider.JdbcConnectionParams connectionParams) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(connectionParams.getDriverClassname());
        config.setDataSourceProperties(connectionParams.getConnectionProperties());
        config.setJdbcUrl(connectionParams.getConnectionProtocol());
        if (connectionParams.getConnectionProperties().getProperty("username") != null)
            config.setUsername(connectionParams.getConnectionProperties().getProperty("username"));
        return new HikariDataSource(config);
    }
}
