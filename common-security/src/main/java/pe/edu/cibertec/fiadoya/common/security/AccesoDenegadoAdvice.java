package pe.edu.cibertec.fiadoya.common.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.common.web.Problemas;

/** @PreAuthorize lanza AccessDeniedException dentro del controlador: se responde 403 AUTH_FORBIDDEN. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccesoDenegadoAdvice {

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ResponseEntity<ProblemDetail> denegado(Exception ex) {
        ProblemDetail pd = Problemas.de(ErrorCode.AUTH_FORBIDDEN, "No tienes permiso para este recurso.");
        return ResponseEntity.status(pd.getStatus()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }
}
