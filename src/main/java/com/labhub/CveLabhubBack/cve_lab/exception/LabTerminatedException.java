package com.labhub.CveLabhubBack.cve_lab.exception;

public class LabTerminatedException extends RuntimeException {
    
    public LabTerminatedException(String uuid) {
        super("Lab session already terminated: " + uuid);
    }
}

