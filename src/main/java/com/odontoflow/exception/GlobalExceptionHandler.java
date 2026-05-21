package com.odontoflow.exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return badRequest(ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", ex.getStatusCode().value());
        body.put("error", ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> {
                    Map<String, String> error = new HashMap<>();
                    error.put("field", fieldError.getField());
                    error.put("message", fieldError.getDefaultMessage());
                    return error;
                })
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("status", 422);
        body.put("error", "Unprocessable Entity");
        body.put("message", "Validação falhou");
        body.put("errors", errors);
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.unprocessableEntity().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        MismatchedInputException mie = findCause(ex, MismatchedInputException.class);
        if (mie != null && mie.getTargetType() != null && mie.getTargetType().isEnum()) {
            String field = mie.getPath().isEmpty() ? "campo" : mie.getPath().get(mie.getPath().size() - 1).getFieldName();
            Object[] accepted = mie.getTargetType().getEnumConstants();
            return badRequest("Valor inválido para '" + field + "'. Aceitos: " + java.util.Arrays.toString(accepted) + ".");
        }
        Throwable root = ex.getMostSpecificCause();
        if (root != null && root.getMessage() != null && root.getMessage().contains("not one of the values accepted for Enum class")) {
            String msg = root.getMessage();
            int start = msg.indexOf("Enum class:");
            String tail = start >= 0 ? msg.substring(start + "Enum class:".length()) : "";
            int end = tail.indexOf(']');
            String accepted = end >= 0 ? tail.substring(0, end + 1).trim() : tail.trim();
            return badRequest("Valor de enum inválido no body. Aceitos: " + accepted);
        }
        return badRequest("Corpo da requisição inválido ou mal formatado.");
    }

    @SuppressWarnings("unchecked")
    private <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        Throwable t = ex;
        while (t != null) {
            if (type.isInstance(t)) return (T) t;
            if (t.getCause() == t) return null;
            t = t.getCause();
        }
        return null;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> required = ex.getRequiredType();
        if (required != null && required.isEnum()) {
            Object[] accepted = required.getEnumConstants();
            return badRequest("Valor inválido para '" + ex.getName() + "'. Aceitos: " + java.util.Arrays.toString(accepted) + ".");
        }
        return badRequest("Parâmetro '" + ex.getName() + "' inválido.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSize(MaxUploadSizeExceededException ex) {
        return badRequest("Arquivo excede o tamanho máximo permitido.");
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
