package ro.tuc.ds2020.controllers.handlers;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ro.tuc.ds2020.controllers.handlers.exceptions.model.CustomException;
import ro.tuc.ds2020.controllers.handlers.exceptions.model.DuplicateResourceException;
import ro.tuc.ds2020.controllers.handlers.exceptions.model.ExceptionHandlerResponseDTO;
import ro.tuc.ds2020.controllers.handlers.exceptions.model.InvalidCredentialsException;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException e, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<String> details = e.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        ExceptionHandlerResponseDTO body = new ExceptionHandlerResponseDTO(
                ConstraintViolationException.class.getSimpleName(),
                status.getReasonPhrase(), status.value(),
                e.getMessage(), details, request.getDescription(false));
        return handleExceptionInternal(e, body, new HttpHeaders(), status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<String> details = new ArrayList<>();
        for (ObjectError err : ex.getBindingResult().getAllErrors()) {
            String name = (err instanceof FieldError fe) ? fe.getField() : err.getObjectName();
            details.add(name + ": " + err.getDefaultMessage());
        }
        ExceptionHandlerResponseDTO body = new ExceptionHandlerResponseDTO(
                ex.getParameter().getParameterName(),
                HttpStatus.valueOf(status.value()).getReasonPhrase(),
                status.value(),
                MethodArgumentNotValidException.class.getSimpleName(),
                details, request.getDescription(false));
        return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
    }

    @ExceptionHandler(value = {InvalidCredentialsException.class, DuplicateResourceException.class})
    protected ResponseEntity<Object> handleCustom(CustomException ex, WebRequest request) {
        ExceptionHandlerResponseDTO body = new ExceptionHandlerResponseDTO(
                ex.getResource(), ex.getStatus().getReasonPhrase(),
                ex.getStatus().value(), ex.getMessage(),
                ex.getValidationErrors(), request.getDescription(false));
        return handleExceptionInternal(ex, body, new HttpHeaders(), ex.getStatus(), request);
    }
}
