package com.labhub.CveLabhubBack.cve_lab.client;

public class RunnerClientException extends RuntimeException {
    public RunnerClientException(String message) {
        super(message);
    }

    public RunnerClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

