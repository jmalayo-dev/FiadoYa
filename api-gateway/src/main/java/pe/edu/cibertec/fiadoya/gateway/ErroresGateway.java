package pe.edu.cibertec.fiadoya.gateway;

import java.net.ConnectException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/** Si un servicio está caído o no hay instancias en Eureka, el gateway responde 503 SERVICIO_NO_DISPONIBLE. */
@Component
@Order(-2)
public class ErroresGateway implements ErrorWebExceptionHandler {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatus status;
        String code;
        String detalle;
        if (ex instanceof NotFoundException || causadoPor(ex, ConnectException.class) || causadoPor(ex, TimeoutException.class)
                || causadoPor(ex, java.net.UnknownHostException.class)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            code = "SERVICIO_NO_DISPONIBLE";
            detalle = "El servicio no está disponible. Intenta en unos segundos.";
        } else if (ex instanceof ResponseStatusException rse && rse.getStatusCode().value() == 404) {
            status = HttpStatus.NOT_FOUND;
            code = "NOT_FOUND";
            detalle = "La ruta no existe.";
        } else if (ex instanceof ResponseStatusException rse && rse.getStatusCode().is5xxServerError()) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            code = "SERVICIO_NO_DISPONIBLE";
            detalle = "El servicio no está disponible. Intenta en unos segundos.";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = "ERROR_INTERNO";
            detalle = "Ocurrió un error inesperado.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("type", "https://fiadoya.local/errors/" + code.toLowerCase().replace('_', '-'));
        cuerpo.put("title", status.getReasonPhrase());
        cuerpo.put("status", status.value());
        cuerpo.put("detail", detalle);
        cuerpo.put("code", code);
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(MAPPER.writeValueAsBytes(cuerpo))));
    }

    private static boolean causadoPor(Throwable ex, Class<? extends Throwable> tipo) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (tipo.isInstance(t)) {
                return true;
            }
        }
        return false;
    }
}
