package com.labhub.CveLabhubBack.config;

import com.labhub.CveLabhubBack.dto.AuthResponseWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400: 요청 형식 오류
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthResponseWrapper<Object>> handleValidation(MethodArgumentNotValidException ex) {

        // 어떤 필드가 왜 잘못됐는지 중 첫 번째만 뽑아 메시지 구성
        FieldError fieldError = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .orElse(null);

        String message = "email 형식이 올바르지 않거나 필수 값이 누락되었습니다.";
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(AuthResponseWrapper.error(
                        400,
                        message
                ));
    }

    // 예기치 못한 나머지 전부 500으로
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponseWrapper<Object>> handleAny(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponseWrapper.error(
                        500,
                        "잠시 후 다시 시도해주세요."
                ));
    }
}
