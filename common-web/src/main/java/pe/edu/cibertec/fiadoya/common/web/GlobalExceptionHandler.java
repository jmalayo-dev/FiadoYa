package pe.edu.cibertec.fiadoya.common.web;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Manejador global: toda respuesta de error sale como application/problem+json (Anexo §7). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ProblemDetail> validacionNegocio(ValidationException ex) {
        return responder(Problemas.de(ErrorCode.VALIDATION_ERROR, ex.getMessage(), ex.getExtra(), ex.getErrores()));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> negocio(ApiException ex) {
        return responder(Problemas.de(ex.getCode(), ex.getMessage(), ex.getExtra(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> beanValidation(MethodArgumentNotValidException ex) {
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::aCampo)
                .toList();
        if (errores.isEmpty()) {
            errores = ex.getBindingResult().getGlobalErrors().stream()
                    .map(e -> new ErrorCampo(e.getObjectName(), e.getDefaultMessage()))
                    .toList();
        }
        return responder(Problemas.de(ErrorCode.VALIDATION_ERROR, "Revisa los campos marcados.", null, errores));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> validacionParametros(HandlerMethodValidationException ex) {
        List<ErrorCampo> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new ErrorCampo(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        return responder(Problemas.de(ErrorCode.VALIDATION_ERROR, "Revisa los campos marcados.", null, errores));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> restricciones(ConstraintViolationException ex) {
        List<ErrorCampo> errores = ex.getConstraintViolations().stream()
                .map(v -> new ErrorCampo(ultimoNodo(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return responder(Problemas.de(ErrorCode.VALIDATION_ERROR, "Revisa los campos marcados.", null, errores));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    ResponseEntity<ProblemDetail> entradaIlegible(Exception ex) {
        String campo = switch (ex) {
            case MissingRequestHeaderException m -> m.getHeaderName();
            case MissingServletRequestParameterException m -> m.getParameterName();
            case MethodArgumentTypeMismatchException m -> m.getName();
            default -> "cuerpo";
        };
        return responder(Problemas.de(ErrorCode.VALIDATION_ERROR, "Revisa los campos marcados.", null,
                List.of(new ErrorCampo(campo, "valor ausente o con formato inválido"))));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> sinRuta(NoResourceFoundException ex) {
        return responder(Problemas.de(ErrorCode.NOT_FOUND, "La ruta no existe."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> metodo(HttpRequestMethodNotSupportedException ex) {
        ProblemDetail pd = Problemas.de(ErrorCode.NOT_FOUND, "Método no soportado en esta ruta.");
        pd.setStatus(405);
        return responder(pd);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> inesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return responder(Problemas.de(ErrorCode.ERROR_INTERNO, "Ocurrió un error inesperado."));
    }

    private static ResponseEntity<ProblemDetail> responder(ProblemDetail pd) {
        return ResponseEntity.status(pd.getStatus()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    private static ErrorCampo aCampo(FieldError fe) {
        return new ErrorCampo(fe.getField(), fe.getDefaultMessage());
    }

    private static String ultimoNodo(String ruta) {
        int i = ruta.lastIndexOf('.');
        return i >= 0 ? ruta.substring(i + 1) : ruta;
    }
}
