/*
 * Copyright 2022 Cognitive Scale, Inc. All Rights Reserved
 */
package com.c12e.solution;


public class ResponseEntity {

    /**
     * The data.
     */
    Object data;

    /**
     * The status code.
     */
    int statusCode;

    /**
     * The errormessage.
     */
    String errorMessage;

    public ResponseEntity() {
    }

    /**
     * Instantiates a new response entity.
     *
     * @param data         the data
     * @param statusCode   the status code
     * @param errorMessage the errorMessage
     */
    public ResponseEntity(Object data, int statusCode, String errorMessage) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}