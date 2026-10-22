package org.spring.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.spring.dto.response.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.LinkedHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 409
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException e, HttpServletRequest req) {
        return this.buildResponseEntity(HttpStatus.CONFLICT, e.getMessage(), req);
    }

    // 404 for user
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e, HttpServletRequest req) {
        return this.buildResponseEntity(HttpStatus.NOT_FOUND, e.getMessage(), req);
    }

    // 401
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e, HttpServletRequest req) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"User Management API\"").body(this.buildErrorResponse(status, e.getMessage(), req));
    }

    // 403
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e, HttpServletRequest req) {
        return this.buildResponseEntity(HttpStatus.FORBIDDEN, e.getMessage(), req);
    }

    // 400 with field error for validation
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest req) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = this.buildErrorResponse(status, "Validation failed", req);
        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fieldError -> fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        errorResponse.setFieldErrors(fieldErrors);
        return ResponseEntity.status(status).body(errorResponse);
    }

    // 400
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest req) {
        return this.buildResponseEntity(HttpStatus.BAD_REQUEST, "Malformed request body", req);
    }

    // 400 for wrong password
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException e, HttpServletRequest req) {
        return this.buildResponseEntity(HttpStatus.BAD_REQUEST, e.getMessage(), req);
    }

    // 500
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleAll(Exception e, HttpServletRequest req) {
        GlobalExceptionHandler.log.error("Unhandled exception", e);
        return this.buildResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error Occurred", req);
    }

    // 404 for whatever endpoint
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException e, HttpServletRequest req) {
        return this.buildResponseEntity(HttpStatus.NOT_FOUND, "Endpoint not found", req);
    }

    // 405
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return this.buildResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage(), req);
    }

    // 415
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e, HttpServletRequest req) {
        return this.buildResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type not supported", req);
    }

    // 400 for wrong parameter type
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return this.buildResponseEntity(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'", req);
    }

    private ResponseEntity<ErrorResponse> buildResponseEntity(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(this.buildErrorResponse(status, message, req));
    }

    private ErrorResponse buildErrorResponse(HttpStatus status, String message, HttpServletRequest req) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, req.getRequestURI());
    }
}
