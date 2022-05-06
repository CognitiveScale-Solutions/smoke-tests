/*
 * Copyright 2022 Cognitive Scale, Inc. All Rights Reserved
 */
package com.c12e.solution;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FileWriterUtil {

    /**
     * The boundary.
     */
    private final String boundary;

    /**
     * The http conn.
     */
    private HttpURLConnection httpConn;

    /**
     * The output stream.
     */
    private OutputStream outputStream;

    /**
     * The writer.
     */
    private PrintWriter writer;

    /**
     * Instantiates a new file write utility.
     *
     * @param requestURL  the request URL
     * @param charset     the charset
     * @param cortexToken the cortex token
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public FileWriterUtil(String requestURL, String charset, String cortexToken) throws IOException {

        boundary = "===" + System.currentTimeMillis() + "===";

        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        httpConn.setRequestProperty("Authorization", "Bearer " + cortexToken);
        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
    }

    /**
     * Adds the file part.
     *
     * @param fieldName  the field name
     * @param uploadFile the upload file
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void addFilePart(String fieldName, File uploadFile) throws IOException {
        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();
    }

    /**
     * Finish.
     *
     * @return the list
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public List<String> finish() throws IOException {
        List<String> response = new ArrayList<>();

        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }

        return response;
    }

}