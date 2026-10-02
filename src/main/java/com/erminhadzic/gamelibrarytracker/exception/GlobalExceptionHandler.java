package com.erminhadzic.gamelibrarytracker.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;

// One place that turns exceptions from any controller into a JSON error body (ProblemDetail, RFC 9457).
// The base class already handles Spring MVC's own exceptions in that format: malformed JSON, a wrong HTTP method,
// a missing parameter, an unknown path, and ResponseStatusException (it keeps the status and uses the reason as "detail").
// This class adds the field list for validation errors, the 401 body and a catch-all for everything unexpected.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // One entry of the "errors" list in a validation error response
    public record FieldValidationError(String field, String message) {
    }

    // Thrown when a @Valid @RequestBody breaks a rule on the DTO. Adds an "errors" list that names each field.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                // The validator returns errors in no fixed order; sorting keeps the response stable
                .sorted(Comparator.comparing(FieldValidationError::field).thenComparing(FieldValidationError::message))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    // An unknown path. Spring's own message is "No static resource ...", which is confusing for an API client.
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
                                                                    HttpHeaders headers,
                                                                    HttpStatusCode status,
                                                                    WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "No endpoint at this path");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    // A missing or invalid token is rejected by the security filter chain, before any controller runs.
    // ProblemDetailAuthenticationEntryPoint passes that exception here, so the 401 has the same body format.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Authentication is required: send a valid Bearer token");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    // Catch-all: the real exception goes to the server log, the client only gets a generic message
    // (no exception class, SQL or stack trace)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        logger.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
