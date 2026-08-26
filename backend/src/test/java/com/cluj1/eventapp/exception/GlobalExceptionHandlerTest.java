package com.cluj1.eventapp.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import com.cluj1.eventapp.dto.ErrorResponse;

@SuppressWarnings("unchecked")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAuthorizationDeniedException_returns403WithForbiddenMessage() {
        AuthorizationResult result = new AuthorizationResult() {
            @Override
            public boolean isGranted() {
                return false;
            }
        };
        AuthorizationDeniedException ex = new AuthorizationDeniedException("denied", result);

        ResponseEntity<Object> response = handler.handleAuthorizationDeniedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "Forbidden");
        assertThat(body).containsEntry("status", HttpStatus.FORBIDDEN.value());
        assertThat(body).containsKey("timestamp");
        assertThat((String) body.get("message")).contains("do not have the required role");
    }

    @Test
    void handleAccessDeniedException_returns403WithAdminHint() {
        ResponseEntity<Object> response = handler.handleAccessDeniedException(new AccessDeniedException("nope"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "Forbidden");
        assertThat((String) body.get("message")).contains("ADMIN");
    }

    @Test
    void handleBadCredentialsException_returns401WithMessage() {
        ResponseEntity<ErrorResponse> response = handler
                .handleBadCredentialsException(new BadCredentialsException("bad creds"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        assertThat(response.getBody().getMessage()).isEqualTo("bad creds");
    }

    @Test
    void handleValidationExceptions_joinsFieldMessages() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "Invalid email format"));
        bindingResult.addError(new FieldError("target", "password", "Password is required"));
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyForMethodParam"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage())
                .contains("email: Invalid email format")
                .contains("password: Password is required");
    }

    @Test
    void handleResponseStatusException_returnsGivenStatus_andPassesReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "conflict reason");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("conflict reason");
    }

    @Test
    void handleResponseStatusException_fallsBackToGenericMessage_whenReasonIsNull() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("An error occurred");
    }

    @Test
    void handleEntityNotFoundException_returns404WithMessage() {
        ResponseEntity<ErrorResponse> response = handler
                .handleEntityNotFoundException(new EntityNotFoundException("Missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Missing");
    }

    @Test
    void handleGenericException_returns500WithGenericMessage() {
        ResponseEntity<ErrorResponse> response = handler
                .handleGenericException(new RuntimeException("something broke"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred.");
    }

    @Test
    void handleEmailAlreadyRegisteredException_returns409WithMessage() {
        ResponseEntity<ErrorResponse> response = handler
                .handleEmailAlreadyRegisteredException(new EmailAlreadyRegisteredException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).isNotBlank();
    }

    @Test
    void handleHttpMessageNotReadable_returns400WithCheckinCodeMessage() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "cannot parse", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("checkin.error.code.invalid");
    }

    @Test
    void handleIllegalArgumentException_returns400WithMessage() {
        ResponseEntity<ErrorResponse> response = handler
                .handleIllegalArgumentException(new IllegalArgumentException("Bad input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Bad input");
    }

    @Test
    void handleInvalidEventOperationException_returns400WithMessage() {
        ResponseEntity<ErrorResponse> response = handler
                .handleInvalidEventOperationException(new InvalidEventOperationException("cannot publish"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("cannot publish");
    }
    @SuppressWarnings("unused")
    private void dummyForMethodParam() {
        List.of();
    }
}

