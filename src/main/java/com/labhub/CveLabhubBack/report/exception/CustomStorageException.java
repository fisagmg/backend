package com.labhub.CveLabhubBack.report.exception;

public class CustomStorageException extends RuntimeException {
    
    public CustomStorageException(String message) {
        super(message);
    }
    
    public CustomStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

