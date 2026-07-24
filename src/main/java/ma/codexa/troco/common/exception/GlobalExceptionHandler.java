package ma.codexa.troco.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.observability.ApiErrorMdc;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException ex, WebRequest request) {
        try (ApiErrorMdc ignored = ApiErrorMdc.start(request, ex.getStatus(), "business")) {
            log.error("Business exception code={}: {}", ex.getErrorCode(), ex.getMessage(), ex);

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    ex.getStatus(), ex.getMessage());
            problemDetail.setTitle(ex.getErrorCode());
            problemDetail.setProperty("timestamp", LocalDateTime.now());
            problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

            return ResponseEntity.status(ex.getStatus()).body(problemDetail);
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        try (ApiErrorMdc ignored = ApiErrorMdc.start(request, HttpStatus.NOT_FOUND, "not_found")) {
            log.warn("Resource not found: {}", ex.getMessage());

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND, ex.getMessage());
            problemDetail.setTitle("Resource Not Found");
            problemDetail.setProperty("timestamp", LocalDateTime.now());
            problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        try (ApiErrorMdc ignored = ApiErrorMdc.start(request, HttpStatus.BAD_REQUEST, "validation")) {
            log.warn("Validation error fields={}",
                    ex.getBindingResult().getFieldErrors().stream()
                            .map(FieldError::getField)
                            .collect(Collectors.joining(",")));

            List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(error -> {
                        ErrorResponse.ValidationError ve = new ErrorResponse.ValidationError();
                        ve.setField(error.getField());
                        ve.setMessage(error.getDefaultMessage());
                        ve.setRejectedValue(error.getRejectedValue());
                        return ve;
                    })
                    .collect(Collectors.toList());

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST, "Erreur de validation");
            problemDetail.setTitle("Validation Error");
            problemDetail.setProperty("timestamp", LocalDateTime.now());
            problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));
            problemDetail.setProperty("validationErrors", validationErrors);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
        }
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        try (ApiErrorMdc ignored = ApiErrorMdc.start(request, HttpStatus.BAD_REQUEST, "constraint_violation")) {
            log.warn("Constraint violation: {}", ex.getMessage());

            List<ErrorResponse.ValidationError> validationErrors = ex.getConstraintViolations()
                    .stream()
                    .map(violation -> {
                        String field = violation.getPropertyPath().toString();
                        ErrorResponse.ValidationError ve = new ErrorResponse.ValidationError();
                        ve.setField(field.substring(field.lastIndexOf('.') + 1));
                        ve.setMessage(violation.getMessage());
                        ve.setRejectedValue(violation.getInvalidValue());
                        return ve;
                    })
                    .collect(Collectors.toList());

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST, "Erreur de validation");
            problemDetail.setTitle("Constraint Violation");
            problemDetail.setProperty("timestamp", LocalDateTime.now());
            problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));
            problemDetail.setProperty("validationErrors", validationErrors);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
        }
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        try (ApiErrorMdc ignored = ApiErrorMdc.start(request, HttpStatus.BAD_REQUEST, "type_mismatch")) {
            log.warn("Type mismatch parameter={}: {}", ex.getName(), ex.getMessage());

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    String.format("Le paramètre '%s' doit être de type %s",
                            ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "inconnu"));
            problemDetail.setTitle("Type Mismatch");
            problemDetail.setProperty("timestamp", LocalDateTime.now());
            problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        try (ApiErrorMdc ignored = ApiErrorMdc.start(request, HttpStatus.BAD_REQUEST, "illegal_argument")) {
            log.warn("Illegal argument: {}", ex.getMessage());

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST, ex.getMessage());
            problemDetail.setTitle("Illegal Argument");
            problemDetail.setProperty("timestamp", LocalDateTime.now());
            problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
        }
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex, WebRequest request) {
        Throwable root = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause() : ex;
        String detail = root.getMessage() != null ? root.getMessage() : ex.getMessage();
        try (ApiErrorMdc ignored = ApiErrorMdc.start(request, HttpStatus.BAD_REQUEST, "data_integrity")) {
            log.error("DB integrity constraint: {}", detail, ex);

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Données refusées par la base. Vérifiez les champs obligatoires et le schéma (ex. colonne display_duration_seconds). Détail: "
                            + detail);
            problemDetail.setTitle("Data Integrity Violation");
            problemDetail.setProperty("timestamp", LocalDateTime.now());
            problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
        }
    }

    @ExceptionHandler(org.springframework.dao.InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<ProblemDetail> handleInvalidDataAccessResourceUsage(
            org.springframework.dao.InvalidDataAccessResourceUsageException ex, WebRequest request) {
        Throwable root = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause() : ex;
        String detail = root.getMessage() != null ? root.getMessage() : ex.getMessage();
        try (ApiErrorMdc ignored = ApiErrorMdc.start(request, HttpStatus.BAD_REQUEST, "data_access")) {
            log.error("SQL / data access: {}", detail, ex);

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Erreur d'accès aux données (souvent colonne ou table manquante). Détail: " + detail);
            problemDetail.setTitle("Invalid Data Access");
            problemDetail.setProperty("timestamp", LocalDateTime.now());
            problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
        }
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, WebRequest request) {
        try (ApiErrorMdc ignored = ApiErrorMdc.start(request, HttpStatus.CONFLICT, "optimistic_lock")) {
            log.warn("optimistic_lock_conflict entity={}", ex.getMessage());

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Le produit a été modifié entre-temps. Veuillez recharger et réessayer.");
            problemDetail.setTitle("Conflit de mise à jour");
            problemDetail.setProperty("timestamp", LocalDateTime.now());
            problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

            return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(
            Exception ex, WebRequest request) {
        try (ApiErrorMdc ignored = ApiErrorMdc.start(request, HttpStatus.INTERNAL_SERVER_ERROR, "internal_error")) {
            log.error("Unexpected internal error", ex);

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Une erreur interne s'est produite. Veuillez réessayer plus tard.");
            problemDetail.setTitle("Internal Server Error");
            problemDetail.setProperty("timestamp", LocalDateTime.now());
            problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
        }
    }
}
