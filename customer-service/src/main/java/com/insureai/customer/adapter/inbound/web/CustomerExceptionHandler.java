package com.insureai.customer.adapter.inbound.web;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.insureai.customer.domain.exception.CustomerNotFoundException;
import com.insureai.customer.domain.exception.CustomerValidationException;
import com.insureai.customer.domain.exception.DuplicateCustomerException;

@RestControllerAdvice
public class CustomerExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerExceptionHandler.class);

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(CustomerNotFoundException exception) {
        LOGGER.atInfo()
            .setMessage("Customer lookup found nothing: {}")
            .addArgument(exception.getMessage())
            .addKeyValue("event.action", "customer.not_found")
            .log();
        return ApiProblem.response(
            HttpStatus.NOT_FOUND,
            "Customer not found",
            exception.getMessage(),
            "CUSTOMER_NOT_FOUND"
        );
    }

    @ExceptionHandler(CustomerValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(CustomerValidationException exception) {
        LOGGER.atWarn()
            .setMessage("Customer request rejected: {}")
            .addArgument(exception.getMessage())
            .addKeyValue("event.action", "customer.rejected")
            .addKeyValue("reason", "invalid_request")
            .log();
        return ApiProblem.response(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            exception.getMessage(),
            "INVALID_CUSTOMER_REQUEST"
        );
    }

    @ExceptionHandler(DuplicateCustomerException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateCustomerException exception) {
        LOGGER.atWarn()
            .setMessage("Duplicate customer rejected on field {}.")
            .addArgument(exception.field())
            .addKeyValue("event.action", "customer.rejected")
            .addKeyValue("rejectedField", exception.field())
            .addKeyValue("reason", "duplicate")
            .log();
        ProblemDetail problem = ApiProblem.of(
            HttpStatus.CONFLICT,
            "Duplicate customer",
            exception.getMessage(),
            "DUPLICATE_CUSTOMER_" + exception.field().toUpperCase(Locale.ROOT)
        );
        problem.setProperty("errors", List.of(new FieldViolation(exception.field(), exception.getMessage())));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handlePersistenceFailure(DataAccessException exception) {
        LOGGER.atError()
            .setMessage("Customer persistence failed ({}).")
            .addArgument(exception.getClass().getSimpleName())
            .addKeyValue("event.action", "persistence.failed")
            .addKeyValue("error.type", exception.getClass().getSimpleName())
            .log();
        return ApiProblem.response(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Persistence failure",
            "The customer request could not be completed. Please try again later.",
            "PERSISTENCE_FAILURE"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        LOGGER.atError()
            .setMessage("Unhandled exception while processing the request.")
            .setCause(exception)
            .addKeyValue("event.action", "request.failed")
            .log();
        return ApiProblem.response(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "An unexpected error occurred. Please try again later.",
            "INTERNAL_ERROR"
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
            .toList();
        String detail = violations.isEmpty() ? "Request validation failed" : violations.get(0).message();
        List<String> fields = violations.stream().map(FieldViolation::field).distinct().toList();
        LOGGER.atWarn()
            .setMessage("Customer request validation failed on fields {}.")
            .addArgument(fields)
            .addKeyValue("event.action", "customer.rejected")
            .addKeyValue("reason", "validation")
            .addKeyValue("rejectedFields", fields)
            .log();

        ProblemDetail problem = ApiProblem.of(HttpStatus.BAD_REQUEST, "Validation failed", detail, "VALIDATION_FAILED");
        problem.setProperty("errors", violations);
        return new ResponseEntity<>(problem, headers, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception exception,
        @Nullable Object body,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest request
    ) {
        if (statusCode.is5xxServerError()) {
            LOGGER.atError()
                .setMessage("Request failed with status {}.")
                .addArgument(statusCode.value())
                .setCause(exception)
                .addKeyValue("event.action", "request.failed")
                .addKeyValue("http.response.status_code", statusCode.value())
                .log();
        }
        if (body instanceof ProblemDetail problem) {
            HttpStatus resolved = HttpStatus.resolve(statusCode.value());
            ApiProblem.enrich(problem, resolved != null ? resolved.name() : "ERROR");
        }
        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
    }
}
