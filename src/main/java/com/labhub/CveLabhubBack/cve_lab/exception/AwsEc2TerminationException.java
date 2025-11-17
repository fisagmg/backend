package com.labhub.CveLabhubBack.cve_lab.exception;

public class AwsEc2TerminationException extends RuntimeException {
    
    public AwsEc2TerminationException(String message) {
        super(message);
    }
    
    public AwsEc2TerminationException(String message, Throwable cause) {
        super(message, cause);
    }
}

