package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.practicum.dto.ApiError;

import java.time.LocalDateTime;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ApiError> handleNotFoundException(NotFoundException e) {
        log.info("404 {}", e.getMessage());
        String notFoundReason = "The required object was not found.";
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.builder()
                .status(HttpStatus.NOT_FOUND.toString())
                .reason(notFoundReason)
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConflictException.class})
    public ResponseEntity<ApiError> handleConflictException(Exception e) {
        log.info("409 {}", e.getMessage());
        String conflictReason = "Integrity constraint has been violated.";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.builder()
                .status(HttpStatus.CONFLICT.toString())
                .reason(conflictReason)
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentNotValidException.class, BadRequestException.class})
    public ResponseEntity<ApiError> handleBadRequestException(Exception e) {
        log.info("400 {}", e.getMessage());
        String badRequestReason = "Incorrectly made request.";
        return ResponseEntity.badRequest()
                .body(ApiError.builder()
                        .status(HttpStatus.BAD_REQUEST.toString())
                        .reason(badRequestReason)
                        .message(e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
