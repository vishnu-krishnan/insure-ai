package com.insureai.insurance.adapter.inbound.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.insureai.insurance.domain.exception.CustomerProfileMissingException;
import com.insureai.insurance.domain.exception.CustomerReferenceNotFoundException;
import com.insureai.insurance.domain.exception.CustomerServiceTimeoutException;
import com.insureai.insurance.domain.exception.CustomerServiceUnavailableException;
import com.insureai.insurance.domain.exception.DomainValidationException;
import com.insureai.insurance.domain.exception.QuoteNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(DomainValidationException exception) {
        LOGGER.atWarn()
            .setMessage("Quote request rejected: {}")
            .addArgument(exception.getMessage())
            .addKeyValue("event.action", "quote.rejected")
            .addKeyValue("reason", "invalid_request")
            .addKeyValue("rejectedField", exception.field())
            .log();
        ProblemDetail problem = ApiProblem.of(
            HttpStatus.BAD_REQUEST,
            "Invalid quote request",
            exception.getMessage(),
            "INVALID_QUOTE_REQUEST"
        );
        if (exception.field() != null) {
            problem.setProperty("errors", List.of(new FieldViolation(exception.field(), exception.getMessage())));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(QuoteNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleQuoteNotFound(QuoteNotFoundException exception) {
        LOGGER.atInfo()
            .setMessage("Quote lookup found nothing: {}")
            .addArgument(exception.getMessage())
            .addKeyValue("event.action", "quote.not_found")
            .log();
        return ApiProblem.response(
            HttpStatus.NOT_FOUND,
            "Quote not found",
            exception.getMessage(),
            "QUOTE_NOT_FOUND"
        );
    }

    @ExceptionHandler(CustomerProfileMissingException.class)
    public ResponseEntity<ProblemDetail> handleProfileMissing(CustomerProfileMissingException exception) {
        LOGGER.atInfo()
            .setMessage("Quote request rejected: {}")
            .addArgument(exception.getMessage())
            .addKeyValue("event.action", "quote.rejected")
            .addKeyValue("reason", "profile_missing")
            .log();
        return ApiProblem.response(
            HttpStatus.CONFLICT,
            "Profile required",
            exception.getMessage(),
            "PROFILE_REQUIRED"
        );
    }

    @ExceptionHandler(CustomerReferenceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUnknownCustomer(CustomerReferenceNotFoundException exception) {
        LOGGER.atWarn()
            .setMessage("Quote request rejected: {}")
            .addArgument(exception.getMessage())
            .addKeyValue("event.action", "quote.rejected")
            .addKeyValue("reason", "unknown_customer")
            .log();
        return ApiProblem.response(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Invalid customer reference",
            exception.getMessage(),
            "CUSTOMER_REFERENCE_INVALID"
        );
    }

    @ExceptionHandler(CustomerServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleCustomerServiceUnavailable(
        CustomerServiceUnavailableException exception
    ) {
        LOGGER.atWarn()
            .setMessage("Customer Service lookup failed: {}")
            .addArgument(exception.getMessage())
            .addKeyValue("event.action", "customer_service.unavailable")
            .log();
        return ApiProblem.response(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Customer Service unavailable",
            "Customer information is temporarily unavailable. Please retry the request.",
            "CUSTOMER_SERVICE_UNAVAILABLE"
        );
    }

    @ExceptionHandler(CustomerServiceTimeoutException.class)
    public ResponseEntity<ProblemDetail> handleCustomerServiceTimeout(CustomerServiceTimeoutException exception) {
        LOGGER.atWarn()
            .setMessage("Customer Service lookup timed out.")
            .addKeyValue("event.action", "customer_service.timeout")
            .log();
        return ApiProblem.response(
            HttpStatus.GATEWAY_TIMEOUT,
            "Customer Service timeout",
            "Customer information could not be retrieved before the request timed out.",
            "CUSTOMER_SERVICE_TIMEOUT"
        );
    }

    @ExceptionHandler({DataAccessException.class, TransactionSystemException.class})
    public ResponseEntity<ProblemDetail> handlePersistenceFailure(RuntimeException exception) {
        LOGGER.atError()
            .setMessage("Quote persistence failed ({}).")
            .addArgument(exception.getClass().getSimpleName())
            .addKeyValue("event.action", "persistence.failed")
            .addKeyValue("error.type", exception.getClass().getSimpleName())
            .log();
        return ApiProblem.response(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Persistence failure",
            "The quote could not be saved. Please try again later.",
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
            .setMessage("Quote request validation failed on fields {}.")
            .addArgument(fields)
            .addKeyValue("event.action", "quote.rejected")
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
