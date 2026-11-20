package com.moldelab.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ApiError(String code, String message, Map<String, Object> details) {}

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException e) {
        return ResponseEntity.status(e.status()).body(new ApiError(e.code(), e.getMessage(), e.details()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        Map<String, Object> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Dados inválidos", Map.of("fields", fields)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ApiError("MALFORMED_JSON", "Corpo da requisição inválido", Map.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUpload(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError("FILE_TOO_LARGE", "Arquivo grande demais (máx. 8 MB)", Map.of()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(org.springframework.dao.DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("INTEGRITY_VIOLATION", "Referência inválida (ex.: perfil de medidas inexistente)", Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_ARGUMENT", e.getMessage(), Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e) {
        // exceções padrão do Spring MVC (rota inexistente, parte multipart ausente, tipo de mídia, UUID inválido...)
        if (e instanceof org.springframework.web.ErrorResponse er) {
            int status = er.getStatusCode().value();
            String code = status == 404 ? "NOT_FOUND" : status == 405 ? "METHOD_NOT_ALLOWED" : status == 415 ? "UNSUPPORTED_MEDIA_TYPE" : "BAD_REQUEST";
            String message = status == 404 ? "Recurso não encontrado" : "Requisição inválida";
            return ResponseEntity.status(status).body(new ApiError(code, message, Map.of()));
        }
        if (e instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
                || e instanceof org.springframework.web.multipart.MultipartException) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", "Requisição inválida", Map.of()));
        }
        log.error("Erro inesperado", e);
        return ResponseEntity.internalServerError()
                .body(new ApiError("INTERNAL_ERROR", "Erro interno. Tente novamente.", Map.of()));
    }
}
