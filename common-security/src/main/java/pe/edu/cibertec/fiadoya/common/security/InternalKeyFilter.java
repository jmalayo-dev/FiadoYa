package pe.edu.cibertec.fiadoya.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * R1: las rutas /internal/** solo aceptan llamadas servicio a servicio con el header X-Internal-Key.
 * Si la clave es correcta se autentica como ROLE_INTERNO; si no, la regla de autorización responde 401.
 */
public class InternalKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Key";

    private final byte[] clave;

    public InternalKeyFilter(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("INTERNAL_API_KEY no está configurada");
        }
        this.clave = clave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibida = request.getHeader(HEADER);
        if (recibida != null && MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
            var auth = new UsernamePasswordAuthenticationToken("servicio-interno", null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + Roles.INTERNO)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
