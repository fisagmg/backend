package com.labhub.CveLabhubBack.report.exception;

public class ReportNotFoundException extends RuntimeException {
    
    public ReportNotFoundException(String message) {
        super(message);
    }
    
    public ReportNotFoundException(Long reportId) {
        super("Report not found with id: " + reportId);
    }
}

