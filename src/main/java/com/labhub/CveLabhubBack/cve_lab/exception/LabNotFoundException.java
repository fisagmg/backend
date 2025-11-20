package com.labhub.CveLabhubBack.cve_lab.exception;

public class LabNotFoundException extends RuntimeException {
    
    public LabNotFoundException(String uuid) {
        super("Lab session not found with uuid: " + uuid);
    }
}


