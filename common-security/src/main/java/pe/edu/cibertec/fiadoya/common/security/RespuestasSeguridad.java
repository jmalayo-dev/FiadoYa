package pe.edu.cibertec.fiadoya.common.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.common.web.Problemas;
import tools.jackson.databind.json.JsonMapper;

/** 401 y 403 con el mismo formato ProblemDetail que el resto de errores. */
public final class RespuestasSeguridad {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private RespuestasSeguridad() {
    }

    public static AuthenticationEntryPoint noAutenticado() {
        return (request, response, ex) -> escribir(response,
                Problemas.de(ErrorCode.AUTH_TOKEN_EXPIRED, "Token ausente, vencido o inválido."));
    }

    public static AccessDeniedHandler sinPermiso() {
        return (request, response, ex) -> escribir(response,
                Problemas.de(ErrorCode.AUTH_FORBIDDEN, "No tienes permiso para este recurso."));
    }

    static void escribir(HttpServletResponse response, ProblemDetail pd) throws IOException {
        response.setStatus(pd.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getOutputStream(), Problemas.comoMapa(pd));
    }
}
