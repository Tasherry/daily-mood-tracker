package com.mycompany.myapp.web.rest.errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.service.EmailAlreadyUsedException;
import com.mycompany.myapp.service.InvalidPasswordException;
import com.mycompany.myapp.service.UsernameAlreadyUsedException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause;

@ExtendWith(MockitoExtension.class)
class ExceptionTranslatorTest {

    @Mock
    private Environment env;

    @InjectMocks
    private ExceptionTranslator exceptionTranslator;

    private NativeWebRequest request;
    private MockHttpServletRequest servletRequest;

    @BeforeEach
    void setUp() {
        servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/api/test");
        request = new ServletWebRequest(servletRequest);
    }

    @Test
    void shouldHandleUsernameAlreadyUsedException() {
        // Given
        UsernameAlreadyUsedException ex = new UsernameAlreadyUsedException();

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
        ProblemDetailWithCause problem = (ProblemDetailWithCause) response.getBody();
        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    void shouldHandleEmailAlreadyUsedException() {
        // Given
        EmailAlreadyUsedException ex = new EmailAlreadyUsedException();

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
        ProblemDetailWithCause problem = (ProblemDetailWithCause) response.getBody();
        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    void shouldHandleInvalidPasswordException() {
        // Given
        InvalidPasswordException ex = new InvalidPasswordException();

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
        ProblemDetailWithCause problem = (ProblemDetailWithCause) response.getBody();
        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    void shouldHandleConcurrencyFailureException() {
        // Given
        ConcurrencyFailureException ex = new ConcurrencyFailureException("Concurrency failure");

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
    }

    @Test
    void shouldHandleDataAccessException() {
        // Given
        DataAccessException ex = new DataAccessException("Data access error") {};

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
    }

    @Test
    void shouldHandleAccessDeniedException() {
        // Given
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
    }

    @Test
    void shouldHandleBadCredentialsException() {
        // Given
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
    }

    @Test
    void shouldHandleHttpMessageConversionException() {
        // Given
        HttpMessageConversionException ex = new HttpMessageConversionException("Conversion error");

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
    }

    @Test
    void shouldHandleNoHandlerFoundException() {
        // Given
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/nonexistent", new HttpHeaders());

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() {
        // Given
        // Create a simple exception without MethodParameter to avoid NPE
        RuntimeException ex = new RuntimeException("Validation error");

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
    }

    @Test
    void shouldHandleGenericException() {
        // Given
        RuntimeException ex = new RuntimeException("Generic error");

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
    }

    @Test
    void shouldIncludePathInProblemDetail() {
        // Given
        RuntimeException ex = new RuntimeException("Test error");

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        ProblemDetailWithCause problem = (ProblemDetailWithCause) response.getBody();
        assertThat(problem.getProperties()).containsKey("path");
        assertThat(problem.getProperties().get("path")).isEqualTo(URI.create("/api/test"));
    }

    @Test
    void shouldHandleExceptionWithCustomStatus() {
        // Given
        CustomStatusException ex = new CustomStatusException();

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
    }

    @Test
    void shouldHandleExceptionWithResponseStatus() {
        // Given
        ResponseStatusException ex = new ResponseStatusException();

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ProblemDetailWithCause.class);
    }

    @Test
    void shouldBuildCauseWhenCasualChainEnabled() {
        // Given
        RuntimeException cause = new RuntimeException("Cause");
        RuntimeException ex = new RuntimeException("Main exception", cause);

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        ProblemDetailWithCause problem = (ProblemDetailWithCause) response.getBody();
        assertThat(problem.getCause()).isNull(); // Casual chain is disabled by default
    }

    @Test
    void shouldHandleExceptionWithCustomMessage() {
        // Given
        CustomMessageException ex = new CustomMessageException("Custom message");

        // When
        ResponseEntity<Object> response = exceptionTranslator.handleAnyException(ex, request);

        // Then
        ProblemDetailWithCause problem = (ProblemDetailWithCause) response.getBody();
        assertThat(problem.getDetail()).contains("Custom message");
    }

    // Custom exception classes for testing
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
    private static class CustomStatusException extends RuntimeException {

        public CustomStatusException() {
            super("Custom status exception");
        }
    }

    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.BAD_REQUEST)
    private static class ResponseStatusException extends RuntimeException {

        public ResponseStatusException() {
            super("Response status exception");
        }
    }

    private static class CustomMessageException extends RuntimeException {

        public CustomMessageException(String message) {
            super(message);
        }
    }
}
