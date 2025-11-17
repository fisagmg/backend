package com.labhub.CveLabhubBack.report.exception;

import com.labhub.CveLabhubBack.cve_lab.exception.AwsEc2TerminationException;
import com.labhub.CveLabhubBack.cve_lab.exception.LabExtensionNotAllowedException;
import com.labhub.CveLabhubBack.cve_lab.exception.LabNotFoundException;
import com.labhub.CveLabhubBack.cve_lab.exception.LabTerminatedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleReportNotFoundException(ReportNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CustomStorageException.class)
    public ResponseEntity<Map<String, Object>> handleCustomStorageException(CustomStorageException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Storage Error");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    // === Lab Session Exception Handlers ===
    
    @ExceptionHandler(LabNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLabNotFoundException(LabNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(LabTerminatedException.class)
    public ResponseEntity<Map<String, Object>> handleLabTerminatedException(LabTerminatedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(LabExtensionNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleLabExtensionNotAllowedException(LabExtensionNotAllowedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(AwsEc2TerminationException.class)
    public ResponseEntity<Map<String, Object>> handleAwsEc2TerminationException(AwsEc2TerminationException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

