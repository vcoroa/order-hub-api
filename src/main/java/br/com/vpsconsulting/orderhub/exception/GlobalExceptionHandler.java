package br.com.vpsconsulting.orderhub.exception;

import br.com.vpsconsulting.orderhub.dto.utils.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {

        log.warn("Entidade não encontrada: {}", ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                ex.getCodigo(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest request) {

        log.warn("Regra de negócio violada: {}", ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                ex.getCodigo(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            ValidationException ex, HttpServletRequest request) {

        log.warn("Erro de validação: {}", ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                ex.getCodigo(),
                ex.getMessage(),
                ex.getErros(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("Erro de validação de campos: {}", ex.getMessage());

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "VALIDATION_ERROR",
                "Dados de entrada inválidos",
                errors,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorResponseDTO> handleIntegration(
            IntegrationException ex, HttpServletRequest request) {

        log.error("Erro de integração: {}", ex.getMessage(), ex);

        ErrorResponseDTO error = ErrorResponseDTO.of(
                ex.getCodigo(),
                "Erro interno do sistema. Tente novamente mais tarde.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Argumento inválido: {}", ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // NOVO: Tratamento para recurso estático não encontrado (404) - Spring Boot 3
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.warn("Endpoint não encontrado: {}", request.getRequestURI());

        String message = String.format("Endpoint '%s' não encontrado", request.getRequestURI());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "ENDPOINT_NOT_FOUND",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // NOVO: Tratamento para endpoint não encontrado (404)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        log.warn("Endpoint não encontrado: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        String message = String.format("Endpoint '%s %s' não encontrado",
                ex.getHttpMethod(), ex.getRequestURL());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "ENDPOINT_NOT_FOUND",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // NOVO: Tratamento para método HTTP não suportado (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("Método HTTP não suportado: {} para {}", ex.getMethod(), request.getRequestURI());

        String supportedMethods = String.join(", ", ex.getSupportedMethods());
        String message = String.format("Método '%s' não suportado para este endpoint. Métodos suportados: %s",
                ex.getMethod(), supportedMethods);

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "METHOD_NOT_SUPPORTED",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneral(
            Exception ex, HttpServletRequest request) {

        log.error("Erro interno: {}", ex.getMessage(), ex);

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "INTERNAL_ERROR",
                "Erro interno do sistema",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        log.warn("Content-Type não suportado: {} para {}", ex.getContentType(), request.getRequestURI());

        String supportedTypes = ex.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        String message = String.format("Content-Type '%s' não suportado. Tipos suportados: %s",
                ex.getContentType(), supportedTypes);

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "UNSUPPORTED_MEDIA_TYPE",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("JSON malformado na requisição para: {}", request.getRequestURI());

        String message = "Formato de JSON inválido. Verifique a sintaxe do JSON enviado.";

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "INVALID_JSON",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}