package com.moldelab.shared;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Exceção de domínio com código estável (ver docs/api-contracts.md → Erros). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    public ApiException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " não encontrado(a)", null);
    }

    public static ApiException business(String code, String message, Map<String, Object> details) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message, details);
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public Map<String, Object> details() { return details; }
}
