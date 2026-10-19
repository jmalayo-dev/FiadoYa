package pe.edu.cibertec.fiadoya.ventas.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.InputStream;
import java.util.Map;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Un 4xx del servicio remoto llega como ProblemDetail: se reconstruye la ApiException con su code y extra
 * para que la saga decida (409 de negocio). Los 5xx quedan como error técnico y cuentan para el circuit breaker.
 */
public class ProblemDetailErrorDecoder implements ErrorDecoder {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private final ErrorDecoder porDefecto = new ErrorDecoder.Default();

    @Override
    @SuppressWarnings("unchecked")
    public Exception decode(String metodo, Response response) {
        if (response.status() >= 400 && response.status() < 500 && response.body() != null) {
            try (InputStream in = response.body().asInputStream()) {
                Map<String, Object> cuerpo = MAPPER.readValue(in, Map.class);
                ErrorCode code = codigo(cuerpo.get("code"), response.status());
                Map<String, Object> extra = cuerpo.get("extra") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
                return new ApiException(code, String.valueOf(cuerpo.getOrDefault("detail", code.titulo())), extra);
            } catch (Exception e) {
                return porDefecto.decode(metodo, response);
            }
        }
        return porDefecto.decode(metodo, response);
    }

    private static ErrorCode codigo(Object valor, int status) {
        try {
            return ErrorCode.valueOf(String.valueOf(valor));
        } catch (IllegalArgumentException e) {
            return status == 404 ? ErrorCode.NOT_FOUND : ErrorCode.VALIDATION_ERROR;
        }
    }
}
